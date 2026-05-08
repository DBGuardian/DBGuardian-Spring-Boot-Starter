package com.erp.service.report.impl;

import com.erp.controller.report.dto.*;
import com.erp.mapper.report.PayableDetailMapper;
import com.erp.service.report.PayableDetailService;
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
 * 应付账款明细表 Service 实现
 * 
 * 功能描述：
 * - 获取应付账款明细表数据（支持Redis缓存）
 * - 重新计算应付账款明细表（清除缓存）
 * - 清除所有应付账款缓存
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
public class PayableDetailServiceImpl implements PayableDetailService {

  private final PayableDetailMapper payableDetailMapper;
  private final RedisTemplate<String, Object> redisTemplate;

  private static final String CACHE_KEY_PREFIX = "payable:detail";
  private static final long CACHE_TTL = 604800; // 1周（7天）
  private static final long COMPUTING_TTL = 300; // 5分钟（防止重复计算）

  /**
   * 获取应付账款明细表数据
   * 
   * 流程：
   * 1. 先检查缓存
   * 2. 缓存不存在则计算并缓存
   * 3. 返回数据
   */
  @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
  @Override
  public PayableDetailResponse getDetailList(PayableDetailRequest request) {
    // 生成缓存键
    String cacheKey = generateCacheKey(request);

    // 尝试从缓存获取
    PayableDetailResponse cachedData = (PayableDetailResponse) redisTemplate.opsForValue().get(cacheKey);

    if (cachedData != null) {
      // 缓存命中，直接返回
      cachedData.setFromCache(true);
      log.info("应付账款明细表数据从缓存加载，缓存键：{}", cacheKey);
      return cachedData;
    }

    // 检查是否正在计算中（防止并发计算）
    String computingKey = cacheKey + ":computing";
    Boolean isComputing = (Boolean) redisTemplate.opsForValue().get(computingKey);

    if (isComputing != null && isComputing) {
      // 正在计算中，返回提示信息
      log.warn("应付账款明细表数据正在计算中，缓存键：{}", cacheKey);
      throw new RuntimeException("数据正在计算中，请稍候...");
    }

    // 标记为计算中
    redisTemplate.opsForValue().set(computingKey, true, Duration.ofSeconds(COMPUTING_TTL));

    try {
      // 执行计算
      log.info("开始计算应付账款明细表数据，缓存键：{}", cacheKey);
      PayableDetailResponse data = calculateDetailList(request);

      // 缓存结果
      redisTemplate.opsForValue().set(cacheKey, data, Duration.ofSeconds(CACHE_TTL));

      data.setFromCache(false);
      log.info("应付账款明细表数据计算完成，缓存键：{}", cacheKey);
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
  public PayableDetailResponse recalculateDetailList(PayableDetailRequest request) {
    // 生成缓存键
    String cacheKey = generateCacheKey(request);

    // 清除缓存
    redisTemplate.delete(cacheKey);
    log.info("应付账款明细表缓存已清除，缓存键：{}", cacheKey);

    // 重新计算
    return getDetailList(request);
  }

  /**
   * 清除所有应付账款缓存
   */
  @Override
  public void clearAllCache() {
    Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + ":*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
      log.info("已清除所有应付账款缓存，共 {} 个", keys.size());
    }
  }

  /**
   * 计算应付账款明细表数据
   */
  private PayableDetailResponse calculateDetailList(PayableDetailRequest request) {
    // 第一步：获取符合条件的合同ID列表（用于分页）
    List<Long> contractIds = payableDetailMapper.selectContractIds(request);
    
    if (contractIds.isEmpty()) {
      return new PayableDetailResponse(new ArrayList<>(), 0L, false, null);
    }

    // 第二步：获取指定合同的完整应付账款明细数据（需要传入contractIds）
    List<PayableDetailDTO> detailDTOs = payableDetailMapper.selectDetailListWithJoin(contractIds);

    // 第三步：批量查询计价方式
    Map<Long, String> pricingModeMap = new HashMap<>();
    if (!contractIds.isEmpty()) {
      List<ContractPricingModeDTO> pricingModes = payableDetailMapper.selectPricingModesByContractIds(contractIds);
      pricingModeMap = pricingModes.stream()
          .collect(Collectors.toMap(
              ContractPricingModeDTO::getContractId,
              ContractPricingModeDTO::getPricingMode
          ));
    }

    // 第四步：构建树形结构
    List<PayableContractRow> contractRows = buildContractRows(detailDTOs, pricingModeMap);

    // 获取总数
    Long total = (long) contractIds.size();

    return new PayableDetailResponse(contractRows, total, false, null);
  }

  /**
   * 构建树形结构数据
   */
  private List<PayableContractRow> buildContractRows(List<PayableDetailDTO> detailDTOs, Map<Long, String> pricingModeMap) {
    Map<Long, PayableContractRow> contractMap = new LinkedHashMap<>();
    Map<Long, PayableSettlementRow> settlementMap = new LinkedHashMap<>();

    int sequenceNo = 1;

    for (PayableDetailDTO dto : detailDTOs) {
      // 构建合同行
      if (!contractMap.containsKey(dto.getContractId())) {
        // 获取计价方式，如果为空则设置默认值
        String pricingMode = pricingModeMap.getOrDefault(dto.getContractId(), "未知");
        if ("未知".equals(pricingMode) && "业务费结算".equals(dto.getSettlementType())) {
          pricingMode = "业务费";
        }

        PayableContractRow contractRow = new PayableContractRow();
        contractRow.setContractId(dto.getContractId());
        contractRow.setContractNo(dto.getContractNo());
        contractRow.setContractSignDate(dto.getContractSignDate());
        contractRow.setPartyBName(dto.getPartyBName());
        contractRow.setContractAmount(dto.getContractAmount());
        contractRow.setSettlementType(dto.getSettlementType());
        contractRow.setPricingMode(pricingMode);
        contractRow.setBusinessPerson(dto.getContractCreatedBy());
        contractRow.setSequenceNo(sequenceNo++);
        contractRow.setRowKey("contract-" + dto.getContractId());
        contractRow.setIsDetailRow(false);
        contractRow.setChildren(new ArrayList<>());

        contractMap.put(dto.getContractId(), contractRow);
      }

      // 构建结算单行
      if (dto.getSettlementId() != null && !settlementMap.containsKey(dto.getSettlementId())) {
        PayableSettlementRow settlementRow = new PayableSettlementRow();
        settlementRow.setSettlementId(dto.getSettlementId());
        settlementRow.setSettlementCode(dto.getSettlementCode());
        settlementRow.setContractId(dto.getContractId());
        settlementRow.setSettlementPeriod(dto.getSettlementPeriod());
        settlementRow.setSettlementAmount(dto.getSettlementAmount());
        settlementRow.setPayableAmount(dto.getSettlementAmount());
        settlementRow.setPaidAmount(dto.getSettlementPaidAmount() != null ? dto.getSettlementPaidAmount() : BigDecimal.ZERO);
        settlementRow.setOutstandingAmount(
          dto.getSettlementAmount().subtract(settlementRow.getPaidAmount())
        );
        settlementRow.setBusinessPerson(dto.getSettlementCreatedBy());
        settlementRow.setRowKey("settlement-" + dto.getSettlementId());
        settlementRow.setIsDetailRow(false);
        settlementRow.setChildren(new ArrayList<>());

        settlementMap.put(dto.getSettlementId(), settlementRow);

        // 添加到合同的子列表
        PayableContractRow contractRow = contractMap.get(dto.getContractId());
        contractRow.getChildren().add(settlementRow);
        contractRow.setHasChildren(true);
      }

      // 构建发票+应付明细行
      if (dto.getInvoiceId() != null && dto.getApDetailId() != null) {
        PayableInvoiceDetailRow invoiceRow = new PayableInvoiceDetailRow(
          dto.getInvoiceId(),
          dto.getInvoiceNumber(),
          dto.getInvoiceDate(),
          dto.getInvoiceAmount(),
          dto.getTaxAmount(),
          dto.getTotalAmount(),
          dto.getApDetailId(),
          dto.getRelatedAmount(),
          BigDecimal.ZERO, // 初始化为0，后续可补充实际已付金额
          dto.getRelatedTime(),
          dto.getApCreatedBy(),
          dto.getInvoiceDate()
        );
        invoiceRow.setSettlementId(dto.getSettlementId());

        // 添加到结算单的子列表
        PayableSettlementRow settlementRow = settlementMap.get(dto.getSettlementId());
        if (settlementRow != null) {
          settlementRow.getChildren().add(invoiceRow);
          settlementRow.setHasChildren(true);
        }
      }
    }

    // 计算合同和结算单的已付金额和未付金额
    for (PayableContractRow contractRow : contractMap.values()) {
      BigDecimal totalPaidAmount = BigDecimal.ZERO;
      BigDecimal totalOutstandingAmount = BigDecimal.ZERO;

      for (PayableSettlementRow settlementRow : contractRow.getChildren()) {
        totalPaidAmount = totalPaidAmount.add(settlementRow.getPaidAmount());
        totalOutstandingAmount = totalOutstandingAmount.add(settlementRow.getOutstandingAmount());
      }

      contractRow.setPaidAmount(totalPaidAmount);
      contractRow.setOutstandingAmount(totalOutstandingAmount);
    }

    return new ArrayList<>(contractMap.values());
  }

  /**
   * 查询全量数据并展平为 Excel 行列表（用于导出，不分页）
   */
  @Override
  @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
  public List<PayableExcelRow> queryAllForExport(PayableDetailRequest request) {
    PayableDetailRequest fullRequest = new PayableDetailRequest();
    fullRequest.setPage(1);
    fullRequest.setSize(Integer.MAX_VALUE);
    fullRequest.setContractNo(request.getContractNo());
    fullRequest.setPartyBName(request.getPartyBName());
    fullRequest.setSettlementType(request.getSettlementType());
    fullRequest.setPricingMode(request.getPricingMode());
    fullRequest.setDateStart(request.getDateStart());
    fullRequest.setDateEnd(request.getDateEnd());

    List<Long> contractIds = payableDetailMapper.selectContractIds(fullRequest);
    if (contractIds.isEmpty()) {
      return new ArrayList<>();
    }

    List<PayableDetailDTO> detailDTOs = payableDetailMapper.selectDetailListWithJoin(contractIds);
    if (detailDTOs.isEmpty()) {
      return new ArrayList<>();
    }

    Map<Long, String> pricingModeMap = new HashMap<>();
    List<ContractPricingModeDTO> pricingModes = payableDetailMapper.selectPricingModesByContractIds(contractIds);
    pricingModeMap = pricingModes.stream()
        .collect(Collectors.toMap(
            ContractPricingModeDTO::getContractId,
            ContractPricingModeDTO::getPricingMode
        ));

    List<PayableContractRow> contractRows = buildContractRows(detailDTOs, pricingModeMap);
    List<PayableContractRow> exportRows = filterBySelectedKeys(contractRows, request.getSelectedKeys());

    return flattenToExcelRows(exportRows);
  }

  private List<PayableContractRow> filterBySelectedKeys(List<PayableContractRow> contracts, List<String> selectedKeys) {
    if (selectedKeys == null || selectedKeys.isEmpty()) {
      return contracts;
    }

    Set<String> selectedKeySet = new LinkedHashSet<>(selectedKeys);
    List<PayableContractRow> result = new ArrayList<>();

    for (PayableContractRow contract : contracts) {
      boolean contractSelected = selectedKeySet.contains(contract.getRowKey());
      List<PayableSettlementRow> selectedSettlements = new ArrayList<>();

      if (contract.getChildren() != null) {
        for (PayableSettlementRow settlement : contract.getChildren()) {
          boolean settlementSelected = contractSelected || selectedKeySet.contains(settlement.getRowKey());
          List<PayableInvoiceDetailRow> selectedInvoices = new ArrayList<>();

          if (settlement.getChildren() != null) {
            for (PayableInvoiceDetailRow invoice : settlement.getChildren()) {
              if (contractSelected || settlementSelected || selectedKeySet.contains(invoice.getRowKey())) {
                selectedInvoices.add(invoice);
              }
            }
          }

          if (settlementSelected || !selectedInvoices.isEmpty()) {
            PayableSettlementRow copiedSettlement = new PayableSettlementRow();
            copiedSettlement.setSettlementId(settlement.getSettlementId());
            copiedSettlement.setSettlementCode(settlement.getSettlementCode());
            copiedSettlement.setContractId(settlement.getContractId());
            copiedSettlement.setSettlementPeriod(settlement.getSettlementPeriod());
            copiedSettlement.setSettlementAmount(settlement.getSettlementAmount());
            copiedSettlement.setPayableAmount(settlement.getPayableAmount());
            copiedSettlement.setPaidAmount(settlement.getPaidAmount());
            copiedSettlement.setOutstandingAmount(settlement.getOutstandingAmount());
            copiedSettlement.setBusinessPerson(settlement.getBusinessPerson());
            copiedSettlement.setRowKey(settlement.getRowKey());
            copiedSettlement.setIsDetailRow(settlement.getIsDetailRow());
            copiedSettlement.setHasChildren(!selectedInvoices.isEmpty());
            copiedSettlement.setChildren(selectedInvoices);
            selectedSettlements.add(copiedSettlement);
          }
        }
      }

      if (contractSelected || !selectedSettlements.isEmpty()) {
        PayableContractRow copiedContract = new PayableContractRow();
        copiedContract.setContractId(contract.getContractId());
        copiedContract.setContractNo(contract.getContractNo());
        copiedContract.setContractSignDate(contract.getContractSignDate());
        copiedContract.setPartyBName(contract.getPartyBName());
        copiedContract.setContractAmount(contract.getContractAmount());
        copiedContract.setPaidAmount(contract.getPaidAmount());
        copiedContract.setOutstandingAmount(contract.getOutstandingAmount());
        copiedContract.setSettlementType(contract.getSettlementType());
        copiedContract.setPricingMode(contract.getPricingMode());
        copiedContract.setBusinessPerson(contract.getBusinessPerson());
        copiedContract.setSequenceNo(contract.getSequenceNo());
        copiedContract.setRowKey(contract.getRowKey());
        copiedContract.setIsDetailRow(contract.getIsDetailRow());
        copiedContract.setHasChildren(!selectedSettlements.isEmpty());
        copiedContract.setChildren(selectedSettlements);
        result.add(copiedContract);
      }
    }

    return result;
  }

  /**
   * 将树形结构展平为 PayableExcelRow 列表
   */
  private List<PayableExcelRow> flattenToExcelRows(List<PayableContractRow> contracts) {
    List<PayableExcelRow> rows = new ArrayList<>();
    int seq = 1;

    for (PayableContractRow contract : contracts) {
      List<PayableSettlementRow> settlements = contract.getChildren();
      if (settlements == null || settlements.isEmpty()) continue;

      // 合同跨行数 = 所有结算单的发票数量之和（无发票的结算单按1计）
      int contractRowSpan = settlements.stream()
          .mapToInt(s -> Math.max(
              s.getChildren() == null ? 0 : s.getChildren().size(), 1
          ))
          .sum();

      boolean contractFirstRow = true;

      for (PayableSettlementRow settlement : settlements) {
        List<PayableInvoiceDetailRow> invoices = settlement.getChildren();
        boolean hasInvoices = invoices != null && !invoices.isEmpty();
        int settlementRowSpan = hasInvoices ? invoices.size() : 1;
        boolean settlementFirstRow = true;

        int invoiceCount = hasInvoices ? invoices.size() : 1;
        for (int i = 0; i < invoiceCount; i++) {
          PayableInvoiceDetailRow invoice = hasInvoices ? invoices.get(i) : null;

          PayableExcelRow row = new PayableExcelRow();

          // ── 合同字段：只有首行填入 ──
          if (contractFirstRow) {
            row.setContractRowSpan(contractRowSpan);
            row.setContractSeq(seq++);
            row.setSettlementType(contract.getSettlementType());
            row.setPricingMode(contract.getPricingMode());
            row.setPartyBName(contract.getPartyBName());
            row.setContractNo(contract.getContractNo());
            row.setContractSignDate(contract.getContractSignDate());
            row.setContractAmount(contract.getContractAmount());
            row.setContractBizPerson(contract.getBusinessPerson());
            contractFirstRow = false;
          } else {
            row.setContractRowSpan(0);
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
            row.setSettlementRowSpan(0);
          }

          // ── 发票 + 应付明细字段：每行均填入 ──
          if (invoice != null) {
            row.setInvoiceNumber(invoice.getInvoiceNumber());
            row.setInvoiceDate(invoice.getInvoiceDate());
            row.setInvoiceAmount(invoice.getInvoiceAmount());
            row.setTaxAmount(invoice.getTaxAmount());
            row.setTotalAmount(invoice.getTotalAmount());
            row.setInvoiceBizPerson(invoice.getBusinessPerson());
            row.setPayableAmount(invoice.getPayableAmount());
            row.setPaidAmount(invoice.getPaidAmount());
            if (invoice.getPaidDate() != null) {
              row.setPaidDate(invoice.getPaidDate().toLocalDate());
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
   * 生成缓存键
   */
  private String generateCacheKey(PayableDetailRequest request) {
    StringBuilder key = new StringBuilder(CACHE_KEY_PREFIX);
    key.append(":page=").append(request.getPage() != null ? request.getPage() : 1);
    key.append(":size=").append(request.getSize() != null ? request.getSize() : 20);
    
    if (request.getContractNo() != null && !request.getContractNo().isEmpty()) {
      key.append(":contractNo=").append(request.getContractNo());
    }
    if (request.getPartyBName() != null && !request.getPartyBName().isEmpty()) {
      key.append(":partyBName=").append(request.getPartyBName());
    }
    if (request.getSettlementType() != null && !request.getSettlementType().isEmpty()) {
      key.append(":settlementType=").append(request.getSettlementType());
    }
    if (request.getDateStart() != null && !request.getDateStart().isEmpty()) {
      key.append(":dateStart=").append(request.getDateStart());
    }
    if (request.getDateEnd() != null && !request.getDateEnd().isEmpty()) {
      key.append(":dateEnd=").append(request.getDateEnd());
    }

    return key.toString();
  }
}
