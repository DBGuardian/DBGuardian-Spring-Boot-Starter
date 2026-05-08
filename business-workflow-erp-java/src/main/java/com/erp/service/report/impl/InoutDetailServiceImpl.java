package com.erp.service.report.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.report.dto.InoutDetailRequest;
import com.erp.controller.report.dto.InoutDetailResponse;
import com.erp.controller.report.dto.InoutDetailRow;
import com.erp.controller.report.dto.InoutExcelRow;
import com.erp.mapper.report.InoutDetailMapper;
import com.erp.service.report.InoutDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 出入库明细表 Service 实现
 * 
 * 查询优化策略：
 * 1. 使用原生SQL JOIN查询获取入库数据（1次查询）
 * 2. 批量查询出库数据并按废物代码+名称分组（1次查询）
 * 3. 在内存中计算库存数量 = 入库数量 - 出库数量
 * 4. 支持Redis缓存，避免重复查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InoutDetailServiceImpl implements InoutDetailService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final InoutDetailMapper inoutDetailMapper;
  
  // 缓存键前缀
  private static final String CACHE_KEY_PREFIX = "inout:detail:";
  private static final String CACHE_KEY_ALL = "inout:detail:all";

  @Override
  @Transactional(readOnly = true)
  public InoutDetailResponse getDetailList(InoutDetailRequest request) {
    // 1. 检查缓存
    String cacheKey = buildCacheKey(request);
    InoutDetailResponse cached = null;
    try {
      cached = (InoutDetailResponse) redisTemplate.opsForValue().get(cacheKey);
    } catch (SerializationException e) {
      log.warn("出入库明细缓存反序列化失败，删除旧缓存并重新计算: {}", cacheKey);
      redisTemplate.delete(cacheKey);
    }
    if (cached != null) {
      log.debug("从缓存获取出入库明细数据: {}", cacheKey);
      return cached;
    }

    // 2. 缓存不存在，计算数据
    InoutDetailResponse response = calculateDetailList(request);

    // 3. 缓存结果（7天过期）
    redisTemplate.opsForValue().set(cacheKey, response, java.time.Duration.ofDays(7));

    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public InoutDetailResponse recalculateDetailList(InoutDetailRequest request) {
    // 清除缓存
    String cacheKey = buildCacheKey(request);
    redisTemplate.delete(cacheKey);
    redisTemplate.delete(CACHE_KEY_ALL);

    // 重新计算
    return getDetailList(request);
  }

  @Override
  public void clearAllCache() {
    Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
      log.info("已清除所有出入库缓存，共 {} 个", keys.size());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<InoutExcelRow> queryAllForExport(InoutDetailRequest request) {
    List<InoutDetailRow> inboundData = inoutDetailMapper.queryAllInboundDetail(
        request.getCategory(),
        request.getContractNo(),
        request.getWasteCode(),
        request.getWasteName(),
        request.getDateStart(),
        request.getDateEnd()
    );

    Map<String, BigDecimal> outboundMap = queryOutboundData();

    for (int i = 0; i < inboundData.size(); i++) {
      InoutDetailRow row = inboundData.get(i);
      row.setSequenceNo(i + 1);
      String key = row.getWasteCode() + "|" + row.getWasteName();
      BigDecimal outboundQty = outboundMap.getOrDefault(key, BigDecimal.ZERO);
      row.setOutboundQty(outboundQty);
      if (row.getStockQty() == null) {
        row.setStockQty(BigDecimal.ZERO);
      }
      row.setRowKey(buildRowKey(row));
    }

    List<String> selectedKeys = request.getSelectedKeys();
    if (selectedKeys != null && !selectedKeys.isEmpty()) {
      Set<String> selectedKeySet = new HashSet<>(selectedKeys);
      inboundData = inboundData.stream()
          .filter(row -> row.getRowKey() != null && selectedKeySet.contains(row.getRowKey()))
          .collect(Collectors.toList());
    }

    return inboundData.stream()
        .map(this::convertToExcelRow)
        .collect(Collectors.toList());
  }

  /**
   * 计算出入库明细数据
   * 
   * 查询流程：
   * 1. 查询入库数据（JOIN多表）
   * 2. 查询出库数据并按废物代码+名称分组
   * 3. 在内存中计算库存数量
   * 4. 分页返回
   */
  @Transactional(readOnly = true)
  protected InoutDetailResponse calculateDetailList(InoutDetailRequest request) {
    // 1. 查询入库数据（包含合同、车辆、总磅等信息）
    List<InoutDetailRow> inboundData = queryInboundData(request);

    // 2. 查询出库数据并按废物代码+名称分组
    Map<String, BigDecimal> outboundMap = queryOutboundData();

    // 3. 计算库存数量并填充出库数据
    int seq = 1;
    for (InoutDetailRow row : inboundData) {
      row.setSequenceNo(seq++);
      
      String key = row.getWasteCode() + "|" + row.getWasteName();
      BigDecimal outboundQty = outboundMap.getOrDefault(key, BigDecimal.ZERO);
      row.setOutboundQty(outboundQty);

      if (row.getStockQty() == null) {
        row.setStockQty(BigDecimal.ZERO);
      }
      row.setRowKey(buildRowKey(row));
    }

    // 4. 分页处理
    InoutDetailResponse response = new InoutDetailResponse();
    int pageNum = request.getPage() != null ? request.getPage() : 1;
    int pageSize = request.getSize() != null ? request.getSize() : 10;
    
    int total = inboundData.size();
    int start = (pageNum - 1) * pageSize;
    int end = Math.min(start + pageSize, total);
    
    // 使用 new ArrayList 包装 subList 结果，避免 Redis 序列化问题
    List<InoutDetailRow> pageData = new ArrayList<>(inboundData.subList(start, end));

    response.setRecords(pageData);
    response.setTotal((long) total);
    response.setCurrent(pageNum);
    response.setSize(pageSize);

    return response;
  }

  /**
   * 查询入库数据（JOIN多表）
   * 
   * 关联路径：
   * WAREHOUSING_WASTE_ITEM → WAREHOUSING → WEIGHING_SLIP → VEHICLE
   * WAREHOUSING → PICKUP_NOTICE → CONTRACT → EMPLOYEE
   */
  private List<InoutDetailRow> queryInboundData(InoutDetailRequest request) {
    log.debug("查询入库数据，筛选条件：{}", request);
    
    List<InoutDetailRow> result = inoutDetailMapper.queryInboundDetail(
        request.getCategory(),
        request.getContractNo(),
        request.getWasteCode(),
        request.getWasteName(),
        request.getDateStart(),
        request.getDateEnd()
    );
    
    log.debug("查询到入库数据 {} 条", result.size());
    return result;
  }

  /**
   * 查询出库数据并按废物代码+名称分组
   * 
   * 返回格式：Map<"废物代码|废物名称", 出库数量>
   */
  private Map<String, BigDecimal> queryOutboundData() {
    log.debug("查询出库数据并分组");
    
    List<Map<String, Object>> outboundList = inoutDetailMapper.queryOutboundGrouped();
    Map<String, BigDecimal> result = new HashMap<>();
    
    for (Map<String, Object> row : outboundList) {
      String wasteKey = (String) row.get("wasteKey");
      Object qtyObj = row.get("outboundQty");
      BigDecimal qty = BigDecimal.ZERO;
      
      if (qtyObj instanceof BigDecimal) {
        qty = (BigDecimal) qtyObj;
      } else if (qtyObj instanceof Number) {
        qty = new BigDecimal(qtyObj.toString());
      }
      
      result.put(wasteKey, qty);
    }
    
    log.debug("查询到出库数据分组 {} 个", result.size());
    return result;
  }

  /**
   * 计算合计行
   */
  private InoutDetailRow calculateSummaryRow(List<InoutDetailRow> data) {
    InoutDetailRow summary = new InoutDetailRow();
    summary.setSequenceNo(0);
    summary.setCategory("合计");
    summary.setContractNo("-");
    summary.setPartyName("-");
    summary.setBusinessPerson("-");
    summary.setPlateNumber("-");
    summary.setWeighingSlipNo("-");
    summary.setWasteCategory("-");
    summary.setWasteCode("-");
    summary.setWasteName("-");
    summary.setUnit("-");
    summary.setRemark("-");
    summary.setIsSummaryRow(true);

    BigDecimal inboundQty = BigDecimal.ZERO;
    BigDecimal outboundQty = BigDecimal.ZERO;
    BigDecimal stockQty = BigDecimal.ZERO;

    for (InoutDetailRow row : data) {
      if (!Boolean.TRUE.equals(row.getIsSummaryRow())) {
        inboundQty = inboundQty.add(row.getInboundQty() != null ? row.getInboundQty() : BigDecimal.ZERO);
        outboundQty = outboundQty.add(row.getOutboundQty() != null ? row.getOutboundQty() : BigDecimal.ZERO);
        stockQty = stockQty.add(row.getStockQty() != null ? row.getStockQty() : BigDecimal.ZERO);
      }
    }

    summary.setInboundQty(inboundQty);
    summary.setOutboundQty(outboundQty);
    summary.setStockQty(stockQty);

    return summary;
  }

  /**
   * 转换为Excel行数据
   */
  private InoutExcelRow convertToExcelRow(InoutDetailRow row) {
    InoutExcelRow excelRow = new InoutExcelRow();
    excelRow.setSequenceNo(row.getSequenceNo());
    excelRow.setCategory(row.getCategory());
    excelRow.setContractNo(row.getContractNo());
    excelRow.setPartyName(row.getPartyName());
    excelRow.setBusinessPerson(row.getBusinessPerson());
    excelRow.setPlateNumber(row.getPlateNumber());
    excelRow.setWeighingSlipNo(row.getWeighingSlipNo());
    excelRow.setWasteCategory(row.getWasteCategory());
    excelRow.setWasteCode(row.getWasteCode());
    excelRow.setWasteName(row.getWasteName());
    excelRow.setUnit(row.getUnit());
    excelRow.setInboundQty(row.getInboundQty() != null ? row.getInboundQty() : BigDecimal.ZERO);
    excelRow.setOutboundQty(row.getOutboundQty() != null ? row.getOutboundQty() : BigDecimal.ZERO);
    excelRow.setStockQty(row.getStockQty() != null ? row.getStockQty() : BigDecimal.ZERO);
    excelRow.setRemark(row.getRemark());
    excelRow.setRowKey(row.getRowKey());
    return excelRow;
  }

  private String buildRowKey(InoutDetailRow row) {
    return String.join("::",
        defaultString(row.getSequenceNo()),
        defaultString(row.getCategory()),
        defaultString(row.getContractNo()),
        defaultString(row.getWeighingSlipNo()),
        defaultString(row.getWasteCode()),
        defaultString(row.getWasteName()),
        defaultString(row.getRemark())
    );
  }

  private String defaultString(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  /**
   * 构建缓存键
   */
  private String buildCacheKey(InoutDetailRequest request) {
    return CACHE_KEY_PREFIX + 
        (request.getCategory() != null ? request.getCategory() : "all") + ":" +
        (request.getContractNo() != null ? request.getContractNo() : "all") + ":" +
        (request.getWasteCode() != null ? request.getWasteCode() : "all") + ":" +
        (request.getDateStart() != null ? request.getDateStart() : "all") + ":" +
        (request.getDateEnd() != null ? request.getDateEnd() : "all") + ":" +
        request.getPage() + ":" +
        request.getSize();
  }
}
