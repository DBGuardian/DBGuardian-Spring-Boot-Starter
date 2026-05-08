package com.erp.service.report.impl;

import com.erp.controller.report.dto.*;
import com.erp.mapper.report.ReceivableDetailMapper;
import com.erp.service.report.ReceivableDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应收账款明细表 Service 实现
 * 
 * 功能描述：
 * - 获取应收账款明细表数据（支持Redis缓存）
 * - 重新计算应收账款明细表（清除缓存）
 * - 清除所有应收账款缓存
 * 
 * 缓存策略：
 * - 首次加载：自动计算数据并缓存到Redis
 * - 后续加载：直接从Redis加载缓存数据
 * - 手动刷新：点击"重新计算"按钮，清除缓存并重新计算
 * 
 * 查询优化：
 * - 使用JOIN连表查询获取所有数据（1次查询）
 * - 批量查询补充数据（2次查询）
 * - 在内存中构建树形结构（避免多次数据库查询）
 * - 总查询次数：3次
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableDetailServiceImpl implements ReceivableDetailService {

  private final ReceivableDetailMapper receivableDetailMapper;
  private final RedisTemplate<String, Object> redisTemplate;

  private static final String CACHE_KEY_PREFIX = "receivable:detail";
  private static final long CACHE_TTL = 604800; // 1周（7天）
  private static final long COMPUTING_TTL = 300; // 5分钟（防止重复计算）

  /**
   * 获取应收账款明细表数据
   * 
   * 流程：
   * 1. 先检查缓存
   * 2. 缓存不存在则计算并缓存
   * 3. 返回数据
   */
  @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
  @Override
  public ReceivableDetailResponse getDetailList(ReceivableDetailRequest request) {
    // 生成缓存键
    String cacheKey = generateCacheKey(request);

    // 尝试从缓存获取
    ReceivableDetailResponse cachedData = (ReceivableDetailResponse) redisTemplate.opsForValue().get(cacheKey);

    if (cachedData != null) {
      // 缓存命中，直接返回
      cachedData.setFromCache(true);
      log.info("应收账款明细表数据从缓存加载，缓存键：{}", cacheKey);
      return cachedData;
    }

    // 检查是否正在计算中（防止并发计算）
    String computingKey = cacheKey + ":computing";
    Boolean isComputing = (Boolean) redisTemplate.opsForValue().get(computingKey);

    if (isComputing != null && isComputing) {
      // 正在计算中，返回提示信息
      log.warn("应收账款明细表数据正在计算中，缓存键：{}", cacheKey);
      throw new RuntimeException("数据正在计算中，请稍候...");
    }

    // 标记为计算中
    redisTemplate.opsForValue().set(computingKey, true, Duration.ofSeconds(COMPUTING_TTL));

    try {
      // 执行计算
      log.info("开始计算应收账款明细表数据，缓存键：{}", cacheKey);
      ReceivableDetailResponse data = calculateDetailList(request);

      // 缓存结果
      redisTemplate.opsForValue().set(cacheKey, data, Duration.ofSeconds(CACHE_TTL));

      data.setFromCache(false);
      log.info("应收账款明细表数据计算完成，缓存键：{}", cacheKey);
      return data;
    } finally {
      // 清除计算中标记
      redisTemplate.delete(computingKey);
    }
  }

  /**
   * 重新计算数据（清除缓存）
   */
  @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
  @Override
  public ReceivableDetailResponse recalculateDetailList(ReceivableDetailRequest request) {
    // 清除全量数据缓存
    String fullDataCacheKey = CACHE_KEY_PREFIX + ":full-data";
    redisTemplate.delete(fullDataCacheKey);
    log.info("已清除应收账款明细表全量数据缓存");

    // 重新计算
    return getDetailList(request);
  }

  /**
   * 清除所有应收账款缓存
   */
  @Override
  public void clearAllCache() {
    Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + ":*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
      log.info("已清除所有应收账款缓存，共清除 {} 个缓存键", keys.size());
    }
  }

  /**
   * 计算应收账款明细数据
   * 
   * 缓存策略优化：
   * 1. 先查询全量数据（不包含搜索条件）并缓存
   * 2. 在内存中进行搜索和筛选
   * 3. 应用分页
   * 
   * 优点：
   * - 减少缓存键数量
   * - 提高缓存命中率
   * - 搜索条件改变时，直接从全量缓存中筛选
   */
  private ReceivableDetailResponse calculateDetailList(ReceivableDetailRequest request) {
    // 第1步：获取全量数据（缓存不包含搜索条件）
    List<ContractRow> allContractRows = getFullDataFromCache();
    
    if (allContractRows == null) {
      // 缓存未命中，从数据库查询全量数据
      allContractRows = queryFullDataFromDatabase();
      
      if (allContractRows.isEmpty()) {
        return new ReceivableDetailResponse(Collections.emptyList(), 0L, false, null);
      }
      
      // 缓存全量数据
      cacheFullData(allContractRows);
    }
    
    // 第2步：在内存中进行搜索和筛选
    List<ContractRow> filteredRows = filterContractRows(allContractRows, request);
    
    if (filteredRows.isEmpty()) {
      return new ReceivableDetailResponse(Collections.emptyList(), 0L, false, null);
    }
    
    // 第3步：应用分页
    long totalCount = filteredRows.size();
    int pageIndex = (request.getPage() != null ? request.getPage() : 1) - 1;
    int pageSize = request.getSize() != null ? request.getSize() : 10;
    int startIndex = pageIndex * pageSize;
    int endIndex = Math.min(startIndex + pageSize, filteredRows.size());
    
    // 将 subList 转换为新的 ArrayList，避免 ArrayList$SubList 无法被 Jackson 反序列化
    List<ContractRow> pagedRows = new ArrayList<>(filteredRows.subList(startIndex, endIndex));
    
    return new ReceivableDetailResponse(pagedRows, totalCount, false, null);
  }

  /**
   * 从缓存获取全量数据
   */
  private List<ContractRow> getFullDataFromCache() {
    String fullDataCacheKey = CACHE_KEY_PREFIX + ":full-data";
    Object cachedData = redisTemplate.opsForValue().get(fullDataCacheKey);
    if (cachedData != null) {
      log.info("应收账款明细表全量数据从缓存加载");
      return (List<ContractRow>) cachedData;
    }
    return null;
  }

  /**
   * 缓存全量数据
   */
  private void cacheFullData(List<ContractRow> contractRows) {
    String fullDataCacheKey = CACHE_KEY_PREFIX + ":full-data";
    redisTemplate.opsForValue().set(fullDataCacheKey, contractRows, Duration.ofSeconds(CACHE_TTL));
    log.info("应收账款明细表全量数据已缓存，共 {} 条合同", contractRows.size());
  }

  /**
   * 从数据库查询全量数据
   */
  private List<ContractRow> queryFullDataFromDatabase() {
    // 创建一个不包含搜索条件的请求对象
    ReceivableDetailRequest fullRequest = new ReceivableDetailRequest();
    fullRequest.setPage(1);
    fullRequest.setSize(Integer.MAX_VALUE); // 获取所有数据
    
    // 第1步：查询符合条件的合同总数（用于分页）
    Long totalCount = receivableDetailMapper.countContractIds(fullRequest);
    
    if (totalCount == null || totalCount == 0) {
      return Collections.emptyList();
    }

    // 第2步：查询符合条件的合同ID列表（应用分页）
    List<Long> contractIds = receivableDetailMapper.selectContractIds(fullRequest);
    
    if (contractIds.isEmpty()) {
      return Collections.emptyList();
    }

    // 第3步：根据合同ID查询完整的树形数据（不分页）
    List<ReceivableDetailDTO> dtoList = receivableDetailMapper.selectDetailListWithJoin(contractIds);

    if (dtoList.isEmpty()) {
      return Collections.emptyList();
    }

    // 第3步：提取所有的 ID 用于批量查询
    Set<Long> allContractIds = dtoList.stream()
        .map(ReceivableDetailDTO::getContractId)
        .collect(Collectors.toSet());

    Set<Long> settlementIds = dtoList.stream()
        .filter(dto -> dto.getSettlementId() != null)
        .map(ReceivableDetailDTO::getSettlementId)
        .collect(Collectors.toSet());

    // 第4步：批量查询计价方式（一次查询）
    Map<Long, String> pricingModeMap = new HashMap<>();
    if (!allContractIds.isEmpty()) {
      List<ContractPricingModeDTO> pricingModes = receivableDetailMapper.selectPricingModesByContractIds(new ArrayList<>(allContractIds));
      pricingModeMap = pricingModes.stream()
          .collect(Collectors.toMap(
              ContractPricingModeDTO::getContractId,
              ContractPricingModeDTO::getPricingMode
          ));
    }

    // 第5步：批量查询已收金额（一次查询）
    Map<Long, BigDecimal> receivedAmountMap = new HashMap<>();
    if (!settlementIds.isEmpty()) {
      receivedAmountMap = receivableDetailMapper.selectReceivedAmountsBySettlementIds(new ArrayList<>(settlementIds))
          .stream()
          .collect(Collectors.toMap(
              SettlementReceivedAmountDTO::getSettlementId,
              SettlementReceivedAmountDTO::getReceivedAmount
          ));
    }

    // 第6步：在内存中构建树形结构（不再查询数据库）
    return buildTreeStructure(dtoList, pricingModeMap, receivedAmountMap);
  }

  /**
   * 在内存中进行搜索和筛选
   */
  private List<ContractRow> filterContractRows(List<ContractRow> allRows, ReceivableDetailRequest request) {
    return allRows.stream()
        .filter(row -> matchesSearchCriteria(row, request))
        .collect(Collectors.toList());
  }

  /**
   * 判断合同行是否匹配搜索条件
   */
  private boolean matchesSearchCriteria(ContractRow row, ReceivableDetailRequest request) {
    // 合同编号模糊查询
    if (request.getContractNo() != null && !request.getContractNo().isEmpty()) {
      if (!row.getContractNo().contains(request.getContractNo())) {
        return false;
      }
    }
    
    // 客户名称模糊查询
    if (request.getPartyAName() != null && !request.getPartyAName().isEmpty()) {
      if (!row.getPartyAName().contains(request.getPartyAName())) {
        return false;
      }
    }
    
    // 结算类型筛选
    if (request.getSettlementType() != null && !request.getSettlementType().isEmpty()) {
      if (!row.getSettlementType().equals(request.getSettlementType())) {
        return false;
      }
    }
    
    // 计价方式筛选
    if (request.getPricingMode() != null && !request.getPricingMode().isEmpty()) {
      if (!row.getPricingMode().equals(request.getPricingMode())) {
        return false;
      }
    }
    
    // 日期范围筛选
    if (request.getDateStart() != null && !request.getDateStart().isEmpty()) {
      try {
        LocalDate startDate = LocalDate.parse(request.getDateStart());
        LocalDate contractDate = row.getContractSignDate();
        if (contractDate != null && contractDate.isBefore(startDate)) {
          return false;
        }
      } catch (Exception e) {
        log.warn("日期解析失败: {}", e.getMessage());
      }
    }
    
    if (request.getDateEnd() != null && !request.getDateEnd().isEmpty()) {
      try {
        LocalDate endDate = LocalDate.parse(request.getDateEnd());
        LocalDate contractDate = row.getContractSignDate();
        if (contractDate != null && contractDate.isAfter(endDate)) {
          return false;
        }
      } catch (Exception e) {
        log.warn("日期解析失败: {}", e.getMessage());
      }
    }
    
    return true;
  }

  /**
   * 在内存中构建树形结构（避免多次数据库查询）
   * 
   * 数据结构：合同 -> 结算单 -> 发票+应收明细（一对一合并显示）
   * 
   * 应收明细计算逻辑：
   * - 第一行应收明细：应收金额 = 结算单.结算金额
   * - 后续行应收明细：应收金额 = 上一行的未收金额
   * - 每一行的已收金额 = SETTLEMENT_INVOICE_REL.关联金额
   * - 每一行的未收金额 = 该行应收金额 - 该行已收金额
   */
  private List<ContractRow> buildTreeStructure(
      List<ReceivableDetailDTO> dtoList,
      Map<Long, String> pricingModeMap,
      Map<Long, BigDecimal> receivedAmountMap) {

    Map<Long, ContractRow> contractMap = new LinkedHashMap<>();
    Map<Long, SettlementRow> settlementMap = new LinkedHashMap<>();
    // key: settlementId, value: 该结算单下已处理的应收明细的累计已收金额（用于计算链式应收金额）
    Map<Long, BigDecimal> settlementAccumulatedReceivedMap = new LinkedHashMap<>();

    for (ReceivableDetailDTO dto : dtoList) {
      // 构建合同行
      ContractRow contractRow = contractMap.computeIfAbsent(
          dto.getContractId(),
          id -> {
            // 如果计价方式为空，根据结算类型设置默认值
            String pricingMode = pricingModeMap.getOrDefault(id, "未知");
            if ("未知".equals(pricingMode) && "业务费结算".equals(dto.getSettlementType())) {
              pricingMode = "业务费";
            }
            return new ContractRow(
                id,
                dto.getContractNo(),
                dto.getPartyAName(),
                dto.getContractAmount(),
                pricingMode,
                dto.getContractCreatedBy(),
                dto.getContractSignDate(),
                dto.getSettlementType()
            );
          }
      );

      if (dto.getSettlementId() == null) continue;

      // 构建结算单行
      SettlementRow settlementRow = settlementMap.computeIfAbsent(
          dto.getSettlementId(),
          id -> {
            SettlementRow row = new SettlementRow(
                id,
                dto.getSettlementCode(),
                dto.getSettlementPeriod(),
                dto.getSettlementAmount(),
                receivedAmountMap.getOrDefault(id, BigDecimal.ZERO),
                dto.getSettlementCreatedBy()
            );
            contractRow.addChild(row);
            settlementAccumulatedReceivedMap.put(id, BigDecimal.ZERO);
            return row;
          }
      );

      // 只有存在真实关联记录（invoiceId和arDetailId都不为null）才构建发票+应收明细行
      if (dto.getInvoiceId() == null || dto.getArDetailId() == null) continue;

      // 计算本行应收金额（链式逻辑）
      BigDecimal accumulatedReceived = settlementAccumulatedReceivedMap.getOrDefault(dto.getSettlementId(), BigDecimal.ZERO);
      BigDecimal receivableAmount;
      if (settlementRow.getChildren().isEmpty()) {
        // 第一行：应收金额 = 结算单.结算金额
        receivableAmount = dto.getSettlementAmount() != null ? dto.getSettlementAmount() : BigDecimal.ZERO;
      } else {
        // 后续行：应收金额 = 上一行的未收金额
        InvoiceDetailRow lastRow = settlementRow.getChildren().get(settlementRow.getChildren().size() - 1);
        receivableAmount = lastRow.getOutstandingAmount() != null ? lastRow.getOutstandingAmount() : BigDecimal.ZERO;
      }

      BigDecimal receivedAmount = dto.getRelatedAmount() != null ? dto.getRelatedAmount() : BigDecimal.ZERO;

      // 构建发票+应收明细合并行
      InvoiceDetailRow invoiceDetailRow = new InvoiceDetailRow(
          dto.getInvoiceId(),
          dto.getInvoiceNumber(),
          dto.getInvoiceDate(),
          dto.getInvoiceAmount(),
          dto.getTaxAmount(),
          dto.getTotalAmount(),
          dto.getArDetailId(),
          receivableAmount,
          receivedAmount,
          dto.getRelatedTime(),
          dto.getArCreatedBy(),
          dto.getInvoiceDate()
      );

      // 累计已收金额
      settlementAccumulatedReceivedMap.put(dto.getSettlementId(), accumulatedReceived.add(receivedAmount));

      settlementRow.addChild(invoiceDetailRow);
    }

    return new ArrayList<>(contractMap.values());
  }

  /**
   * 生成缓存键
   */
  private String generateCacheKey(ReceivableDetailRequest request) {
    String paramHash = hashSearchParams(request);
    return String.format("%s:page:%d:size:%d:%s",
        CACHE_KEY_PREFIX,
        request.getPage() != null ? request.getPage() : 1,
        request.getSize() != null ? request.getSize() : 10,
        paramHash);
  }

  /**
   * 查询全量数据并展平为 Excel 行列表（用于导出，不分页）
   *
   * 展平规则：
   * - contractRowSpan = 该合同下所有结算单的发票数量之和（无发票的结算单按1计）
   * - settlementRowSpan = 该结算单下的发票数量
   * - rowSpan = 0 表示被上方单元格合并，写入时跳过
   * - 发票与应收明细 1:1，合并在同一行
   */
  @Override
  @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
  public List<ReceivableExcelRow> queryAllForExport(ReceivableDetailRequest request) {
    // 复用已有查询逻辑，不分页
    ReceivableDetailRequest fullRequest = new ReceivableDetailRequest();
    fullRequest.setPage(1);
    fullRequest.setSize(Integer.MAX_VALUE);
    fullRequest.setContractNo(request.getContractNo());
    fullRequest.setPartyAName(request.getPartyAName());
    fullRequest.setSettlementType(request.getSettlementType());
    fullRequest.setPricingMode(request.getPricingMode());
    fullRequest.setDateStart(request.getDateStart());
    fullRequest.setDateEnd(request.getDateEnd());

    List<ContractRow> allContractRows = getFullDataFromCache();
    if (allContractRows == null) {
      allContractRows = queryFullDataFromDatabase();
    }

    List<ContractRow> filteredRows = filterContractRows(allContractRows, fullRequest);
    List<ContractRow> exportRows = filterBySelectedKeys(filteredRows, request.getSelectedKeys());

    return flattenToExcelRows(exportRows);
  }

  private List<ContractRow> filterBySelectedKeys(List<ContractRow> contracts, List<String> selectedKeys) {
    if (selectedKeys == null || selectedKeys.isEmpty()) {
      return contracts;
    }

    Set<String> selectedKeySet = new LinkedHashSet<>(selectedKeys);
    List<ContractRow> result = new ArrayList<>();

    for (ContractRow contract : contracts) {
      boolean contractSelected = selectedKeySet.contains(contract.getRowKey());
      List<SettlementRow> selectedSettlements = new ArrayList<>();

      if (contract.getChildren() != null) {
        for (SettlementRow settlement : contract.getChildren()) {
          boolean settlementSelected = contractSelected || selectedKeySet.contains(settlement.getRowKey());
          List<InvoiceDetailRow> selectedInvoices = new ArrayList<>();

          if (settlement.getChildren() != null) {
            for (InvoiceDetailRow invoice : settlement.getChildren()) {
              if (contractSelected || settlementSelected || selectedKeySet.contains(invoice.getRowKey())) {
                selectedInvoices.add(invoice);
              }
            }
          }

          if (settlementSelected || !selectedInvoices.isEmpty()) {
            SettlementRow copiedSettlement = new SettlementRow();
            copiedSettlement.setSettlementId(settlement.getSettlementId());
            copiedSettlement.setSettlementCode(settlement.getSettlementCode());
            copiedSettlement.setSettlementPeriod(settlement.getSettlementPeriod());
            copiedSettlement.setSettlementAmount(settlement.getSettlementAmount());
            copiedSettlement.setReceivableAmount(settlement.getReceivableAmount());
            copiedSettlement.setReceivedAmount(settlement.getReceivedAmount());
            copiedSettlement.setBusinessPerson(settlement.getBusinessPerson());
            copiedSettlement.setRowKey(settlement.getRowKey());
            copiedSettlement.setContractId(settlement.getContractId());
            copiedSettlement.setOutstandingAmount(settlement.getOutstandingAmount());
            copiedSettlement.setIsDetailRow(settlement.getIsDetailRow());
            copiedSettlement.setHasChildren(!selectedInvoices.isEmpty());
            copiedSettlement.setChildren(selectedInvoices);
            selectedSettlements.add(copiedSettlement);
          }
        }
      }

      if (contractSelected || !selectedSettlements.isEmpty()) {
        ContractRow copiedContract = new ContractRow();
        copiedContract.setContractId(contract.getContractId());
        copiedContract.setContractNo(contract.getContractNo());
        copiedContract.setPartyAName(contract.getPartyAName());
        copiedContract.setContractAmount(contract.getContractAmount());
        copiedContract.setPricingMode(contract.getPricingMode());
        copiedContract.setBusinessPerson(contract.getBusinessPerson());
        copiedContract.setContractSignDate(contract.getContractSignDate());
        copiedContract.setSettlementType(contract.getSettlementType());
        copiedContract.setRowKey(contract.getRowKey());
        copiedContract.setSequenceNo(contract.getSequenceNo());
        copiedContract.setReceivedAmount(contract.getReceivedAmount());
        copiedContract.setOutstandingAmount(contract.getOutstandingAmount());
        copiedContract.setIsDetailRow(contract.getIsDetailRow());
        copiedContract.setHasChildren(!selectedSettlements.isEmpty());
        copiedContract.setChildren(selectedSettlements);
        result.add(copiedContract);
      }
    }

    return result;
  }

  /**
   * 将树形结构展平为 ReceivableExcelRow 列表
   */
  private List<ReceivableExcelRow> flattenToExcelRows(List<ContractRow> contracts) {
    List<ReceivableExcelRow> rows = new ArrayList<>();
    int seq = 1;

    for (ContractRow contract : contracts) {
      List<SettlementRow> settlements = contract.getChildren();
      if (settlements == null || settlements.isEmpty()) continue;

      // 合同跨行数 = 所有结算单的发票数量之和（无发票的结算单按1计，保留结算单行）
      int contractRowSpan = settlements.stream()
          .mapToInt(s -> Math.max(
              s.getChildren() == null ? 0 : s.getChildren().size(), 1
          ))
          .sum();

      boolean contractFirstRow = true;

      for (SettlementRow settlement : settlements) {
        List<InvoiceDetailRow> invoices = settlement.getChildren();
        // 无发票时用占位行保留结算单
        boolean hasInvoices = invoices != null && !invoices.isEmpty();
        int settlementRowSpan = hasInvoices ? invoices.size() : 1;
        boolean settlementFirstRow = true;

        int invoiceCount = hasInvoices ? invoices.size() : 1;
        for (int i = 0; i < invoiceCount; i++) {
          InvoiceDetailRow invoice = hasInvoices ? invoices.get(i) : null;

          ReceivableExcelRow row = new ReceivableExcelRow();

          // ── 合同字段：只有首行填入 ──
          if (contractFirstRow) {
            row.setContractRowSpan(contractRowSpan);
            row.setContractSeq(seq++);
            row.setSettlementType(contract.getSettlementType());
            row.setPricingMode(contract.getPricingMode());
            row.setPartyAName(contract.getPartyAName());
            row.setContractNo(contract.getContractNo());
            row.setContractSignDate(contract.getContractSignDate());
            row.setContractAmount(contract.getContractAmount());
            row.setContractBizPerson(contract.getBusinessPerson());
            contractFirstRow = false;
          } else {
            row.setContractRowSpan(0); // 被合并，写入阶段跳过
          }

          // ── 结算单字段：只有首行填入 ──
          if (settlementFirstRow) {
            row.setSettlementRowSpan(settlementRowSpan);
            row.setSettlementCode(settlement.getSettlementCode());
            row.setSettlementPeriod(settlement.getSettlementPeriod());
            row.setSettlementAmount(settlement.getSettlementAmount());
            row.setSettlementBizPerson(settlement.getBusinessPerson());
            settlementFirstRow = false;
          } else {
            row.setSettlementRowSpan(0); // 被合并，写入阶段跳过
          }

          // ── 发票 + 应收明细字段：每行均填入 ──
          if (invoice != null) {
            row.setInvoiceNumber(invoice.getInvoiceNumber());
            row.setInvoiceDate(invoice.getInvoiceDate());
            row.setInvoiceAmount(invoice.getInvoiceAmount());
            row.setTaxAmount(invoice.getTaxAmount());
            row.setTotalAmount(invoice.getTotalAmount());
            row.setInvoiceBizPerson(invoice.getBusinessPerson());
            row.setReceivableAmount(invoice.getReceivableAmount());
            row.setReceivedAmount(invoice.getReceivedAmount());
            if (invoice.getReceivedDate() != null) {
              row.setReceivedDate(invoice.getReceivedDate().toLocalDate());
            }
            row.setOutstandingAmount(invoice.getOutstandingAmount());
            row.setDaysToPayment(invoice.getDaysToPayment());
            row.setAccountAge(invoice.getAccountAge());
          }

          rows.add(row);
        }
      }
    }
    return rows;
  }

  /**
   * 搜索参数哈希函数
   */
  private String hashSearchParams(ReceivableDetailRequest request) {
    Map<String, Object> params = new LinkedHashMap<>();
    if (request.getContractNo() != null && !request.getContractNo().isEmpty()) {
      params.put("contractNo", request.getContractNo());
    }
    if (request.getPartyAName() != null && !request.getPartyAName().isEmpty()) {
      params.put("partyAName", request.getPartyAName());
    }
    if (request.getSettlementType() != null && !request.getSettlementType().isEmpty()) {
      params.put("settlementType", request.getSettlementType());
    }
    if (request.getPricingMode() != null && !request.getPricingMode().isEmpty()) {
      params.put("pricingMode", request.getPricingMode());
    }
    if (request.getDateStart() != null && !request.getDateStart().isEmpty()) {
      params.put("dateStart", request.getDateStart());
    }
    if (request.getDateEnd() != null && !request.getDateEnd().isEmpty()) {
      params.put("dateEnd", request.getDateEnd());
    }
    if (request.getSortField() != null && !request.getSortField().isEmpty()) {
      params.put("sortField", request.getSortField());
    }
    if (request.getSortOrder() != null && !request.getSortOrder().isEmpty()) {
      params.put("sortOrder", request.getSortOrder());
    }

    // 使用JSON字符串的哈希值作为缓存键的一部分
    String paramJson = params.toString();
    return String.valueOf(paramJson.hashCode());
  }
}
