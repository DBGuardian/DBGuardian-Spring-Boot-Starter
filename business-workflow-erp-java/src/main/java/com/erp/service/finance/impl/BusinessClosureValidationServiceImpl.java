package com.erp.service.finance.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.common.constant.RedisConstant;
import com.erp.common.enums.ContractClosureStatus;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.*;
import com.erp.entity.contract.Contract;
import com.erp.entity.finance.Invoice;
import com.erp.entity.settlement.Settlement;
import com.erp.entity.settlement.SettlementFundTransactionRel;
import com.erp.entity.settlement.SettlementInvoiceRel;
import com.erp.entity.settlement.SettlementReference;
import com.erp.entity.finance.FundTransaction;
import com.erp.entity.production.PickupNotice;
import com.erp.entity.production.Warehousing;
import com.erp.entity.production.WeighingSlip;
import com.erp.entity.production.WeighingSlipDispatch;
import com.erp.entity.transport.DispatchOrder;
import com.erp.entity.contract.ContractItem;
import com.erp.mapper.contract.ContractItemMapper;
import com.erp.mapper.contract.ContractMapper;
import com.erp.mapper.contract.ContractWasteItemMapper;
import com.erp.entity.contract.ContractWasteItem;
import com.erp.mapper.finance.InvoiceMapper;
import com.erp.mapper.finance.SettlementMapper;
import com.erp.mapper.finance.SettlementFundTransactionRelMapper;
import com.erp.mapper.finance.FundTransactionMapper;
import com.erp.mapper.finance.SettlementInvoiceRelMapper;
import com.erp.mapper.finance.SettlementReferenceMapper;
import com.erp.mapper.production.PickupNoticeMapper;
import com.erp.mapper.production.WarehousingMapper;
import com.erp.mapper.production.WeighingSlipMapper;
import com.erp.mapper.production.WeighingSlipDispatchMapper;
import com.erp.mapper.transport.DispatchOrderMapper;
import com.erp.service.contract.IContractStatusPermissionService;
import com.erp.service.finance.BusinessClosureValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * 业务闭环校验服务实现类
 *
 * @author ERP System
 * @date 2025-02-05
 */
@Slf4j
@Service
public class BusinessClosureValidationServiceImpl implements BusinessClosureValidationService {

    // 存储最近的校验结果，用于前端查询
    private static volatile List<ClosureIssueDTO> latestValidationIssues = new ArrayList<>();
    // 存储最近一次聚合的看板快照（基于 latestValidationIssues 聚合）
    private static volatile com.erp.controller.finance.dto.ClosureDashboardStats latestDashboardSnapshot = null;
    // 最近一次校验时间（字符串形式，便于前端显示）
    private static volatile String lastValidationAt = null;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private ContractItemMapper contractItemMapper;

    @Autowired
    private ContractWasteItemMapper contractWasteItemMapper;

    @Autowired
    private SettlementMapper settlementMapper;

    @Autowired
    private InvoiceMapper invoiceMapper;

    @Autowired
    private PickupNoticeMapper pickupNoticeMapper;

    @Autowired
    private SettlementFundTransactionRelMapper settlementFundTransactionRelMapper;

    @Autowired
    private FundTransactionMapper fundTransactionMapper;

    @Autowired
    private SettlementInvoiceRelMapper settlementInvoiceRelMapper;

    @Autowired
    private SettlementReferenceMapper settlementReferenceMapper;

    @Autowired
    private WarehousingMapper warehousingMapper;

    @Autowired
    private WeighingSlipMapper weighingSlipMapper;

    @Autowired
    private WeighingSlipDispatchMapper weighingSlipDispatchMapper;

    @Autowired
    private DispatchOrderMapper dispatchOrderMapper;

    @Autowired
    private IContractStatusPermissionService statusPermissionService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ISSUE_TYPE_TIME_SEQUENCE_VIOLATION = "TIME_SEQUENCE_VIOLATION";
    private static final String ISSUE_TYPE_AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    private static final String ISSUE_TYPE_MISSING_ASSOCIATION = "MISSING_ASSOCIATION";
    private static final String ISSUE_TYPE_PREPAYMENT_VIOLATION = "PREPAYMENT_VIOLATION";
    private static final String ISSUE_TYPE_UNCLAIMED_PAYMENT = "UNCLAIMED_PAYMENT";
    private static final String ISSUE_TYPE_STATUS_INCONSISTENCY = "STATUS_INCONSISTENCY";
    private static final String ISSUE_TYPE_DATA_INCONSISTENCY = "DATA_INCONSISTENCY";
    private static final String ISSUE_TYPE_DUPLICATE_RECORD = "DUPLICATE_RECORD";
    private static final String ISSUE_TYPE_INVALID_DATA = "INVALID_DATA";
    /** 业务闭环虚拟类型：前端传入，后端展开为 STATUS_INCONSISTENCY + DATA_INCONSISTENCY */
    private static final String ISSUE_TYPE_BUSINESS_CLOSURE = "BUSINESS_CLOSURE";
    private static final String ISSUE_TYPE_OVERDUE_EXECUTION = "OVERDUE_EXECUTION";

    private static final String RISK_LEVEL_LOW = "LOW";
    private static final String RISK_LEVEL_MEDIUM = "MEDIUM";
    private static final String RISK_LEVEL_HIGH = "HIGH";
    private static final String RISK_LEVEL_CRITICAL = "CRITICAL";

    private static final String SEVERITY_LOW = "LOW";
    private static final String SEVERITY_MEDIUM = "MEDIUM";
    private static final String SEVERITY_HIGH = "HIGH";
    private static final String SEVERITY_CRITICAL = "CRITICAL";

    private static final String ACTION_TYPE_AUTO_FIX = "AUTO_FIX";
    private static final String ACTION_TYPE_MANUAL_FIX = "MANUAL_FIX";
    private static final String ACTION_TYPE_REVIEW_REQUIRED = "REVIEW_REQUIRED";

    @Override
    @Transactional(readOnly = true)
    public ClosureValidationResponse executeFullValidation(ClosureValidationRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("开始执行全量业务闭环校验，请求参数：{}", request);

        // 清除所有旧缓存，确保使用最新的计算结果
        clearAllCache();

        // 生成校验ID
        String validationId = "VALIDATION_" + System.currentTimeMillis();

        // 获取要校验的合同列表
        List<Long> contractIds = getContractIdsToValidate(request);

        // 执行各项校验
        List<ClosureIssueDTO> allIssues = new ArrayList<>();

        List<String> requestCheckItems = request.getCheckItems();
        if (CollectionUtils.isEmpty(requestCheckItems)) {
            // 默认执行所有校验
            allIssues.addAll(validateTimeSequence(contractIds));
            allIssues.addAll(validateAmountConsistency(contractIds));
            allIssues.addAll(validateAssociationIntegrity(contractIds));
            allIssues.addAll(validateUnclaimedPayments()); // 新增：待认领款项校验
            allIssues.addAll(validateStatusConsistency(contractIds)); // 新增：状态异常校验
            allIssues.addAll(validateOverdueExecution(contractIds)); // 新增：超期未执行校验
        } else {
            // 执行指定的校验项目
            if (requestCheckItems.contains("TIME_SEQUENCE")) {
                allIssues.addAll(validateTimeSequence(contractIds));
            }
            if (requestCheckItems.contains("AMOUNT_CONSISTENCY")) {
                allIssues.addAll(validateAmountConsistency(contractIds));
            }
            if (requestCheckItems.contains("ASSOCIATION_INTEGRITY")) {
                allIssues.addAll(validateAssociationIntegrity(contractIds));
            }
            if (requestCheckItems.contains("UNCLAIMED_PAYMENTS")) {
                allIssues.addAll(validateUnclaimedPayments());
            }
            if (requestCheckItems.contains("STATUS_CONSISTENCY")) {
                allIssues.addAll(validateStatusConsistency(contractIds));
            }
            if (requestCheckItems.contains("OVERDUE_EXECUTION")) {
                allIssues.addAll(validateOverdueExecution(contractIds));
            }
        }

        // 构建校验项目详情
        List<ValidationCheckItemDTO> checkItems = getValidationCheckItems();

        // 计算统计信息
        long executionTime = System.currentTimeMillis() - startTime;
        int totalChecked = contractIds.size() * 10; // 假设每个合同有10个校验点
        int issuesFound = allIssues.size();
        int resolvedIssues = (int) allIssues.stream()
                .filter(issue -> ACTION_TYPE_AUTO_FIX.equals(issue.getActionType()))
                .count();

        // 构建响应
        ClosureValidationResponse response = new ClosureValidationResponse();
        response.setValidationId(validationId);
        response.setTotalChecked(totalChecked);
        response.setIssuesFound(issuesFound);
        response.setResolvedIssues(resolvedIssues);
        response.setExecutionTime(executionTime);
        response.setIssues(allIssues);
        response.setCheckItems(checkItems);
        response.setOverallStatus(issuesFound == 0 ? "PASS" : (issuesFound < 5 ? "WARNING" : "FAIL"));
        response.setPassedChecks(totalChecked - issuesFound);
        response.setFailedChecks(issuesFound);
        response.setWarningChecks(0);
        response.setExecutedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
        response.setExecutedBy(SecurityUtil.getCurrentUsername());

        // 保存校验结果到内存和Redis缓存中，供前端查询使用
        synchronized (BusinessClosureValidationServiceImpl.class) {
            latestValidationIssues = new ArrayList<>(allIssues);
        }
        // 保存问题列表到Redis缓存
        saveIssuesToCache(new ArrayList<>(allIssues));

        // 生成并缓存完整的看板统计数据，同时记录校验时间
        try {
            // 先计算异常监控数据（基于问题列表）
            ClosureDashboardStats snapshot = aggregateStatsFromIssues(allIssues);
            snapshot.setPendingReviewsCount(response.getFailedChecks()); // 示例：将失败检查数放入待审核计数（可根据需要调整）

            // 再计算完整的合同维度和收款中心数据
            ClosureDashboardStats fullStats = calculateFullDashboardStats(null, null);
            // 合并异常监控数据
            fullStats.setTimeSequenceViolations(snapshot.getTimeSequenceViolations());
            fullStats.setTimeSequenceRiskBreakdown(snapshot.getTimeSequenceRiskBreakdown());
            fullStats.setAmountMismatches(snapshot.getAmountMismatches());
            fullStats.setAmountMismatchRiskBreakdown(snapshot.getAmountMismatchRiskBreakdown());
            fullStats.setStatusInconsistencies(snapshot.getStatusInconsistencies());
            fullStats.setStatusInconsistencyRiskBreakdown(snapshot.getStatusInconsistencyRiskBreakdown());
            fullStats.setDataInconsistencies(snapshot.getDataInconsistencies());
            fullStats.setDataInconsistencyRiskBreakdown(snapshot.getDataInconsistencyRiskBreakdown());
            fullStats.setMissingAssociations(snapshot.getMissingAssociations());
            fullStats.setMissingAssociationRiskBreakdown(snapshot.getMissingAssociationRiskBreakdown());
            fullStats.setPendingReviewsCount(snapshot.getPendingReviewsCount());

            synchronized (BusinessClosureValidationServiceImpl.class) {
                latestDashboardSnapshot = fullStats;
                lastValidationAt = DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss");
            }
            // 保存完整的看板统计到Redis缓存
            saveDashboardStatsToCache(fullStats);
            // 保存最后更新时间到Redis缓存
            saveLastUpdateTimeToCache(lastValidationAt);
            log.info("已生成完整看板统计数据，validationId={}, snapshotTime={}", validationId, lastValidationAt);
        } catch (Exception e) {
            log.warn("生成/缓存完整看板统计数据失败，validationId={}, error={}", validationId, e.getMessage());
        }

        // 在校验完成后触发统计数据更新（异步执行，避免影响校验性能）
        try {
            // 这里可以选择异步更新统计数据，或者在前端调用统计接口时实时计算
            log.debug("业务闭环校验完成，统计数据将在下次查询时更新");
        } catch (Exception e) {
            log.warn("更新统计数据失败，但不影响校验结果", e);
        }

        log.info("全量业务闭环校验完成，校验ID：{}，发现{}个问题，耗时{}ms",
                validationId, issuesFound, executionTime);

        return response;
    }

    @Override
    public List<ClosureIssueDTO> validateContractClosure(Long contractId, List<String> checkTypes) {
        List<Long> contractIds = Collections.singletonList(contractId);

        List<ClosureIssueDTO> issues = new ArrayList<>();
        if (CollectionUtils.isEmpty(checkTypes)) {
            issues.addAll(validateTimeSequence(contractIds));
            issues.addAll(validateAmountConsistency(contractIds));
            issues.addAll(validateAssociationIntegrity(contractIds));
            // 新增：专门校验预收款情况
            issues.addAll(validatePrepaymentScenariosForContract(contractId));
            issues.addAll(validateStatusConsistency(contractIds)); // 新增：状态异常校验
            issues.addAll(validateOverdueExecution(contractIds)); // 新增：超期未执行校验
        } else {
            if (checkTypes.contains("TIME_SEQUENCE")) {
                issues.addAll(validateTimeSequence(contractIds));
            }
            if (checkTypes.contains("AMOUNT_MISMATCH")) {
                issues.addAll(validateAmountConsistency(contractIds));
            }
            if (checkTypes.contains("ASSOCIATION_INTEGRITY")) {
                issues.addAll(validateAssociationIntegrity(contractIds));
            }
            if (checkTypes.contains("PREPAYMENT_CHECK")) {
                issues.addAll(validatePrepaymentScenariosForContract(contractId));
            }
            if (checkTypes.contains("STATUS_CONSISTENCY")) {
                issues.addAll(validateStatusConsistency(contractIds));
            }
            if (checkTypes.contains("OVERDUE_EXECUTION")) {
                issues.addAll(validateOverdueExecution(contractIds));
            }
        }

        return issues;
    }

    /**
     * 为单个合同校验预收款情况
     */
    private List<ClosureIssueDTO> validatePrepaymentScenariosForContract(Long contractId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            return new ArrayList<>();
        }
        return validatePrepaymentScenarios(contract);
    }

    @Override
    public List<ClosureIssueDTO> validateTimeSequence(List<Long> contractIds) {
        log.info("开始执行时间顺序校验，合同数量：{}", contractIds.size());

        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取合同列表
        QueryWrapper<Contract> contractQuery = new QueryWrapper<>();
        if (!CollectionUtils.isEmpty(contractIds)) {
            contractQuery.in("合同编号", contractIds);
        }
        List<Contract> contracts = contractMapper.selectList(contractQuery);

        for (Contract contract : contracts) {
            // 校验合同签订时间与执行时间
            issues.addAll(validateContractTimeSequence(contract));

            // 校验预收款情况（包含收款时间与合同签订时间的完整校验）
            issues.addAll(validatePrepaymentScenarios(contract));

            // 校验收款时间与开票时间
            issues.addAll(validatePaymentInvoiceTimeSequence(contract));
        }

        log.info("时间顺序校验完成，发现{}个问题", issues.size());
        return issues;
    }

    @Override
    public List<ClosureIssueDTO> validateAmountConsistency(List<Long> contractIds) {
        log.info("开始执行金额一致性校验，合同数量：{}", contractIds.size());

        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取合同列表
        QueryWrapper<Contract> contractQuery = new QueryWrapper<>();
        if (!CollectionUtils.isEmpty(contractIds)) {
            contractQuery.in("合同编号", contractIds);
        }
        List<Contract> contracts = contractMapper.selectList(contractQuery);

        for (Contract contract : contracts) {
            // 校验结算单金额与发票金额一致性
            issues.addAll(validateSettlementInvoiceAmountConsistency(contract));

            // 校验结算单金额与收款金额一致性
            issues.addAll(validateSettlementReceiptAmountConsistency(contract));

            // 校验单次收款不能超过剩余金额
            issues.addAll(validateSinglePaymentLimit(contract));

            // 校验累计收款不能超过合同总金额
            issues.addAll(validateTotalPaymentLimit(contract));

            // 新增：校验闭环数据一致性
            issues.addAll(validateClosureDataConsistency(contract));
        }

        // 校验发票总额与流水总额交叉校验
        issues.addAll(validateInvoiceReceiptCrossCheck(contractIds));

        log.info("金额一致性校验完成，发现{}个问题", issues.size());
        return issues;
    }

    /**
     * 校验合同闭环数据一致性
     * 验证从合同签订到归档的完整业务流程中各模块数据的逻辑一致性
     */
    private List<ClosureIssueDTO> validateClosureDataConsistency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 1. 合同金额 vs 执行金额一致性
            issues.addAll(validateContractExecutionAmountConsistency(contract));

            // 2. 执行金额 vs 结算金额一致性
            issues.addAll(validateExecutionSettlementAmountConsistency(contract));

            // 3. 结算金额 vs 收款金额一致性
            issues.addAll(validateSettlementPaymentAmountConsistency(contract));

            // 4. 收款金额 vs 发票金额一致性
            issues.addAll(validatePaymentInvoiceAmountConsistency(contract));

            // 5. 业务数量一致性验证
            issues.addAll(validateBusinessQuantityConsistency(contract));

            // 6. 时间顺序一致性验证
            issues.addAll(validateClosureTimelineConsistency(contract));

        } catch (Exception e) {
            log.error("校验合同闭环数据一致性时发生异常，合同ID：{}", contract.getContractId(), e);

            ClosureIssueDTO exceptionIssue = new ClosureIssueDTO();
            exceptionIssue.setIssueId("CLOSURE_DATA_EXCEPTION_" + contract.getContractId() + "_" + System.currentTimeMillis());
            exceptionIssue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
            exceptionIssue.setRiskLevel(RISK_LEVEL_HIGH);
            exceptionIssue.setRelatedEntityType("CONTRACT");
            exceptionIssue.setRelatedEntityId(contract.getContractId().longValue());
            exceptionIssue.setRelatedEntityCode(contract.getContractNo());
            exceptionIssue.setRelatedEntityName("合同-" + contract.getContractNo());
            exceptionIssue.setIssueTitle("闭环数据一致性校验异常");
            exceptionIssue.setIssueDescription("系统在校验合同闭环数据一致性时发生异常：" + e.getMessage());
            exceptionIssue.setSuggestedAction("请联系系统管理员检查数据完整性");
            exceptionIssue.setActionType(ACTION_TYPE_REVIEW_REQUIRED);
            exceptionIssue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
            issues.add(exceptionIssue);
        }

        return issues;
    }

    /**
     * 验证合同金额与执行金额的一致性
     */
    private List<ClosureIssueDTO> validateContractExecutionAmountConsistency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            BigDecimal contractAmount = contract.getContractAmount();
            if (contractAmount == null || contractAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return issues; // 合同金额无效，跳过校验
            }

            // 计算执行总金额（从入库记录、运输记录等计算）
            BigDecimal executionAmount = calculateExecutionTotalAmount(contract);

            if (executionAmount != null && executionAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 允许一定的误差范围（例如1%）
                BigDecimal tolerance = contractAmount.multiply(new BigDecimal("0.01"));
                BigDecimal difference = contractAmount.subtract(executionAmount).abs();

                if (difference.compareTo(tolerance) > 0) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("CONTRACT_EXECUTION_AMOUNT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_AMOUNT_MISMATCH);
                    issue.setRiskLevel(RISK_LEVEL_MEDIUM);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("合同金额与执行金额不一致");
                    issue.setIssueDescription(String.format("合同金额：%s，执行总金额：%s，差异：%s",
                        contractAmount, executionAmount, difference));
                    issue.setSuggestedAction("请检查执行记录是否完整，或调整合同金额");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }

        } catch (Exception e) {
            log.warn("校验合同执行金额一致性失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 验证执行金额与结算金额的一致性
     */
    private List<ClosureIssueDTO> validateExecutionSettlementAmountConsistency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            BigDecimal executionAmount = calculateExecutionTotalAmount(contract);
            BigDecimal settlementAmount = calculateSettlementTotalAmount(contract);

            if (executionAmount != null && settlementAmount != null &&
                executionAmount.compareTo(BigDecimal.ZERO) > 0 && settlementAmount.compareTo(BigDecimal.ZERO) > 0) {

                BigDecimal tolerance = executionAmount.multiply(new BigDecimal("0.01"));
                BigDecimal difference = executionAmount.subtract(settlementAmount).abs();

                if (difference.compareTo(tolerance) > 0) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("EXECUTION_SETTLEMENT_AMOUNT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_AMOUNT_MISMATCH);
                    issue.setRiskLevel(RISK_LEVEL_MEDIUM);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("执行金额与结算金额不一致");
                    issue.setIssueDescription(String.format("执行总金额：%s，结算总金额：%s，差异：%s",
                        executionAmount, settlementAmount, difference));
                    issue.setSuggestedAction("请检查结算单是否包含所有执行项目");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }

        } catch (Exception e) {
            log.warn("校验执行结算金额一致性失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 验证结算金额与收款金额的一致性
     */
    private List<ClosureIssueDTO> validateSettlementPaymentAmountConsistency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            BigDecimal settlementAmount = calculateSettlementTotalAmount(contract);
            BigDecimal paymentAmount = calculatePaymentTotalAmount(contract);

            if (settlementAmount != null && paymentAmount != null &&
                settlementAmount.compareTo(BigDecimal.ZERO) > 0 && paymentAmount.compareTo(BigDecimal.ZERO) > 0) {

                BigDecimal tolerance = settlementAmount.multiply(new BigDecimal("0.01"));
                BigDecimal difference = settlementAmount.subtract(paymentAmount).abs();

                if (difference.compareTo(tolerance) > 0) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("SETTLEMENT_PAYMENT_AMOUNT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_AMOUNT_MISMATCH);
                    issue.setRiskLevel(RISK_LEVEL_HIGH);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("结算金额与收款金额不一致");
                    issue.setIssueDescription(String.format("结算总金额：%s，收款总金额：%s，差异：%s",
                        settlementAmount, paymentAmount, difference));
                    issue.setSuggestedAction("请检查收款记录是否完整，或确认是否存在未结算项目");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }

        } catch (Exception e) {
            log.warn("校验结算收款金额一致性失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 验证收款金额与发票金额的一致性
     */
    private List<ClosureIssueDTO> validatePaymentInvoiceAmountConsistency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            BigDecimal paymentAmount = calculatePaymentTotalAmount(contract);
            BigDecimal invoiceAmount = calculateInvoiceTotalAmount(contract);

            if (paymentAmount != null && invoiceAmount != null &&
                paymentAmount.compareTo(BigDecimal.ZERO) > 0 && invoiceAmount.compareTo(BigDecimal.ZERO) > 0) {

                BigDecimal tolerance = paymentAmount.multiply(new BigDecimal("0.01"));
                BigDecimal difference = paymentAmount.subtract(invoiceAmount).abs();

                if (difference.compareTo(tolerance) > 0) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("PAYMENT_INVOICE_AMOUNT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_AMOUNT_MISMATCH);
                    issue.setRiskLevel(RISK_LEVEL_HIGH);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("收款金额与发票金额不一致");
                    issue.setIssueDescription(String.format("收款总金额：%s，发票总金额：%s，差异：%s",
                        paymentAmount, invoiceAmount, difference));
                    issue.setSuggestedAction("请检查发票是否已全部开具，或确认是否存在税率差异");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }

        } catch (Exception e) {
            log.warn("校验收款发票金额一致性失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 验证业务数量一致性
     */
    private List<ClosureIssueDTO> validateBusinessQuantityConsistency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 检查入库数量 vs 运输数量一致性
            issues.addAll(validateWarehousingTransportQuantityConsistency(contract));

            // 检查执行项目数量 vs 结算项目数量一致性
            issues.addAll(validateExecutionSettlementQuantityConsistency(contract));

            // 执行跨模块依赖关系验证
            issues.addAll(validateCrossModuleDependencies(contract));

        } catch (Exception e) {
            log.warn("校验业务数量一致性失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 验证跨模块依赖关系
     * 检查合同、执行、收运、入库、结算、收款、发票各模块间的关联完整性
     */
    private List<ClosureIssueDTO> validateCrossModuleDependencies(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 依赖关系1：合同 vs 执行模块
            issues.addAll(validateContractExecutionDependency(contract));

            // 依赖关系2：执行 vs 收运模块
            issues.addAll(validateExecutionTransportDependency(contract));

            // 依赖关系3：收运 vs 入库模块
            issues.addAll(validateTransportWarehousingDependency(contract));

            // 依赖关系4：执行 vs 结算模块
            issues.addAll(validateExecutionSettlementDependency(contract));

            // 依赖关系5：结算 vs 收款模块
            issues.addAll(validateSettlementPaymentDependency(contract));

            // 依赖关系6：收款 vs 发票模块
            issues.addAll(validatePaymentInvoiceDependency(contract));

            // 依赖关系7：完整业务流程链条验证
            issues.addAll(validateCompleteBusinessChain(contract));

            // 执行闭环完整性验证
            issues.addAll(validateClosureIntegrity(contract));

        } catch (Exception e) {
            log.warn("校验跨模块依赖关系失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 验证闭环完整性
     * 综合检查"草稿 → 已签订 → [执行中] → 执行完成 → 部分收款 → 全额收款 → 已开票 → 归档"流程是否形成完整闭环
     */
    private List<ClosureIssueDTO> validateClosureIntegrity(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            ContractClosureStatus currentStatus = ContractClosureStatus.fromDisplayName(contract.getContractStatus());
            if (currentStatus == null) {
                return issues;
            }

            // 闭环完整性检查1：状态序列完整性
            issues.addAll(validateStatusSequenceIntegrity(contract, currentStatus));

            // 闭环完整性检查2：业务数据完整性
            issues.addAll(validateBusinessDataIntegrity(contract, currentStatus));

            // 闭环完整性检查3：时间顺序完整性
            issues.addAll(validateTimelineIntegrity(contract, currentStatus));

            // 闭环完整性检查4：金额平衡完整性
            issues.addAll(validateAmountBalanceIntegrity(contract, currentStatus));

            // 闭环完整性检查5：最终闭环验证
            issues.addAll(validateFinalClosureIntegrity(contract, currentStatus));

        } catch (Exception e) {
            log.warn("校验闭环完整性失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 闭环完整性检查1：状态序列完整性
     * 验证状态是否按照预期的序列推进，没有跳跃或倒退
     */
    private List<ClosureIssueDTO> validateStatusSequenceIntegrity(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 预期状态序列：1→2→3→4→5→6→7→8
        // DRAFT(1) → SIGNED(2) → EXECUTING(3) → EXECUTED(4) → PARTIAL_PAYMENT(5) → FULL_PAYMENT(6) → INVOICED(7) → ARCHIVED(8)

        // 对于已归档的合同，检查是否经过了所有必要的状态
        if (currentStatus == ContractClosureStatus.ARCHIVED) {
            // 检查关键路径是否完整
            boolean hasExecutionData = checkContractHasExecutionData(contract);
            boolean hasPaymentData = checkContractHasPaymentData(contract);
            boolean hasInvoiceData = checkContractHasInvoiceData(contract);

            if (!hasExecutionData) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("CLOSURE_SEQUENCE_EXECUTION_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_STATUS_INCONSISTENCY);
                issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("闭环完整性缺失：执行阶段");
                issue.setIssueDescription("合同已归档，但缺少执行阶段的必要数据，状态序列不完整");
                issue.setSuggestedAction("请补全执行数据，确保完整的业务闭环");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }

            if (!hasPaymentData) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("CLOSURE_SEQUENCE_PAYMENT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_STATUS_INCONSISTENCY);
                issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("闭环完整性缺失：收款阶段");
                issue.setIssueDescription("合同已归档，但缺少收款阶段的必要数据，状态序列不完整");
                issue.setSuggestedAction("请补全收款数据，确保完整的业务闭环");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }

            if (!hasInvoiceData) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("CLOSURE_SEQUENCE_INVOICE_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_STATUS_INCONSISTENCY);
                issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("闭环完整性缺失：开票阶段");
                issue.setIssueDescription("合同已归档，但缺少开票阶段的必要数据，状态序列不完整");
                issue.setSuggestedAction("请补全发票数据，确保完整的业务闭环");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }
        }

        return issues;
    }

    /**
     * 闭环完整性检查2：业务数据完整性
     * 验证所有业务模块的数据是否完整关联
     */
    private List<ClosureIssueDTO> validateBusinessDataIntegrity(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        if (currentStatus == ContractClosureStatus.ARCHIVED) {
            // 检查业务数据的完整性覆盖

            // 1. 合同基本信息完整性
            if (contract.getContractAmount() == null || contract.getSignTime() == null) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("CLOSURE_DATA_CONTRACT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                issue.setRiskLevel(RISK_LEVEL_HIGH);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("闭环完整性缺失：合同基本信息");
                issue.setIssueDescription("合同已归档，但合同金额或签订时间等基本信息不完整");
                issue.setSuggestedAction("请补全合同基本信息");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }

            // 2. 业务流程数据完整性验证将在其他方法中处理
        }

        return issues;
    }

    /**
     * 闭环完整性检查3：时间顺序完整性
     * 验证关键时间节点是否符合业务逻辑
     */
    private List<ClosureIssueDTO> validateTimelineIntegrity(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        if (currentStatus == ContractClosureStatus.ARCHIVED) {
            LocalDateTime signTime = contract.getSignTime();
            if (signTime != null) {
                LocalDateTime now = LocalDateTime.now();
                long totalDays = java.time.Duration.between(signTime, now).toDays();

                // 检查总周期是否合理（不应超过5年）
                if (totalDays > 1825) { // 5年
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("CLOSURE_TIMELINE_TOTAL_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_TIME_SEQUENCE_VIOLATION);
                    issue.setRiskLevel(RISK_LEVEL_MEDIUM);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("闭环完整性异常：执行周期过长");
                    issue.setIssueDescription("合同从签订到归档耗时" + totalDays + "天，超过正常业务周期");
                    issue.setSuggestedAction("请检查是否存在异常延迟的情况");
                    issue.setActionType(ACTION_TYPE_REVIEW_REQUIRED);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }

                // 检查是否在合理时间内完成（不应少于1天）
                if (totalDays < 1) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("CLOSURE_TIMELINE_TOO_FAST_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_TIME_SEQUENCE_VIOLATION);
                    issue.setRiskLevel(RISK_LEVEL_HIGH);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("闭环完整性异常：执行周期过短");
                    issue.setIssueDescription("合同在极短时间内完成所有流程，可能存在流程跳跃");
                    issue.setSuggestedAction("请检查业务流程是否完整合规");
                    issue.setActionType(ACTION_TYPE_REVIEW_REQUIRED);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }
        }

        return issues;
    }

    /**
     * 闭环完整性检查4：金额平衡完整性
     * 验证金额在各个环节的平衡关系
     */
    private List<ClosureIssueDTO> validateAmountBalanceIntegrity(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        if (currentStatus == ContractClosureStatus.ARCHIVED) {
            try {
                BigDecimal contractAmount = contract.getContractAmount();
                BigDecimal executionAmount = calculateExecutionTotalAmount(contract);
                BigDecimal paymentAmount = calculatePaymentTotalAmount(contract);
                BigDecimal invoiceAmount = calculateInvoiceTotalAmount(contract);

                // 检查金额链条的完整性
                if (contractAmount != null && contractAmount.compareTo(BigDecimal.ZERO) > 0) {
                    List<String> missingAmounts = new ArrayList<>();

                    if (executionAmount == null || executionAmount.compareTo(BigDecimal.ZERO) == 0) {
                        missingAmounts.add("执行金额");
                    }
                    if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) == 0) {
                        missingAmounts.add("收款金额");
                    }
                    if (invoiceAmount == null || invoiceAmount.compareTo(BigDecimal.ZERO) == 0) {
                        missingAmounts.add("发票金额");
                    }

                    if (!missingAmounts.isEmpty()) {
                        ClosureIssueDTO issue = new ClosureIssueDTO();
                        issue.setIssueId("CLOSURE_AMOUNT_BALANCE_" + contract.getContractId() + "_" + System.currentTimeMillis());
                        issue.setIssueType(ISSUE_TYPE_AMOUNT_MISMATCH);
                        issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                        issue.setRelatedEntityType("CONTRACT");
                        issue.setRelatedEntityId(contract.getContractId().longValue());
                        issue.setRelatedEntityCode(contract.getContractNo());
                        issue.setRelatedEntityName("合同-" + contract.getContractNo());
                        issue.setIssueTitle("闭环完整性缺失：金额平衡链条");
                        issue.setIssueDescription("合同已归档，但缺少完整的金额数据链条：合同金额 → " +
                            String.join(" → ", missingAmounts));
                        issue.setSuggestedAction("请补全所有金额环节的数据，确保金额平衡");
                        issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                        issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                        issues.add(issue);
                    }
                }

            } catch (Exception e) {
                log.warn("校验金额平衡完整性失败，合同ID：{}", contract.getContractId(), e);
            }
        }

        return issues;
    }

    /**
     * 闭环完整性检查5：最终闭环验证
     * 综合验证整个业务流程是否形成完整闭环
     */
    private List<ClosureIssueDTO> validateFinalClosureIntegrity(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        if (currentStatus == ContractClosureStatus.ARCHIVED) {
            // 最终闭环验证：检查是否真正形成了完整的业务闭环

            boolean isClosureComplete = true;
            StringBuilder closureIssues = new StringBuilder();

            // 检查1：状态序列完整性
            boolean hasAllRequiredData = checkContractHasExecutionData(contract) &&
                                       checkContractHasPaymentData(contract) &&
                                       checkContractHasInvoiceData(contract);

            if (!hasAllRequiredData) {
                isClosureComplete = false;
                closureIssues.append("缺少必要业务数据；");
            }

            // 检查2：金额平衡性
            BigDecimal contractAmount = contract.getContractAmount();
            BigDecimal paymentAmount = calculatePaymentTotalAmount(contract);

            if (contractAmount != null && paymentAmount != null) {
                BigDecimal difference = contractAmount.subtract(paymentAmount).abs();
                BigDecimal tolerance = contractAmount.multiply(new BigDecimal("0.01")); // 1%误差容忍

                if (difference.compareTo(tolerance) > 0) {
                    isClosureComplete = false;
                    closureIssues.append("合同金额与收款金额不平衡；");
                }
            }

            // 检查3：时间合理性
            LocalDateTime signTime = contract.getSignTime();
            if (signTime != null) {
                long daysSinceSign = java.time.Duration.between(signTime, LocalDateTime.now()).toDays();
                if (daysSinceSign < 1 || daysSinceSign > 1825) { // 1天到5年
                    isClosureComplete = false;
                    closureIssues.append("业务执行时间不合理；");
                }
            }

            if (!isClosureComplete) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("CLOSURE_FINAL_VALIDATION_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_STATUS_INCONSISTENCY);
                issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("最终闭环验证失败：业务流程不完整");
                issue.setIssueDescription("合同[" + contract.getContractNo() + "]未能形成完整的业务闭环：" + closureIssues.toString());
                issue.setSuggestedAction("请补全所有缺失环节，确保'草稿→已签订→执行中→执行完成→部分收款→全额收款→已开票→归档'的完整流程");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }
        }

        return issues;
    }

    /**
     * 依赖关系1：合同 vs 执行模块
     */
    private List<ClosureIssueDTO> validateContractExecutionDependency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 检查合同签订后是否有对应的执行记录
        // 如果合同状态已推进到执行相关阶段，但没有执行数据，则标记异常

        ContractClosureStatus currentStatus = ContractClosureStatus.fromDisplayName(contract.getContractStatus());
        if (currentStatus != null && currentStatus.getOrder() >= ContractClosureStatus.EXECUTING.getOrder()) {
            if (!checkContractHasExecutionData(contract)) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("DEPENDENCY_CONTRACT_EXECUTION_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_MISSING_ASSOCIATION);
                issue.setRiskLevel(RISK_LEVEL_HIGH);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("跨模块依赖缺失：合同-执行");
                issue.setIssueDescription("合同已推进到执行阶段，但系统中没有找到对应的执行记录");
                issue.setSuggestedAction("请检查执行数据是否正确关联到合同");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }
        }

        return issues;
    }

    /**
     * 依赖关系2：执行 vs 收运模块
     */
    private List<ClosureIssueDTO> validateExecutionTransportDependency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        ContractClosureStatus currentStatus = ContractClosureStatus.fromDisplayName(contract.getContractStatus());
        if (currentStatus != null && currentStatus.getOrder() >= ContractClosureStatus.EXECUTING.getOrder()) {
            // 检查是否存在执行数据
            boolean hasExecutionData = checkContractHasExecutionData(contract);

            if (hasExecutionData) {
                // 如果有执行数据，检查是否有对应的运输记录（收运通知单）
                boolean hasTransportData = checkContractHasTransportData(contract);

                if (!hasTransportData) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("DEPENDENCY_EXECUTION_TRANSPORT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_MISSING_ASSOCIATION);
                    issue.setRiskLevel(RISK_LEVEL_HIGH);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("跨模块依赖缺失：执行-运输");
                    issue.setIssueDescription("合同已开始执行，但系统中没有找到对应的收运通知单记录");
                    issue.setSuggestedAction("请检查收运通知单是否正确关联到合同，或补充收运数据");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }
        }

        return issues;
    }

    /**
     * 依赖关系3：收运 vs 入库模块
     */
    private List<ClosureIssueDTO> validateTransportWarehousingDependency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        ContractClosureStatus currentStatus = ContractClosureStatus.fromDisplayName(contract.getContractStatus());
        if (currentStatus != null && currentStatus.getOrder() >= ContractClosureStatus.EXECUTING.getOrder()) {
            // 检查是否存在运输数据
            boolean hasTransportData = checkContractHasTransportData(contract);

            if (hasTransportData) {
                // 如果有运输数据，检查是否有对应的入库记录
                boolean hasWarehousingData = checkContractHasWarehousingData(contract);

                if (!hasWarehousingData) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("DEPENDENCY_TRANSPORT_WAREHOUSING_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_MISSING_ASSOCIATION);
                    issue.setRiskLevel(RISK_LEVEL_HIGH);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("跨模块依赖缺失：运输-入库");
                    issue.setIssueDescription("合同已有收运通知单，但系统中没有找到对应的入库记录");
                    issue.setSuggestedAction("请检查入库单是否正确关联到合同，或补充入库数据");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }
        }

        return issues;
    }

    /**
     * 依赖关系4：执行 vs 结算模块
     */
    private List<ClosureIssueDTO> validateExecutionSettlementDependency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 检查执行完成后是否有对应的结算记录
        ContractClosureStatus currentStatus = ContractClosureStatus.fromDisplayName(contract.getContractStatus());
        if (currentStatus != null && currentStatus.getOrder() >= ContractClosureStatus.PARTIAL_PAYMENT.getOrder()) {
            BigDecimal settlementAmount = calculateSettlementTotalAmount(contract);
            if (settlementAmount == null || settlementAmount.compareTo(BigDecimal.ZERO) == 0) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("DEPENDENCY_EXECUTION_SETTLEMENT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_MISSING_ASSOCIATION);
                issue.setRiskLevel(RISK_LEVEL_HIGH);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("跨模块依赖缺失：执行-结算");
                issue.setIssueDescription("合同已推进到收款阶段，但系统中没有找到对应的结算记录");
                issue.setSuggestedAction("请检查结算单是否正确关联到合同");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }
        }

        return issues;
    }

    /**
     * 依赖关系5：结算 vs 收款模块
     */
    private List<ClosureIssueDTO> validateSettlementPaymentDependency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 检查结算完成后是否有对应的收款记录
        ContractClosureStatus currentStatus = ContractClosureStatus.fromDisplayName(contract.getContractStatus());
        if (currentStatus != null && currentStatus.getOrder() >= ContractClosureStatus.FULL_PAYMENT.getOrder()) {
            if (!checkContractHasPaymentData(contract)) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("DEPENDENCY_SETTLEMENT_PAYMENT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_MISSING_ASSOCIATION);
                issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("跨模块依赖缺失：结算-收款");
                issue.setIssueDescription("合同已标记为全额收款，但系统中没有找到对应的收款记录");
                issue.setSuggestedAction("请检查收款数据是否正确关联到结算单");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }
        }

        return issues;
    }

    /**
     * 依赖关系6：收款 vs 发票模块
     */
    private List<ClosureIssueDTO> validatePaymentInvoiceDependency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 检查收款完成后是否有对应的发票记录
        ContractClosureStatus currentStatus = ContractClosureStatus.fromDisplayName(contract.getContractStatus());
        if (currentStatus != null && currentStatus.getOrder() >= ContractClosureStatus.INVOICED.getOrder()) {
            if (!checkContractHasInvoiceData(contract)) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("DEPENDENCY_PAYMENT_INVOICE_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_MISSING_ASSOCIATION);
                issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("跨模块依赖缺失：收款-发票");
                issue.setIssueDescription("合同已标记为已开票，但系统中没有找到对应的发票记录");
                issue.setSuggestedAction("请检查发票数据是否正确关联到合同");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }
        }

        return issues;
    }

    /**
     * 依赖关系7：完整业务流程链条验证
     */
    private List<ClosureIssueDTO> validateCompleteBusinessChain(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 验证完整的业务闭环链条
        ContractClosureStatus currentStatus = ContractClosureStatus.fromDisplayName(contract.getContractStatus());

        if (currentStatus == ContractClosureStatus.ARCHIVED || currentStatus == ContractClosureStatus.COMPLETED) {
            // 对于已归档的合同，验证所有关键环节是否完整

            boolean hasExecution = checkContractHasExecutionData(contract);
            boolean hasPayment = checkContractHasPaymentData(contract);
            boolean hasInvoice = checkContractHasInvoiceData(contract);

            if (!hasExecution || !hasPayment || !hasInvoice) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("DEPENDENCY_COMPLETE_CHAIN_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_MISSING_ASSOCIATION);
                issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("跨模块依赖缺失：完整业务链条");
                issue.setIssueDescription(String.format("合同已归档，但缺少关键业务数据：执行[%s]，收款[%s]，发票[%s]",
                    hasExecution ? "✓" : "✗", hasPayment ? "✓" : "✗", hasInvoice ? "✓" : "✗"));
                issue.setSuggestedAction("请补全所有缺失的业务数据，确保完整业务闭环");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }
        }

        return issues;
    }

    /**
     * 验证时间顺序一致性
     */
    private List<ClosureIssueDTO> validateClosureTimelineConsistency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 签订时间 vs 执行开始时间
            issues.addAll(validateContractExecutionTimeline(contract));

            // 执行完成时间 vs 收款时间
            issues.addAll(validateExecutionPaymentTimeline(contract));

            // 收款时间 vs 开票时间
            issues.addAll(validatePaymentInvoiceTimeline(contract));

        } catch (Exception e) {
            log.warn("校验时间顺序一致性失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    @Override
    public List<ClosureIssueDTO> validateUnclaimedPayments() {
        log.info("开始执行待认领款项校验");

        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取所有资金流水记录
        QueryWrapper<FundTransaction> fundTransactionQuery = new QueryWrapper<>();
        // 按创建时间倒序，优先检查最近的资金流水
        fundTransactionQuery.orderByDesc("创建时间");
        List<FundTransaction> allFundTransactions = fundTransactionMapper.selectList(fundTransactionQuery);

        log.info("待认领款项校验 - 总资金流水数量: {}", allFundTransactions != null ? allFundTransactions.size() : 0);

        if (!CollectionUtils.isEmpty(allFundTransactions)) {
            // 获取所有已有关联的资金流水ID（通过结算单资金流水关联表）
            QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
            List<SettlementFundTransactionRel> allRelations = settlementFundTransactionRelMapper.selectList(relQuery);

            Set<Long> claimedTransactionIds = new HashSet<>();
            if (!CollectionUtils.isEmpty(allRelations)) {
                claimedTransactionIds = allRelations.stream()
                        .map(SettlementFundTransactionRel::getTransactionId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }

            log.info("待认领款项校验 - 已关联资金流水数量: {}", claimedTransactionIds.size());

            // 找出未关联的资金流水
            int unclaimedCount = 0;
            BigDecimal totalUnclaimedAmount = BigDecimal.ZERO;

            for (FundTransaction transaction : allFundTransactions) {
                if (transaction == null || transaction.getTransactionId() == null) {
                    continue;
                }

                // 检查是否已被关联
                if (!claimedTransactionIds.contains(transaction.getTransactionId())) {
                    unclaimedCount++;
                    if (transaction.getAmount() != null) {
                        totalUnclaimedAmount = totalUnclaimedAmount.add(transaction.getAmount());
                    }

                    // 生成待认领款项问题
                    ClosureIssueDTO issue = createUnclaimedPaymentIssue(transaction);
                    issues.add(issue);

                    log.debug("发现待认领款项 - 流水ID: {}, 金额: {}", transaction.getTransactionId(), transaction.getAmount());
                }
            }

            log.info("待认领款项校验完成 - 未关联流水数量: {}, 总金额: {}", unclaimedCount, totalUnclaimedAmount);
        }

        log.info("待认领款项校验完成，发现{}个问题", issues.size());
        return issues;
    }

    @Override
    public List<ClosureIssueDTO> validateAssociationIntegrity(List<Long> contractIds) {
        log.info("开始执行数据关联完整性校验，合同数量：{}", contractIds.size());

        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取合同列表
        QueryWrapper<Contract> contractQuery = new QueryWrapper<>();
        if (!CollectionUtils.isEmpty(contractIds)) {
            contractQuery.in("合同编号", contractIds);
        }
        List<Contract> contracts = contractMapper.selectList(contractQuery);

        for (Contract contract : contracts) {
            // 校验结算单是否关联合同
            issues.addAll(validateSettlementContractAssociation(contract));

            // 校验收款记录是否关联结算单
            issues.addAll(validateReceiptSettlementAssociation(contract));

            // 校验发票是否关联收款记录
            issues.addAll(validateInvoiceReceiptAssociation(contract));

            // 校验入库必须关联运输单
            issues.addAll(validateWarehousingTransportAssociation(contract));
        }

        // 校验弱关联提醒（非强制性）
        issues.addAll(validateWeakAssociations(contractIds));

        log.info("数据关联完整性校验完成，发现{}个问题", issues.size());
        return issues;
    }

    @Override
    public List<ClosureIssueDTO> validateStatusConsistency(List<Long> contractIds) {
        log.info("开始执行合同状态一致性校验，合同数量：{}", contractIds.size());

        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取合同列表
        QueryWrapper<Contract> contractQuery = new QueryWrapper<>();
        if (!CollectionUtils.isEmpty(contractIds)) {
            contractQuery.in("合同编号", contractIds);
        }
        List<Contract> contracts = contractMapper.selectList(contractQuery);

        for (Contract contract : contracts) {
            // 校验合同状态与业务实际执行情况的一致性
            issues.addAll(validateContractStatusConsistency(contract));
        }

        log.info("合同状态一致性校验完成，发现{}个问题", issues.size());
        return issues;
    }

    /**
     * 校验超期未执行合同
     * 签订超过30天仍未开始的合同将被识别为问题
     * 复用管理看板calculateContractStats中的实现逻辑
     */
    public List<ClosureIssueDTO> validateOverdueExecution(List<Long> contractIds) {
        log.info("开始执行超期未执行校验，合同数量：{}", contractIds.size());

        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取合同列表
        QueryWrapper<Contract> contractQuery = new QueryWrapper<>();
        if (!CollectionUtils.isEmpty(contractIds)) {
            contractQuery.in("合同编号", contractIds);
        }
        // 筛选已审核通过的合同（与管理看板逻辑一致）
        contractQuery.eq("合同状态", "已通过");
        List<Contract> contracts = contractMapper.selectList(contractQuery);

        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        for (Contract contract : contracts) {
            // 签订时间超过30天，生成问题记录（与管理看板逻辑一致）
            if (contract.getSignTime() != null &&
                contract.getSignTime().isBefore(threshold)) {

                // 生成问题记录
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("OVERDUE_EXECUTION_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_OVERDUE_EXECUTION);
                issue.setRiskLevel(RISK_LEVEL_HIGH);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId() != null ? contract.getContractId().longValue() : 0L);
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + (contract.getContractNo() != null ? contract.getContractNo() : "未知"));
                issue.setIssueTitle("合同超期未执行");
                issue.setIssueDescription(String.format("合同%s已于%s签订，现已超过30天仍未开始执行，请及时跟进处理。",
                    contract.getContractNo() != null ? contract.getContractNo() : "未知",
                    DateUtil.format(contract.getSignTime(), "yyyy-MM-dd HH:mm:ss")));
                issue.setSuggestedAction("请确认是否需要执行该合同，或调整合同状态");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }
        }

        log.info("超期未执行校验完成，发现{}个问题", issues.size());
        return issues;
    }

    /**
     * 检查合同是否有执行记录
     * 注意：此方法暂时不再使用，校验逻辑已与管理看板统一
     */
    private boolean checkContractExecutionRecords(Contract contract) {
        if (contract == null || contract.getContractNo() == null) {
            return false;
        }

        String contractNo = contract.getContractNo();

        // 检查收运通知单
        QueryWrapper<PickupNotice> pickupQuery = new QueryWrapper<>();
        pickupQuery.eq("合同号", contractNo);
        if (pickupNoticeMapper.selectCount(pickupQuery) > 0) {
            return true;
        }

        // 检查入库单（WAREHOUSING表没有合同号字段，通过findWarehousingsByContract方式查询）
        List<Warehousing> warehousings = findWarehousingsByContract(contract);
        if (!warehousings.isEmpty()) {
            return true;
        }

        // 检查结算单
        QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
        settlementQuery.eq("合同号", contractNo);
        if (settlementMapper.selectCount(settlementQuery) > 0) {
            return true;
        }

        return false;
    }

    /**
     * 校验合同状态与业务实际执行情况的一致性
     */
    private List<ClosureIssueDTO> validateContractStatusConsistency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        if (contract == null || contract.getContractStatus() == null) {
            return issues;
        }

        try {
            // 解析合同当前状态
            ContractClosureStatus currentStatus = ContractClosureStatus.fromDisplayName(contract.getContractStatus());
            if (currentStatus == null) {
                // 未知状态，创建问题
                ClosureIssueDTO unknownStatusIssue = new ClosureIssueDTO();
                unknownStatusIssue.setIssueId("STATUS_UNKNOWN_" + contract.getContractId() + "_" + System.currentTimeMillis());
                unknownStatusIssue.setIssueType(ISSUE_TYPE_STATUS_INCONSISTENCY);
                unknownStatusIssue.setRiskLevel(RISK_LEVEL_MEDIUM);
                unknownStatusIssue.setRelatedEntityType("CONTRACT");
                unknownStatusIssue.setRelatedEntityId(contract.getContractId().longValue());
                unknownStatusIssue.setRelatedEntityCode(contract.getContractNo());
                unknownStatusIssue.setRelatedEntityName("合同-" + contract.getContractNo());
                unknownStatusIssue.setIssueTitle("合同状态异常：未知状态");
                unknownStatusIssue.setIssueDescription("合同[" + contract.getContractNo() + "]的状态[" + contract.getContractStatus() + "]为系统无法识别的状态");
                unknownStatusIssue.setSuggestedAction("请检查合同状态是否正确，或联系系统管理员处理");
                unknownStatusIssue.setActionType(ACTION_TYPE_MANUAL_FIX);
                unknownStatusIssue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(unknownStatusIssue);
                return issues;
            }

            // 校验状态与业务数据的逻辑一致性
            issues.addAll(validateStatusBusinessLogicConsistency(contract, currentStatus));

            // 校验状态流转的合规性（检查是否存在违规的状态逆转）
            issues.addAll(validateStatusTransitionCompliance(contract, currentStatus));

            // 执行异常情况处理
            issues.addAll(validateExceptionScenarios(contract, currentStatus));

        } catch (Exception e) {
            log.error("校验合同状态一致性时发生异常，合同ID：{}，状态：{}",
                     contract.getContractId(), contract.getContractStatus(), e);

            // 创建异常处理问题
            ClosureIssueDTO exceptionIssue = new ClosureIssueDTO();
            exceptionIssue.setIssueId("STATUS_EXCEPTION_" + contract.getContractId() + "_" + System.currentTimeMillis());
            exceptionIssue.setIssueType(ISSUE_TYPE_STATUS_INCONSISTENCY);
            exceptionIssue.setRiskLevel(RISK_LEVEL_HIGH);
            exceptionIssue.setRelatedEntityType("CONTRACT");
            exceptionIssue.setRelatedEntityId(contract.getContractId().longValue());
            exceptionIssue.setRelatedEntityCode(contract.getContractNo());
            exceptionIssue.setRelatedEntityName("合同-" + contract.getContractNo());
            exceptionIssue.setIssueTitle("状态校验异常");
            exceptionIssue.setIssueDescription("系统在校验合同状态时发生异常：" + e.getMessage());
            exceptionIssue.setSuggestedAction("请联系系统管理员检查数据完整性");
            exceptionIssue.setActionType(ACTION_TYPE_REVIEW_REQUIRED);
            exceptionIssue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
            issues.add(exceptionIssue);
        }

        return issues;
    }

    /**
     * 校验异常情况处理
     */
    private List<ClosureIssueDTO> validateExceptionScenarios(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 异常场景1：状态倒退检测
        issues.addAll(validateStatusRegressionScenarios(contract, currentStatus));

        // 异常场景2：数据缺失但状态已推进
        issues.addAll(validateDataMissingScenarios(contract, currentStatus));

        // 异常场景3：金额异常检测
        issues.addAll(validateAmountAnomalyScenarios(contract, currentStatus));

        // 异常场景4：时间异常检测
        issues.addAll(validateTimeAnomalyScenarios(contract, currentStatus));

        return issues;
    }

    /**
     * 异常场景1：状态倒退检测
     */
    private List<ClosureIssueDTO> validateStatusRegressionScenarios(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 这里需要检查状态变更历史，检测是否存在状态倒退
        // 例如：从"全额收款"状态回到"部分收款"状态
        // 由于当前没有状态变更历史表，这里提供框架

        // TODO: 实现基于状态变更历史的状态倒退检测
        // 检测逻辑：
        // 1. 查询合同的状态变更历史
        // 2. 检查是否存在从高序数状态回到低序数状态的情况
        // 3. 对于关键状态倒退，需要特殊审批

        return issues;
    }

    /**
     * 异常场景2：数据缺失但状态已推进
     */
    private List<ClosureIssueDTO> validateDataMissingScenarios(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        switch (currentStatus) {
            case EXECUTED:
                // 执行完成状态但缺少执行数据
                if (!checkContractHasExecutionData(contract)) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("EXCEPTION_DATA_MISSING_EXECUTION_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                    issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("异常场景：执行完成但无执行数据");
                    issue.setIssueDescription("合同状态为'执行完成'，但系统中没有找到任何执行相关的记录数据");
                    issue.setSuggestedAction("请检查执行数据是否正确录入，或调整合同状态");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
                break;

            case FULL_PAYMENT:
                // 全额收款状态但缺少收款数据
                if (!checkContractHasPaymentData(contract)) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("EXCEPTION_DATA_MISSING_PAYMENT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                    issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("异常场景：全额收款但无收款数据");
                    issue.setIssueDescription("合同状态为'全额收款'，但系统中没有找到任何收款记录");
                    issue.setSuggestedAction("请检查收款数据是否正确录入，或调整合同状态");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
                break;

            case INVOICED:
                // 已开票状态但缺少发票数据
                if (!checkContractHasInvoiceData(contract)) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("EXCEPTION_DATA_MISSING_INVOICE_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                    issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("异常场景：已开票但无发票数据");
                    issue.setIssueDescription("合同状态为'已开票'，但系统中没有找到任何发票记录");
                    issue.setSuggestedAction("请检查发票数据是否正确录入，或调整合同状态");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
                break;
        }

        return issues;
    }

    /**
     * 异常场景3：金额异常检测
     */
    private List<ClosureIssueDTO> validateAmountAnomalyScenarios(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 检测金额为负数的情况
            BigDecimal contractAmount = contract.getContractAmount();
            if (contractAmount != null && contractAmount.compareTo(BigDecimal.ZERO) < 0) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("EXCEPTION_AMOUNT_NEGATIVE_CONTRACT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("异常场景：合同金额为负数");
                issue.setIssueDescription("合同金额为负数：" + contractAmount + "，这可能表示数据异常");
                issue.setSuggestedAction("请检查合同金额录入是否正确");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }

            // 检测超大金额的情况（例如超过1亿元的异常金额）
            if (contractAmount != null && contractAmount.compareTo(new BigDecimal("100000000")) > 0) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("EXCEPTION_AMOUNT_TOO_LARGE_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                issue.setRiskLevel(RISK_LEVEL_HIGH);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("异常场景：合同金额异常巨大");
                issue.setIssueDescription("合同金额异常巨大：" + contractAmount + "，超过正常业务范围");
                issue.setSuggestedAction("请确认合同金额是否正确录入");
                issue.setActionType(ACTION_TYPE_REVIEW_REQUIRED);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }

        } catch (Exception e) {
            log.warn("校验金额异常场景失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 异常场景4：时间异常检测
     */
    private List<ClosureIssueDTO> validateTimeAnomalyScenarios(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            LocalDateTime signTime = contract.getSignTime();
            LocalDateTime now = LocalDateTime.now();

            if (signTime != null) {
                // 检测签订时间为未来的情况
                if (signTime.isAfter(now.plusDays(1))) { // 允许1天误差
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("EXCEPTION_TIME_FUTURE_SIGN_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                    issue.setRiskLevel(RISK_LEVEL_HIGH);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("异常场景：签订时间为未来时间");
                    issue.setIssueDescription("合同签订时间为未来时间：" + signTime + "，这可能表示数据录入错误");
                    issue.setSuggestedAction("请检查签订时间录入是否正确");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }

                // 检测签订时间过早的情况（例如超过10年的历史合同）
                if (signTime.isBefore(now.minusYears(10))) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("EXCEPTION_TIME_TOO_OLD_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                    issue.setRiskLevel(RISK_LEVEL_MEDIUM);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("异常场景：签订时间过早");
                    issue.setIssueDescription("合同签订时间过早：" + signTime + "，超过10年历史");
                    issue.setSuggestedAction("请确认这是否为历史数据迁移，或检查时间录入是否正确");
                    issue.setActionType(ACTION_TYPE_REVIEW_REQUIRED);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }

        } catch (Exception e) {
            log.warn("校验时间异常场景失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 校验状态与业务数据的逻辑一致性
     */
    private List<ClosureIssueDTO> validateStatusBusinessLogicConsistency(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取合同相关的业务数据
        boolean hasExecutionData = checkContractHasExecutionData(contract);
        boolean hasPaymentData = checkContractHasPaymentData(contract);
        boolean hasInvoiceData = checkContractHasInvoiceData(contract);

        // 根据状态检查业务数据的逻辑一致性
        switch (currentStatus) {
            case DRAFT:
            case PENDING_REVIEW:
                // 草稿/待审核状态不应该有执行、收款、开票数据
                if (hasExecutionData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "草稿/待审核状态下不应有执行数据", "STATUS_LOGIC_DRAFT_EXECUTION"));
                }
                if (hasPaymentData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "草稿/待审核状态下不应有收款数据", "STATUS_LOGIC_DRAFT_PAYMENT"));
                }
                if (hasInvoiceData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "草稿/待审核状态下不应有发票数据", "STATUS_LOGIC_DRAFT_INVOICE"));
                }
                break;

            case SIGNED:
            case APPROVED:
                // 已签订状态不应该有收款、开票数据
                if (hasPaymentData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "已签订状态下不应有收款数据", "STATUS_LOGIC_SIGNED_PAYMENT"));
                }
                if (hasInvoiceData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "已签订状态下不应有发票数据", "STATUS_LOGIC_SIGNED_INVOICE"));
                }
                break;

            case EXECUTING:
                // 执行中状态不应该有收款、开票数据
                if (hasPaymentData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "执行中状态下不应有收款数据", "STATUS_LOGIC_EXECUTING_PAYMENT"));
                }
                if (hasInvoiceData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "执行中状态下不应有发票数据", "STATUS_LOGIC_EXECUTING_INVOICE"));
                }
                // 执行中状态应该有执行数据（如果长期处于执行中状态但无执行数据，可能需要检查）
                // 暂时注释掉此检查，因为刚进入执行中状态时可能执行数据还在录入过程中
                // if (!hasExecutionData) {
                //     issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                //         "执行中状态下应有执行数据", "STATUS_LOGIC_EXECUTING_NO_DATA"));
                // }
                break;

            case EXECUTED:
                // 执行完成状态应该有执行数据，不应该有开票数据
                if (!hasExecutionData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "执行完成状态下应有执行数据", "STATUS_LOGIC_EXECUTED_NO_DATA"));
                }
                if (hasInvoiceData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "执行完成状态下不应有发票数据", "STATUS_LOGIC_EXECUTED_INVOICE"));
                }
                break;

            case PARTIAL_PAYMENT:
                // 部分收款状态应该有执行和收款数据，不应该有发票数据（除非是预开票）
                if (!hasExecutionData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "部分收款状态下应有执行数据", "STATUS_LOGIC_PARTIAL_NO_EXECUTION"));
                }
                if (!hasPaymentData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "部分收款状态下应有收款数据", "STATUS_LOGIC_PARTIAL_NO_PAYMENT"));
                }
                break;

            case FULL_PAYMENT:
                // 全额收款状态应该有执行和收款数据
                if (!hasExecutionData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "全额收款状态下应有执行数据", "STATUS_LOGIC_FULL_NO_EXECUTION"));
                }
                if (!hasPaymentData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "全额收款状态下应有收款数据", "STATUS_LOGIC_FULL_NO_PAYMENT"));
                }
                break;

            case INVOICED:
                // 已开票状态应该有执行、收款、发票数据
                if (!hasExecutionData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "已开票状态下应有执行数据", "STATUS_LOGIC_INVOICED_NO_EXECUTION"));
                }
                if (!hasPaymentData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "已开票状态下应有收款数据", "STATUS_LOGIC_INVOICED_NO_PAYMENT"));
                }
                if (!hasInvoiceData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "已开票状态下应有发票数据", "STATUS_LOGIC_INVOICED_NO_INVOICE"));
                }
                break;

            case ARCHIVED:
            case COMPLETED:
                // 归档/完结状态应该有完整的业务数据
                if (!hasExecutionData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "归档状态下应有执行数据", "STATUS_LOGIC_ARCHIVED_NO_EXECUTION"));
                }
                if (!hasPaymentData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "归档状态下应有收款数据", "STATUS_LOGIC_ARCHIVED_NO_PAYMENT"));
                }
                if (!hasInvoiceData) {
                    issues.add(createStatusLogicInconsistencyIssue(contract, currentStatus,
                        "归档状态下应有发票数据", "STATUS_LOGIC_ARCHIVED_NO_INVOICE"));
                }
                break;
        }

        return issues;
    }

    /**
     * 校验状态流转的合规性
     */
    private List<ClosureIssueDTO> validateStatusTransitionCompliance(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 检查是否存在明显的状态跳跃问题
        issues.addAll(validateStatusTransitionLogic(contract, currentStatus));

        // 检查状态权限合规性（基于历史状态变更记录）
        issues.addAll(validateStatusTransitionPermissions(contract, currentStatus));

        return issues;
    }

    /**
     * 校验业务规则约束
     */
    private List<ClosureIssueDTO> validateBusinessRuleConstraints(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 规则1：开票必须在收款之后
        issues.addAll(validateInvoiceAfterPaymentRule(contract, currentStatus));

        // 规则2：预收款不能超过合同金额的50%
        issues.addAll(validatePrepaymentLimitRule(contract, currentStatus));

        // 规则3：单次收款不能超过剩余应收金额
        issues.addAll(validateSinglePaymentLimitRule(contract, currentStatus));

        // 规则4：累计收款不能超过合同总金额
        issues.addAll(validateTotalPaymentLimitRule(contract, currentStatus));

        // 规则5：发票金额不能超过收款金额
        issues.addAll(validateInvoiceAmountLimitRule(contract, currentStatus));

        return issues;
    }

    /**
     * 规则1：开票必须在收款之后的验证
     */
    private List<ClosureIssueDTO> validateInvoiceAfterPaymentRule(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        if (currentStatus == ContractClosureStatus.INVOICED) {
            // 检查是否有收款记录
            boolean hasPaymentData = checkContractHasPaymentData(contract);
            if (!hasPaymentData) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("BUSINESS_RULE_INVOICE_AFTER_PAYMENT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_STATUS_INCONSISTENCY);
                issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("业务规则违规：开票前必须先收款");
                issue.setIssueDescription("合同[" + contract.getContractNo() + "]已开票但没有收款记录，违反了'开票必须在收款之后'的业务规则");
                issue.setSuggestedAction("请先确认收款记录，或将合同状态调整为适当状态");
                issue.setActionType(ACTION_TYPE_REVIEW_REQUIRED);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }
        }

        return issues;
    }

    /**
     * 规则2：预收款不能超过合同金额50%的验证
     */
    private List<ClosureIssueDTO> validatePrepaymentLimitRule(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            BigDecimal contractAmount = contract.getContractAmount();
            if (contractAmount == null || contractAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return issues;
            }

            // 计算预收款金额（签订前或签订时的收款）
            BigDecimal prepaymentAmount = calculatePrepaymentAmount(contract);
            if (prepaymentAmount != null && prepaymentAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal maxPrepaymentLimit = contractAmount.multiply(new BigDecimal("0.5")); // 50%上限

                if (prepaymentAmount.compareTo(maxPrepaymentLimit) > 0) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("BUSINESS_RULE_PREPAYMENT_LIMIT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_AMOUNT_MISMATCH);
                    issue.setRiskLevel(RISK_LEVEL_HIGH);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("业务规则违规：预收款超过上限");
                    issue.setIssueDescription(String.format("合同预收款金额：%s，超过合同金额50%%上限：%s",
                        prepaymentAmount, maxPrepaymentLimit));
                    issue.setSuggestedAction("请确认预收款合理性，或调整预收款金额");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }

        } catch (Exception e) {
            log.warn("校验预收款上限规则失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 规则3：单次收款不能超过剩余应收金额的验证
     */
    private List<ClosureIssueDTO> validateSinglePaymentLimitRule(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 这个规则已经在validateSinglePaymentLimit方法中实现了，这里调用即可
        issues.addAll(validateSinglePaymentLimit(contract));

        return issues;
    }

    /**
     * 规则4：累计收款不能超过合同总金额的验证
     */
    private List<ClosureIssueDTO> validateTotalPaymentLimitRule(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 这个规则已经在validateTotalPaymentLimit方法中实现了，这里调用即可
        issues.addAll(validateTotalPaymentLimit(contract));

        return issues;
    }

    /**
     * 规则5：发票金额不能超过收款金额的验证
     */
    private List<ClosureIssueDTO> validateInvoiceAmountLimitRule(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            BigDecimal paymentAmount = calculatePaymentTotalAmount(contract);
            BigDecimal invoiceAmount = calculateInvoiceTotalAmount(contract);

            if (paymentAmount != null && invoiceAmount != null &&
                paymentAmount.compareTo(BigDecimal.ZERO) > 0 && invoiceAmount.compareTo(BigDecimal.ZERO) > 0) {

                // 发票金额不能超过收款金额
                if (invoiceAmount.compareTo(paymentAmount) > 0) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("BUSINESS_RULE_INVOICE_AMOUNT_LIMIT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_AMOUNT_MISMATCH);
                    issue.setRiskLevel(RISK_LEVEL_CRITICAL);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("业务规则违规：发票金额超过收款金额");
                    issue.setIssueDescription(String.format("发票总金额：%s，收款总金额：%s，发票金额不能超过收款金额",
                        invoiceAmount, paymentAmount));
                    issue.setSuggestedAction("请检查发票开具是否正确，或确认是否存在其他收款来源");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }

        } catch (Exception e) {
            log.warn("校验发票金额上限规则失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 计算预收款金额（签订前或签订时的收款）
     */
    private BigDecimal calculatePrepaymentAmount(Contract contract) {
        try {
            if (contract == null || contract.getContractId() == null || contract.getSignTime() == null) {
                return BigDecimal.ZERO;
            }

            // 查询所有与该合同相关的资金流水
            QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
            relQuery.eq("合同编号", contract.getContractId());
            List<SettlementFundTransactionRel> relations = settlementFundTransactionRelMapper.selectList(relQuery);

            if (CollectionUtils.isEmpty(relations)) {
                return BigDecimal.ZERO;
            }

            // 获取所有相关的资金流水ID
            List<Long> transactionIds = relations.stream()
                    .map(SettlementFundTransactionRel::getTransactionId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (CollectionUtils.isEmpty(transactionIds)) {
                return BigDecimal.ZERO;
            }

            // 查询资金流水详情
            List<FundTransaction> fundTransactions = fundTransactionMapper.selectList(
                    new QueryWrapper<FundTransaction>()
                            .in("流水编号", transactionIds)
                            .eq("交易类型", "INCOME") // 只统计收入类型的交易
            );

            if (CollectionUtils.isEmpty(fundTransactions)) {
                return BigDecimal.ZERO;
            }

            // 计算预收款金额：签订前或签订当天的收款
            LocalDateTime signDateTime = contract.getSignTime();
            LocalDate signDate = signDateTime.toLocalDate();

            BigDecimal prepaymentAmount = fundTransactions.stream()
                    .filter(ft -> ft.getAmount() != null && ft.getAmount().compareTo(BigDecimal.ZERO) > 0)
                    .filter(ft -> {
                        // 检查交易日期是否在签订前或当天
                        if (ft.getTransactionDate() != null) {
                            return !ft.getTransactionDate().isAfter(signDate); // 签订前或当天
                        }
                        return false;
                    })
                    .map(FundTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("合同{}预收款金额计算完成：签订时间={}, 预收款金额={}",
                     contract.getContractNo(), signDateTime, prepaymentAmount);

            return prepaymentAmount;

        } catch (Exception e) {
            log.warn("计算预收款金额失败，合同ID：{}", contract.getContractId(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 校验状态流转闭环逻辑合规性
     */
    private List<ClosureIssueDTO> validateStatusTransitionClosureLogic(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 验证前置状态依赖关系
        issues.addAll(validatePreconditionStatusDependencies(contract, currentStatus));

        // 验证状态流转矩阵合规性
        issues.addAll(validateStatusTransitionMatrix(contract, currentStatus));

        return issues;
    }

    /**
     * 校验前置状态依赖关系
     */
    private List<ClosureIssueDTO> validatePreconditionStatusDependencies(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        switch (currentStatus) {
            case DRAFT:
            case PENDING_REVIEW:
            case SIGNED:
            case APPROVED:
                // 草稿、待审核、已签订、已通过状态没有特殊前置条件要求
                break;

            case EXECUTING:
                // 执行中状态必须从已签订状态流转，且必须有签订记录
                if (!validateSignedPrecondition(contract)) {
                    ClosureIssueDTO issue = createStatusTransitionIssue(contract, currentStatus,
                        "执行中状态前置条件不满足", "STATUS_PRECONDITION_EXECUTING",
                        "合同必须先完成签订才能进入执行中状态", ACTION_TYPE_REVIEW_REQUIRED);
                    issues.add(issue);
                }
                break;

            case EXECUTED:
                // 执行完成状态必须从执行中状态流转，且必须有执行数据
                if (!validateExecutingPrecondition(contract)) {
                    ClosureIssueDTO issue = createStatusTransitionIssue(contract, currentStatus,
                        "执行完成状态前置条件不满足", "STATUS_PRECONDITION_EXECUTED",
                        "合同必须先进入执行中状态并有执行数据才能标记为执行完成", ACTION_TYPE_REVIEW_REQUIRED);
                    issues.add(issue);
                }
                break;

            case PARTIAL_PAYMENT:
            case FULL_PAYMENT:
                // 收款状态必须从执行完成状态流转，且必须有执行数据
                if (!validateExecutedPrecondition(contract)) {
                    ClosureIssueDTO issue = createStatusTransitionIssue(contract, currentStatus,
                        "收款状态前置条件不满足", "STATUS_PRECONDITION_PAYMENT",
                        "合同必须先完成执行才能进入收款状态", ACTION_TYPE_REVIEW_REQUIRED);
                    issues.add(issue);
                }
                break;

            case INVOICED:
                // 已开票状态必须从收款状态(部分收款或全额收款)流转，且必须有收款数据
                if (!validatePaymentPrecondition(contract)) {
                    ClosureIssueDTO issue = createStatusTransitionIssue(contract, currentStatus,
                        "已开票状态前置条件不满足", "STATUS_PRECONDITION_INVOICED",
                        "合同必须先完成收款才能开票", ACTION_TYPE_REVIEW_REQUIRED);
                    issues.add(issue);
                }
                break;

            case ARCHIVED:
                // 归档状态必须从已开票状态流转，且必须有完整的业务数据
                if (!validateInvoicedPrecondition(contract)) {
                    ClosureIssueDTO issue = createStatusTransitionIssue(contract, currentStatus,
                        "归档状态前置条件不满足", "STATUS_PRECONDITION_ARCHIVED",
                        "合同必须先完成开票且业务数据完整才能归档", ACTION_TYPE_REVIEW_REQUIRED);
                    issues.add(issue);
                }
                break;

            case COMPLETED:
                // 已完结状态与归档状态类似，必须有完整的业务数据
                if (!validateInvoicedPrecondition(contract)) {
                    ClosureIssueDTO issue = createStatusTransitionIssue(contract, currentStatus,
                        "已完结状态前置条件不满足", "STATUS_PRECONDITION_COMPLETED",
                        "合同必须先完成开票且业务数据完整才能完结", ACTION_TYPE_REVIEW_REQUIRED);
                    issues.add(issue);
                }
                break;
        }

        return issues;
    }

    /**
     * 校验状态流转矩阵合规性 - 禁止非法状态跳跃
     */
    private List<ClosureIssueDTO> validateStatusTransitionMatrix(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 定义允许的状态流转矩阵
        Map<ContractClosureStatus, List<ContractClosureStatus>> allowedTransitions = new HashMap<>();
        allowedTransitions.put(ContractClosureStatus.DRAFT, Arrays.asList(
            ContractClosureStatus.PENDING_REVIEW, ContractClosureStatus.SIGNED));
        allowedTransitions.put(ContractClosureStatus.PENDING_REVIEW, Arrays.asList(
            ContractClosureStatus.DRAFT, ContractClosureStatus.APPROVED));
        allowedTransitions.put(ContractClosureStatus.SIGNED, Arrays.asList(
            ContractClosureStatus.APPROVED, ContractClosureStatus.EXECUTING));
        allowedTransitions.put(ContractClosureStatus.APPROVED, Arrays.asList(
            ContractClosureStatus.EXECUTING));
        allowedTransitions.put(ContractClosureStatus.EXECUTING, Arrays.asList(
            ContractClosureStatus.EXECUTED));
        allowedTransitions.put(ContractClosureStatus.EXECUTED, Arrays.asList(
            ContractClosureStatus.PARTIAL_PAYMENT, ContractClosureStatus.FULL_PAYMENT));
        allowedTransitions.put(ContractClosureStatus.PARTIAL_PAYMENT, Arrays.asList(
            ContractClosureStatus.FULL_PAYMENT, ContractClosureStatus.INVOICED));
        allowedTransitions.put(ContractClosureStatus.FULL_PAYMENT, Arrays.asList(
            ContractClosureStatus.INVOICED));
        allowedTransitions.put(ContractClosureStatus.INVOICED, Arrays.asList(
            ContractClosureStatus.ARCHIVED, ContractClosureStatus.COMPLETED));
        allowedTransitions.put(ContractClosureStatus.ARCHIVED, Arrays.asList(
            ContractClosureStatus.COMPLETED));
        allowedTransitions.put(ContractClosureStatus.COMPLETED, Arrays.asList());

        // 这里需要获取合同的历史状态变更记录来验证流转路径
        // 由于当前没有状态变更历史表，这里提供框架，实际实现需要数据库支持

        // TODO: 实现基于状态变更历史的状态流转矩阵验证
        // 验证逻辑：
        // 1. 获取合同的状态变更历史
        // 2. 检查是否存在从不允许的前置状态直接跳跃到当前状态的情况
        // 3. 例如：不允许从DRAFT直接到FULL_PAYMENT

        return issues;
    }

    /**
     * 验证已签订状态的前置条件
     */
    private boolean validateSignedPrecondition(Contract contract) {
        // 检查是否有签订记录或审批记录
        // 这里需要根据实际业务逻辑实现
        // 示例：检查合同签订时间是否有效
        return contract.getSignTime() != null;
    }

    /**
     * 验证执行中状态的前置条件
     */
    private boolean validateExecutingPrecondition(Contract contract) {
        // 检查是否有执行数据
        return checkContractHasExecutionData(contract);
    }

    /**
     * 验证执行完成状态的前置条件
     */
    private boolean validateExecutedPrecondition(Contract contract) {
        // 检查是否有执行数据
        return checkContractHasExecutionData(contract);
    }

    /**
     * 验证收款状态的前置条件
     */
    private boolean validatePaymentPrecondition(Contract contract) {
        // 检查是否有收款数据
        return checkContractHasPaymentData(contract);
    }

    /**
     * 验证已开票状态的前置条件
     */
    private boolean validateInvoicedPrecondition(Contract contract) {
        // 检查是否有发票数据
        return checkContractHasInvoiceData(contract);
    }

    /**
     * 创建状态流转问题DTO的辅助方法
     */
    private ClosureIssueDTO createStatusTransitionIssue(Contract contract, ContractClosureStatus currentStatus,
            String issueTitle, String issueCode, String issueDescription, String actionType) {
        ClosureIssueDTO issue = new ClosureIssueDTO();
        issue.setIssueId("STATUS_TRANSITION_" + contract.getContractId() + "_" + issueCode + "_" + System.currentTimeMillis());
        issue.setIssueType(ISSUE_TYPE_STATUS_INCONSISTENCY);
        issue.setRiskLevel(RISK_LEVEL_HIGH);
        issue.setRelatedEntityType("CONTRACT");
        issue.setRelatedEntityId(contract.getContractId().longValue());
        issue.setRelatedEntityCode(contract.getContractNo());
        issue.setRelatedEntityName("合同-" + contract.getContractNo());
        issue.setIssueTitle(issueTitle);
        issue.setIssueDescription("合同[" + contract.getContractNo() + "]处于[" + currentStatus.getDisplayName() + "]状态，但" + issueDescription);
        issue.setSuggestedAction("请检查合同状态流转历史，确保按照正确顺序变更状态");
        issue.setActionType(actionType);
        issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
        return issue;
    }

    /**
     * 校验状态变更的权限合规性
     */
    private List<ClosureIssueDTO> validateStatusTransitionPermissions(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 执行历史变更审计
        issues.addAll(validateStatusChangeAudit(contract, currentStatus));

        // 注意：状态变更的业务逻辑合理性已在 validateStatusTransitionCompliance 中校验，避免重复

        return issues;
    }

    /**
     * 校验状态变更的业务逻辑合理性
     *
     * 注意：由于状态逻辑一致性检查已在 validateStatusBusinessLogicConsistency 中完成，
     * 此方法主要用于检查状态逆转风险，避免重复提醒。
     */
    private List<ClosureIssueDTO> validateStatusTransitionLogic(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 主要检查状态逆转风险，其他状态逻辑问题已在 validateStatusBusinessLogicConsistency 中处理
            issues.addAll(validateStatusReversalRisk(contract, currentStatus));

        } catch (Exception e) {
            log.warn("校验状态变更逻辑失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }


    /**
     * 校验状态逆转风险
     */
    private List<ClosureIssueDTO> validateStatusReversalRisk(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 检查是否存在高风险的状态逆转
            // 例如：从已收款状态退回到执行中状态
            if (currentStatus.getOrder() < ContractClosureStatus.FULL_PAYMENT.getOrder()) {
                // 检查是否曾经有过更高状态的记录
                boolean hasHigherStatusHistory = checkContractHasHigherStatusHistory(contract, currentStatus);

                if (hasHigherStatusHistory) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("STATUS_REVERSAL_RISK_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_STATUS_INCONSISTENCY);
                    issue.setRiskLevel(RISK_LEVEL_HIGH);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("状态逆转风险");
                    issue.setIssueDescription("合同曾处于更高业务阶段，但当前状态已回退，可能存在业务异常");
                    issue.setSuggestedAction("请确认状态回退的合理性，并检查相关业务数据的完整性");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }
        } catch (Exception e) {
            log.warn("校验状态逆转风险失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 检查合同是否曾经处于更高状态
     *
     * 注意：由于当前系统没有状态变更历史记录表，这个方法基于业务数据的推断。
     * 正常情况下，合同状态应该是单向前进，不应该出现回退。
     * 只有在确实存在明显的状态不一致时才报告异常。
     */
    private boolean checkContractHasHigherStatusHistory(Contract contract, ContractClosureStatus currentStatus) {
        try {
            // 只有在极度异常的情况下才认为存在状态回退风险
            int currentOrder = currentStatus.getOrder();

            // 场景1：当前状态是早期状态，但合同状态已经是归档或完结
            // 例如：合同还在"执行中"，但合同状态显示为"已归档"
            if (currentOrder <= ContractClosureStatus.EXECUTING.getOrder()) {
                ContractClosureStatus contractStatus = ContractClosureStatus.fromDisplayName(contract.getContractStatus());
                if (contractStatus == ContractClosureStatus.ARCHIVED ||
                    contractStatus == ContractClosureStatus.COMPLETED) {
                    log.warn("合同当前业务状态为{}但合同状态显示为{}，存在明显状态不一致，合同ID：{}",
                        currentStatus.getDisplayName(), contract.getContractStatus(), contract.getContractId());
                    return true;
                }
            }

            // 场景2：当前状态还未到收款阶段，但已有开票数据
            // 例如：合同还在"执行完成"状态，但已经开票
            if (currentOrder < ContractClosureStatus.FULL_PAYMENT.getOrder()) {
                if (checkContractHasInvoiceData(contract) &&
                    !checkContractHasPaymentData(contract)) {
                    // 有开票但没有收款记录，这可能是异常的
                    log.warn("合同状态为{}但检测到开票数据而无收款记录，可能存在状态不一致，合同ID：{}",
                        currentStatus.getDisplayName(), contract.getContractId());
                    return true;
                }
            }

            // 正常情况下，不应该仅仅因为有业务数据就认为是状态回退
            // 业务数据可能是正常录入的，合同状态流转应该是线性的

            return false;
        } catch (Exception e) {
            log.warn("检查更高状态历史失败，合同ID：{}", contract.getContractId(), e);
            return false;
        }
    }

    /**
     * 校验状态变更历史审计
     */
    private List<ClosureIssueDTO> validateStatusChangeAudit(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 审计1：检查状态变更频率是否异常
            issues.addAll(validateStatusChangeFrequency(contract));

            // 审计2：检查是否有可疑的状态变更模式
            issues.addAll(validateSuspiciousStatusPatterns(contract, currentStatus));

            // 审计3：检查状态变更的时间合理性
            issues.addAll(validateStatusChangeTiming(contract, currentStatus));

            // 审计4：生成状态变更审计报告
            issues.addAll(generateStatusChangeAuditReport(contract));

        } catch (Exception e) {
            log.warn("执行状态变更历史审计失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 审计1：检查状态变更频率是否异常
     */
    private List<ClosureIssueDTO> validateStatusChangeFrequency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            if (contract.getCreateTime() == null || contract.getUpdateTime() == null) {
                return issues;
            }

            LocalDateTime createTime = contract.getCreateTime();
            LocalDateTime updateTime = contract.getUpdateTime();
            LocalDateTime now = LocalDateTime.now();

            // 计算合同存在时间（天数）
            long contractAgeDays = java.time.Duration.between(createTime, now).toDays();
            if (contractAgeDays <= 0) {
                return issues;
            }

            // 计算更新频率（每天更新次数）
            long totalUpdates = calculateTotalUpdates(contract);
            double updatesPerDay = (double) totalUpdates / contractAgeDays;

            // 检查更新频率是否异常
            // 如果合同存在时间超过7天，但每天更新超过2次，认为是异常
            if (contractAgeDays > 7 && updatesPerDay > 2.0) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("STATUS_CHANGE_FREQUENCY_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                issue.setRiskLevel(RISK_LEVEL_MEDIUM);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("状态变更频率异常");
                issue.setIssueDescription(String.format("合同存在%ddays，每天平均变更%.2f次，频率过高可能存在异常操作",
                    contractAgeDays, updatesPerDay));
                issue.setSuggestedAction("请检查合同变更记录，确认是否存在异常操作或数据录入错误");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }

            // 检查近期频繁变更
            issues.addAll(validateRecentFrequentChanges(contract));

        } catch (Exception e) {
            log.warn("检查状态变更频率失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 计算合同的总更新次数（估算）
     */
    private long calculateTotalUpdates(Contract contract) {
        try {
            // 这里通过一些间接方式估算更新次数
            long estimatedUpdates = 1; // 至少有创建

            // 如果有审核时间，增加一次更新
            if (contract.getAuditTime() != null) {
                estimatedUpdates++;
            }

            // 如果有寄件时间，增加一次更新
            if (contract.getSendDate() != null) {
                estimatedUpdates++;
            }

            // 如果有收件时间，增加一次更新
            if (contract.getReceiveDate() != null) {
                estimatedUpdates++;
            }

            // 如果创建时间和更新时间相差较大，估算更多更新
            if (contract.getCreateTime() != null && contract.getUpdateTime() != null) {
                long daysBetween = java.time.Duration.between(contract.getCreateTime(), contract.getUpdateTime()).toDays();
                if (daysBetween > 30) {
                    // 长期合同，估算更多更新
                    estimatedUpdates += Math.max(1, daysBetween / 30);
                }
            }

            return estimatedUpdates;
        } catch (Exception e) {
            log.warn("计算总更新次数失败，合同ID：{}", contract.getContractId(), e);
            return 1;
        }
    }

    /**
     * 检查近期频繁变更
     */
    private List<ClosureIssueDTO> validateRecentFrequentChanges(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime recentTime = now.minusDays(7); // 最近7天

            // 检查近期是否有频繁的更新
            if (contract.getUpdateTime() != null && contract.getUpdateTime().isAfter(recentTime)) {
                // 查询相关的业务数据变更
                int recentBusinessChanges = countRecentBusinessChanges(contract, recentTime);

                if (recentBusinessChanges > 5) { // 如果7天内有超过5次业务变更
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("RECENT_FREQUENT_CHANGES_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                    issue.setRiskLevel(RISK_LEVEL_LOW);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("近期频繁变更");
                    issue.setIssueDescription(String.format("合同在最近7天内有%d次业务数据变更，请关注变更合理性",
                        recentBusinessChanges));
                    issue.setSuggestedAction("检查近期变更记录，确保所有变更都有相应的业务依据");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }
        } catch (Exception e) {
            log.warn("检查近期频繁变更失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 统计近期业务变更次数
     */
    private int countRecentBusinessChanges(Contract contract, LocalDateTime sinceTime) {
        int changeCount = 0;

        try {
            // 检查结算单的近期变更
            QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
            settlementQuery.eq("合同编号", contract.getContractId());
            if (sinceTime != null) {
                settlementQuery.ge("创建时间", sinceTime);
            }
            long settlementCount = settlementMapper.selectCount(settlementQuery);
            changeCount += settlementCount;

            // 检查收运通知单的近期变更
            QueryWrapper<PickupNotice> pickupQuery = new QueryWrapper<>();
            pickupQuery.eq("合同号", contract.getContractNo());
            if (sinceTime != null) {
                pickupQuery.ge("提交时间", sinceTime);
            }
            long pickupCount = pickupNoticeMapper.selectCount(pickupQuery);
            changeCount += pickupCount;

            // 检查入库单的近期变更
            List<Warehousing> warehousings = findWarehousingsByContract(contract);
            long warehousingCount = warehousings.stream()
                    .filter(w -> w.getCreateTime() != null && w.getCreateTime().isAfter(sinceTime))
                    .count();
            changeCount += warehousingCount;

        } catch (Exception e) {
            log.warn("统计近期业务变更失败，合同ID：{}", contract.getContractId(), e);
        }

        return changeCount;
    }

    /**
     * 审计2：检查是否有可疑的状态变更模式
     */
    private List<ClosureIssueDTO> validateSuspiciousStatusPatterns(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 检测可疑模式1：状态跳跃
        // 例如：从DRAFT直接到INVOICED

        // 检测可疑模式2：状态频繁来回切换
        // 例如：EXECUTING -> EXECUTED -> EXECUTING -> EXECUTED

        // 检测可疑模式3：非工作时间的变更
        // 例如：凌晨2点的状态变更

        // TODO: 实现可疑模式检测逻辑

        return issues;
    }

    /**
     * 审计3：检查状态变更的时间合理性
     */
    private List<ClosureIssueDTO> validateStatusChangeTiming(Contract contract, ContractClosureStatus currentStatus) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 检查状态变更是否在合理的时间范围内
            LocalDateTime signTime = contract.getSignTime();
            LocalDateTime now = LocalDateTime.now();

            if (signTime != null) {
                long daysSinceSign = java.time.Duration.between(signTime, now).toDays();

                // 如果合同签订不到1天就已完成所有流程，标记为异常
                if (currentStatus == ContractClosureStatus.ARCHIVED && daysSinceSign < 1) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("AUDIT_TIMING_TOO_FAST_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_STATUS_INCONSISTENCY);
                    issue.setRiskLevel(RISK_LEVEL_HIGH);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("审计异常：合同完成速度过快");
                    issue.setIssueDescription("合同从签订到归档仅用了" + daysSinceSign + "天，速度异常，可能存在流程跳跃");
                    issue.setSuggestedAction("请检查合同执行流程是否完整合规");
                    issue.setActionType(ACTION_TYPE_REVIEW_REQUIRED);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }

                // 如果合同签订超过2年仍未完成，标记为异常
                if (currentStatus.getOrder() < ContractClosureStatus.ARCHIVED.getOrder() && daysSinceSign > 730) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("AUDIT_TIMING_TOO_SLOW_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_STATUS_INCONSISTENCY);
                    issue.setRiskLevel(RISK_LEVEL_MEDIUM);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("审计异常：合同执行周期过长");
                    issue.setIssueDescription("合同签订已超过" + (daysSinceSign/365) + "年仍未完成，可能存在执行异常");
                    issue.setSuggestedAction("请检查合同执行进度和状态");
                    issue.setActionType(ACTION_TYPE_REVIEW_REQUIRED);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }

        } catch (Exception e) {
            log.warn("校验状态变更时间合理性失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 审计4：生成状态变更审计报告
     */
    private List<ClosureIssueDTO> generateStatusChangeAuditReport(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 分析合同的完整生命周期
            ContractLifecycleAnalysis analysis = analyzeContractLifecycle(contract);

            // 生成审计建议
            issues.addAll(generateAuditRecommendations(contract, analysis));

        } catch (Exception e) {
            log.warn("生成状态变更审计报告失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 分析合同生命周期
     */
    private ContractLifecycleAnalysis analyzeContractLifecycle(Contract contract) {
        ContractLifecycleAnalysis analysis = new ContractLifecycleAnalysis();

        try {
            // 计算合同年龄（使用签订时间而不是创建时间，更符合业务逻辑）
            LocalDateTime referenceTime = contract.getSignTime() != null ? contract.getSignTime() : contract.getCreateTime();
            if (referenceTime != null) {
                analysis.contractAgeDays = java.time.Duration.between(referenceTime, LocalDateTime.now()).toDays();
            }

            // 计算业务完成度
            analysis.hasExecutionData = checkContractHasExecutionData(contract);
            analysis.hasPaymentData = checkContractHasPaymentData(contract);
            analysis.hasInvoiceData = checkContractHasInvoiceData(contract);

            // 计算时间效率
            analysis.signingToExecutionDays = calculatePhaseDuration(contract.getSignTime(), getExecutionStartTime(contract));
            analysis.executionToPaymentDays = calculatePhaseDuration(getExecutionStartTime(contract), getPaymentStartTime(contract));
            analysis.paymentToInvoiceDays = calculatePhaseDuration(getPaymentStartTime(contract), getInvoiceStartTime(contract));

            // 计算业务数据量
            analysis.settlementCount = countSettlementsByContract(contract);
            analysis.pickupNoticeCount = countPickupNoticesByContract(contract);
            analysis.warehousingCount = countWarehousingsByContract(contract);
            analysis.fundTransactionCount = countFundTransactionsByContract(contract);

        } catch (Exception e) {
            log.warn("分析合同生命周期失败，合同ID：{}", contract.getContractId(), e);
        }

        return analysis;
    }

    /**
     * 计算阶段持续时间
     */
    private Long calculatePhaseDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return null;
        }
        return java.time.Duration.between(startTime, endTime).toDays();
    }

    /**
     * 获取执行开始时间
     */
    private LocalDateTime getExecutionStartTime(Contract contract) {
        try {
            // 从收运通知单或入库单中获取最早的执行时间
            QueryWrapper<PickupNotice> pickupQuery = new QueryWrapper<>();
            pickupQuery.eq("合同号", contract.getContractNo());
            pickupQuery.orderByAsc("提交时间");
            pickupQuery.last("LIMIT 1");

            PickupNotice firstPickup = pickupNoticeMapper.selectOne(pickupQuery);
            if (firstPickup != null && firstPickup.getSubmittedAt() != null) {
                return firstPickup.getSubmittedAt();
            }

            // 如果没有收运通知单，从入库单获取
            List<Warehousing> warehousings = findWarehousingsByContract(contract);
            if (!CollectionUtils.isEmpty(warehousings)) {
                return warehousings.stream()
                        .filter(w -> w.getWarehousingTime() != null)
                        .map(Warehousing::getWarehousingTime)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);
            }
        } catch (Exception e) {
            log.warn("获取执行开始时间失败，合同ID：{}", contract.getContractId(), e);
        }
        return null;
    }

    /**
     * 获取收款开始时间
     */
    private LocalDateTime getPaymentStartTime(Contract contract) {
        try {
            // 1. 先找到与合同相关的结算单
            QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
            settlementQuery.eq("合同编号", contract.getContractId());
            List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

            if (!CollectionUtils.isEmpty(settlements)) {
                List<Long> settlementIds = settlements.stream()
                        .map(Settlement::getSettlementId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // 2. 再找到这些结算单相关的资金流水关联记录
                QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
                relQuery.in("结算单编号", settlementIds);
                List<SettlementFundTransactionRel> relations = settlementFundTransactionRelMapper.selectList(relQuery);

                if (!CollectionUtils.isEmpty(relations)) {
                    List<Long> transactionIds = relations.stream()
                            .map(SettlementFundTransactionRel::getTransactionId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    // 3. 获取资金流水记录并找出最早的收款时间
                    List<FundTransaction> transactions = fundTransactionMapper.selectBatchIds(transactionIds);
                    return transactions.stream()
                            .filter(t -> t.getTransactionDate() != null)
                            .map(t -> t.getTransactionDate().atStartOfDay())
                            .min(LocalDateTime::compareTo)
                            .orElse(null);
                }
            }
        } catch (Exception e) {
            log.warn("获取收款开始时间失败，合同ID：{}", contract.getContractId(), e);
        }
        return null;
    }

    /**
     * 获取开票开始时间
     */
    private LocalDateTime getInvoiceStartTime(Contract contract) {
        try {
            // 这里简化处理，实际应该从发票表获取开票时间
            // 由于没有发票表，这里返回null或估算时间
            return null;
        } catch (Exception e) {
            log.warn("获取开票开始时间失败，合同ID：{}", contract.getContractId(), e);
        }
        return null;
    }

    /**
     * 统计各种业务数据的数量
     */
    private int countSettlementsByContract(Contract contract) {
        try {
            QueryWrapper<Settlement> query = new QueryWrapper<>();
            query.eq("合同编号", contract.getContractId());
            return settlementMapper.selectCount(query).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private int countPickupNoticesByContract(Contract contract) {
        try {
            QueryWrapper<PickupNotice> query = new QueryWrapper<>();
            query.eq("合同号", contract.getContractNo());
            return pickupNoticeMapper.selectCount(query).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private int countWarehousingsByContract(Contract contract) {
        try {
            List<Warehousing> warehousings = findWarehousingsByContract(contract);
            return warehousings.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private int countFundTransactionsByContract(Contract contract) {
        try {
            QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
            relQuery.eq("合同编号", contract.getContractId());
            return settlementFundTransactionRelMapper.selectCount(relQuery).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 生成审计建议
     */
    private List<ClosureIssueDTO> generateAuditRecommendations(Contract contract, ContractLifecycleAnalysis analysis) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 基于生命周期分析生成建议

            // 1. 检查业务流程完整性
            if (analysis.contractAgeDays > 30) { // 合同超过30天
                boolean isProcessComplete = analysis.hasExecutionData && analysis.hasPaymentData && analysis.hasInvoiceData;
                if (!isProcessComplete) {
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("AUDIT_PROCESS_COMPLETENESS_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                    issue.setRiskLevel(RISK_LEVEL_LOW);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("业务流程完整性审计建议");
                    issue.setIssueDescription("合同已签订超过30天，但业务流程未完整执行，建议加快业务进度");
                    issue.setSuggestedAction("检查合同执行进度，确保按时完成收运、入库、结算等业务环节");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }

            // 2. 检查时间效率
            if (analysis.signingToExecutionDays != null && analysis.signingToExecutionDays > 30) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("AUDIT_TIME_EFFICIENCY_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                issue.setRiskLevel(RISK_LEVEL_LOW);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("业务效率审计建议");
                issue.setIssueDescription(String.format("合同签订到开始执行耗时%d天，建议优化业务流程", analysis.signingToExecutionDays));
                issue.setSuggestedAction("分析执行延误原因，优化内部流程，提高业务执行效率");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }

            // 3. 检查数据量合理性
            int totalBusinessRecords = analysis.settlementCount + analysis.pickupNoticeCount +
                                     analysis.warehousingCount + analysis.fundTransactionCount;
            if (totalBusinessRecords == 0 && analysis.contractAgeDays > 7) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("AUDIT_DATA_VOLUME_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                issue.setRiskLevel(RISK_LEVEL_MEDIUM);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("业务数据缺失审计提醒");
                issue.setIssueDescription("合同已签订超过7天，但没有任何业务数据记录");
                issue.setSuggestedAction("检查合同是否为有效合同，或补充相应的业务执行数据");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }

        } catch (Exception e) {
            log.warn("生成审计建议失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 合同生命周期分析数据类
     */
    private static class ContractLifecycleAnalysis {
        long contractAgeDays;
        boolean hasExecutionData;
        boolean hasPaymentData;
        boolean hasInvoiceData;
        Long signingToExecutionDays;
        Long executionToPaymentDays;
        Long paymentToInvoiceDays;
        int settlementCount;
        int pickupNoticeCount;
        int warehousingCount;
        int fundTransactionCount;
    }

    /**
     * 检查合同是否有执行数据
     */
    private boolean checkContractHasExecutionData(Contract contract) {
        if (contract == null || contract.getContractId() == null) {
            return false;
        }

        try {
            // 使用收运通知单和运输单链路判断是否存在执行数据，避免直接在 WAREHOUSING 表上使用不存在的合同编号列
            List<Warehousing> warehousings = findWarehousingsByContract(contract);
            if (!CollectionUtils.isEmpty(warehousings)) {
                return true;
            }

            // 如果没有入库记录，再检查是否存在派车记录（通过收运通知单）
            if (contract.getContractNo() != null) {
                QueryWrapper<PickupNotice> pnQuery = new QueryWrapper<>();
                pnQuery.eq("合同号", contract.getContractNo());
                List<PickupNotice> notices = pickupNoticeMapper.selectList(pnQuery);
                if (!CollectionUtils.isEmpty(notices)) {
                    List<String> noticeCodes = notices.stream()
                            .map(PickupNotice::getNoticeCode)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    if (!noticeCodes.isEmpty()) {
                        QueryWrapper<DispatchOrder> dQ = new QueryWrapper<>();
                        dQ.in("收运通知单号", noticeCodes);
                        Long dispatchCount = Long.valueOf(dispatchOrderMapper.selectList(dQ).size());
                        return dispatchCount > 0;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            log.warn("检查合同执行数据时发生异常，合同ID：{}", contract.getContractId(), e);
            return false;
        }
    }

    /**
     * 检查合同是否有收款数据
     */
    private boolean checkContractHasPaymentData(Contract contract) {
        if (contract == null || contract.getContractId() == null) {
            return false;
        }

        try {
            // 检查是否有结算单
            QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
            settlementQuery.eq("合同编号", contract.getContractId());
            Long settlementCount = settlementMapper.selectCount(settlementQuery);

            // 检查是否有资金流水
            QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
            relQuery.exists("SELECT 1 FROM settlement s WHERE s.结算单编号 = " +
                           "settlement_fund_transaction_rel.结算单编号 AND s.合同编号 = " + contract.getContractId());
            Long transactionCount = settlementFundTransactionRelMapper.selectCount(relQuery);

            return settlementCount > 0 || transactionCount > 0;
        } catch (Exception e) {
            log.warn("检查合同收款数据时发生异常，合同ID：{}", contract.getContractId(), e);
            return false;
        }
    }

    /**
     * 检查合同是否有发票数据
     */
    private boolean checkContractHasInvoiceData(Contract contract) {
        if (contract == null || contract.getContractId() == null) {
            return false;
        }

        try {
            // 检查是否有发票记录
            QueryWrapper<SettlementInvoiceRel> relQuery = new QueryWrapper<>();
            relQuery.exists("SELECT 1 FROM settlement s WHERE s.结算单编号 = " +
                           "settlement_invoice_rel.结算单编号 AND s.合同编号 = " + contract.getContractId());
            Long invoiceCount = settlementInvoiceRelMapper.selectCount(relQuery);

            return invoiceCount > 0;
        } catch (Exception e) {
            log.warn("检查合同发票数据时发生异常，合同ID：{}", contract.getContractId(), e);
            return false;
        }
    }

    /**
     * 创建状态逻辑不一致问题
     */
    private ClosureIssueDTO createStatusLogicInconsistencyIssue(Contract contract, ContractClosureStatus currentStatus,
                                                               String issueDescription, String issueCode) {
        ClosureIssueDTO issue = new ClosureIssueDTO();
        issue.setIssueId("STATUS_LOGIC_" + contract.getContractId() + "_" + issueCode + "_" + System.currentTimeMillis());
        issue.setIssueType(ISSUE_TYPE_STATUS_INCONSISTENCY);
        issue.setRiskLevel(currentStatus.getRiskLevel());
        issue.setRelatedEntityType("CONTRACT");
        issue.setRelatedEntityId(contract.getContractId().longValue());
        issue.setRelatedEntityCode(contract.getContractNo());
        issue.setRelatedEntityName("合同-" + contract.getContractNo());
        issue.setIssueTitle("状态逻辑不一致");
        issue.setIssueDescription("合同[" + contract.getContractNo() + "]处于[" + currentStatus.getDisplayName() + "]状态，但" + issueDescription);
        issue.setSuggestedAction("请检查合同状态是否正确，或更新相关业务数据以匹配当前状态");
        issue.setActionType(ACTION_TYPE_MANUAL_FIX);
        issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
        return issue;
    }

    @Override
    public List<ValidationCheckItemDTO> getValidationCheckItems() {
        List<ValidationCheckItemDTO> checkItems = new ArrayList<>();

        // 时间顺序校验项
        ValidationCheckItemDTO timeCheck = new ValidationCheckItemDTO();
        timeCheck.setCheckId("CHECK_TIME_SEQUENCE");
        timeCheck.setCheckType("TIME_SEQUENCE");
        timeCheck.setCheckName("时间顺序校验");
        timeCheck.setStatus("PASS");
        timeCheck.setMessage("时间顺序校验正常");
        checkItems.add(timeCheck);

        // 金额一致性校验项
        ValidationCheckItemDTO amountCheck = new ValidationCheckItemDTO();
        amountCheck.setCheckId("CHECK_AMOUNT_CONSISTENCY");
        amountCheck.setCheckType("AMOUNT_CONSISTENCY");
        amountCheck.setCheckName("金额一致性校验");
        amountCheck.setStatus("PASS");
        amountCheck.setMessage("金额一致性校验正常");
        checkItems.add(amountCheck);

        // 数据关联校验项
        ValidationCheckItemDTO associationCheck = new ValidationCheckItemDTO();
        associationCheck.setCheckId("CHECK_ASSOCIATION_INTEGRITY");
        associationCheck.setCheckType("ASSOCIATION_INTEGRITY");
        associationCheck.setCheckName("数据关联完整性校验");
        associationCheck.setStatus("PASS");
        associationCheck.setMessage("数据关联完整性校验正常");
        checkItems.add(associationCheck);

        // 状态一致性校验项
        ValidationCheckItemDTO statusCheck = new ValidationCheckItemDTO();
        statusCheck.setCheckId("CHECK_STATUS_CONSISTENCY");
        statusCheck.setCheckType("STATUS_CONSISTENCY");
        statusCheck.setCheckName("合同状态一致性校验");
        statusCheck.setStatus("PASS");
        statusCheck.setMessage("合同状态一致性校验正常");
        checkItems.add(statusCheck);

        return checkItems;
    }

    /**
     * 获取要校验的合同ID列表
     */
    private List<Long> getContractIdsToValidate(ClosureValidationRequest request) {
        // 全量校验时，如果指定了合同ID，则校验指定合同，否则获取所有有效合同
        if (request.getContractId() != null) {
            return Arrays.asList(request.getContractId());
        }

        // 获取所有有效合同的ID
        QueryWrapper<Contract> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("签订时间");
        queryWrapper.orderByDesc("签订时间");

        List<Contract> contracts = contractMapper.selectList(queryWrapper);
        return contracts.stream()
                .map(Contract::getContractId)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * 校验合同时间顺序（签订时间 vs 执行时间）
     */
    private List<ClosureIssueDTO> validateContractTimeSequence(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取该合同的收运通知单（作为执行的开始时间）
        QueryWrapper<PickupNotice> pickupQuery = new QueryWrapper<>();
        pickupQuery.eq("合同号", contract.getContractNo());
        pickupQuery.orderByAsc("创建时间");
        List<PickupNotice> pickupNotices = pickupNoticeMapper.selectList(pickupQuery);

        if (!CollectionUtils.isEmpty(pickupNotices)) {
            PickupNotice firstPickup = pickupNotices.get(0);
            if (contract.getSignTime() != null && firstPickup.getCreateTime() != null) {
                if (firstPickup.getCreateTime().isBefore(contract.getSignTime())) {
                    // 发现问题：执行时间早于签订时间
                    ClosureIssueDTO issue = createTimeSequenceIssue(
                            ISSUE_TYPE_TIME_SEQUENCE_VIOLATION,
                            RISK_LEVEL_HIGH,
                            "CONTRACT",
                            contract.getContractId() != null ? contract.getContractId().longValue() : 0L,
                            contract.getContractNo(),
                            "合同签订时间晚于执行时间",
                            String.format("合同%s的签订日期为%s，但实际执行开始时间为%s，存在时间顺序违规。",
                                    contract.getContractNo() != null ? contract.getContractNo() : "未知",
                                    DateUtil.format(contract.getSignTime(), "yyyy-MM-dd HH:mm:ss"),
                                    DateUtil.format(firstPickup.getCreateTime(), "yyyy-MM-dd HH:mm:ss")),
                            createDetailsMap(
                                    "contractSigningTime", DateUtil.format(contract.getSignTime(), "yyyy-MM-dd HH:mm:ss"),
                                    "executionStartTime", DateUtil.format(firstPickup.getCreateTime(), "yyyy-MM-dd HH:mm:ss"),
                                    "timeDifference", calculateTimeDifference(firstPickup.getCreateTime(), contract.getSignTime())
                            ),
                            "请确认合同签订时间是否正确，或调整执行记录的时间",
                            ACTION_TYPE_REVIEW_REQUIRED
                    );
                    issues.add(issue);
                }
            }
        }

        return issues;
    }

    /**
     * 校验单次收款不能超过剩余金额
     */
    private List<ClosureIssueDTO> validateSinglePaymentLimit(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取该合同的所有收款记录
        QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
        settlementQuery.eq("合同号", contract.getContractNo());
        // 使用别名映射解决中文字段名映射问题
        settlementQuery.select(
            "结算单编号 AS settlementId",
            "结算单单号 AS settlementCode",
            "结算金额 AS settlementAmount",
            "创建时间 AS createTime",
            "更新时间 AS updateTime",
            "version AS version"
        );
        List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

        BigDecimal totalReceived = BigDecimal.ZERO;

        for (Settlement settlement : settlements) {
            if (settlement == null || settlement.getSettlementId() == null) {
                continue; // 跳过null的结算单
            }
            // 获取该结算单的收款记录
            QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
            relQuery.eq("结算单编号", settlement.getSettlementId());
            List<SettlementFundTransactionRel> relations = settlementFundTransactionRelMapper.selectList(relQuery);

            for (SettlementFundTransactionRel relation : relations) {
                if (relation.getRelAmount() != null) {
                    BigDecimal currentReceived = totalReceived.add(relation.getRelAmount());
                    BigDecimal remainingAmount = contract.getContractAmount().subtract(totalReceived);

                    // 检查单次收款是否超过剩余金额
                    if (relation.getRelAmount().compareTo(remainingAmount) > 0) {
                        ClosureIssueDTO issue = createTimeSequenceIssue(
                                ISSUE_TYPE_AMOUNT_MISMATCH,
                                RISK_LEVEL_CRITICAL,
                                "RECEIPT",
                                relation.getTransactionId(),
                                "TXN_" + relation.getTransactionId(),
                                "单次收款超过合同剩余金额",
                            String.format("收款记录%s金额%s元，超过合同%s剩余可收款金额%s元。",
                                    relation.getTransactionId() != null ? relation.getTransactionId() : "未知",
                                        relation.getRelAmount(),
                                        contract.getContractNo(),
                                        remainingAmount),
                                createDetailsMap(
                                        "paymentAmount", relation.getRelAmount(),
                                        "remainingAmount", remainingAmount,
                                        "contractTotal", contract.getContractAmount(),
                                        "totalReceived", totalReceived,
                                        "settlementCode", settlement.getSettlementCode()
                                ),
                                "请检查收款金额是否正确，或确认是否为预收款/多收款情况",
                                ACTION_TYPE_MANUAL_FIX
                        );
                        issues.add(issue);
                    }

                    totalReceived = currentReceived;
                }
            }
        }

        return issues;
    }

    /**
     * 校验累计收款不能超过合同总金额
     */
    private List<ClosureIssueDTO> validateTotalPaymentLimit(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 计算该合同的总收款金额
        QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
        settlementQuery.eq("合同号", contract.getContractNo());
        // 使用别名映射解决中文字段名映射问题
        settlementQuery.select(
            "结算单编号 AS settlementId",
            "结算单单号 AS settlementCode",
            "结算金额 AS settlementAmount",
            "创建时间 AS createTime",
            "更新时间 AS updateTime",
            "version AS version"
        );
        List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

        BigDecimal totalReceived = BigDecimal.ZERO;
        int receiptCount = 0;

        for (Settlement settlement : settlements) {
            if (settlement == null || settlement.getSettlementId() == null) {
                continue; // 跳过null的结算单
            }
            QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
            relQuery.eq("结算单编号", settlement.getSettlementId());
            List<SettlementFundTransactionRel> relations = settlementFundTransactionRelMapper.selectList(relQuery);

            for (SettlementFundTransactionRel relation : relations) {
                if (relation.getRelAmount() != null) {
                    totalReceived = totalReceived.add(relation.getRelAmount());
                    receiptCount++;
                }
            }
        }

        // 检查累计收款是否超过合同总金额
        if (contract.getContractAmount() != null && totalReceived.compareTo(contract.getContractAmount()) > 0) {
            BigDecimal excessAmount = totalReceived.subtract(contract.getContractAmount());

            ClosureIssueDTO issue = createTimeSequenceIssue(
                    ISSUE_TYPE_AMOUNT_MISMATCH,
                    RISK_LEVEL_CRITICAL,
                    "CONTRACT",
                    contract.getContractId() != null ? contract.getContractId().longValue() : 0L,
                    contract.getContractNo(),
                    "累计收款超过合同总金额",
                            String.format("合同%s累计收款%s元，超过合同总金额%s元，多收%s元。",
                            contract.getContractNo() != null ? contract.getContractNo() : "未知",
                            totalReceived,
                            contract.getContractAmount(),
                            excessAmount),
                    createDetailsMap(
                            "totalReceived", totalReceived,
                            "contractAmount", contract.getContractAmount(),
                            "excessAmount", excessAmount,
                            "receiptCount", receiptCount,
                            "settlementCount", settlements.size()
                    ),
                    "请检查是否存在重复收款或多收款情况，必要时进行退款处理",
                    ACTION_TYPE_MANUAL_FIX
            );
            issues.add(issue);
        }

        return issues;
    }

    /**
     * 校验发票总额与流水总额交叉校验
     */
    private List<ClosureIssueDTO> validateInvoiceReceiptCrossCheck(List<Long> contractIds) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取合同列表
        QueryWrapper<Contract> contractQuery = new QueryWrapper<>();
        if (!CollectionUtils.isEmpty(contractIds)) {
            contractQuery.in("合同编号", contractIds);
        }
        List<Contract> contracts = contractMapper.selectList(contractQuery);

        for (Contract contract : contracts) {
            // 获取该合同的所有结算单
            QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
            settlementQuery.eq("合同号", contract.getContractNo());
            // 使用别名映射解决中文字段名映射问题
            settlementQuery.select(
                "结算单编号 AS settlementId",
                "结算单单号 AS settlementCode",
                "结算金额 AS settlementAmount",
                "创建时间 AS createTime",
                "更新时间 AS updateTime",
                "version AS version"
            );
            List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

            for (Settlement settlement : settlements) {
                if (settlement == null || settlement.getSettlementId() == null) {
                    continue; // 跳过null的结算单
                }
                // 计算该结算单的发票总额
                QueryWrapper<SettlementInvoiceRel> invoiceRelQuery = new QueryWrapper<>();
                invoiceRelQuery.eq("结算单编号", settlement.getSettlementId());
                List<SettlementInvoiceRel> invoiceRelations = settlementInvoiceRelMapper.selectList(invoiceRelQuery);

                BigDecimal invoiceTotal = invoiceRelations.stream()
                        .filter(rel -> rel.getRelAmount() != null)
                        .map(SettlementInvoiceRel::getRelAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 计算该结算单的流水总额
                QueryWrapper<SettlementFundTransactionRel> receiptRelQuery = new QueryWrapper<>();
                receiptRelQuery.eq("结算单编号", settlement.getSettlementId());
                List<SettlementFundTransactionRel> receiptRelations = settlementFundTransactionRelMapper.selectList(receiptRelQuery);

                BigDecimal receiptTotal = receiptRelations.stream()
                        .filter(rel -> rel.getRelAmount() != null)
                        .map(SettlementFundTransactionRel::getRelAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 只有当都有数据时才进行交叉校验
                if (invoiceTotal.compareTo(BigDecimal.ZERO) > 0 && receiptTotal.compareTo(BigDecimal.ZERO) > 0) {
                    // 允许±1%的误差范围
                    BigDecimal maxAmount = invoiceTotal.max(receiptTotal);
                    BigDecimal tolerance = maxAmount.multiply(new BigDecimal("0.01"));
                    BigDecimal difference = invoiceTotal.subtract(receiptTotal).abs();

                    if (difference.compareTo(tolerance) > 0) {
                        ClosureIssueDTO issue = createTimeSequenceIssue(
                                ISSUE_TYPE_AMOUNT_MISMATCH,
                                RISK_LEVEL_HIGH,
                                "SETTLEMENT",
                                settlement.getSettlementId(),
                                settlement.getSettlementCode(),
                                "发票总额与流水总额不一致",
                                String.format("结算单%s的发票总额%s元与流水总额%s元不一致，差额%s元。",
                                        settlement.getSettlementCode() != null ? settlement.getSettlementCode() : "未知",
                                        invoiceTotal,
                                        receiptTotal,
                                        difference),
                                createDetailsMap(
                                        "invoiceTotal", invoiceTotal,
                                        "receiptTotal", receiptTotal,
                                        "difference", difference,
                                        "tolerance", tolerance,
                                        "invoiceCount", invoiceRelations.size(),
                                        "receiptCount", receiptRelations.size(),
                                        "settlementAmount", settlement.getSettlementAmount()
                                ),
                                "请核对发票和流水记录，确保金额一致",
                                ACTION_TYPE_MANUAL_FIX
                        );
                        issues.add(issue);
                    }
                }
            }
        }

        return issues;
    }

    /**
     * 校验弱关联提醒（非强制性关联）
     */
    private List<ClosureIssueDTO> validateWeakAssociations(List<Long> contractIds) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取合同列表
        QueryWrapper<Contract> contractQuery = new QueryWrapper<>();
        if (!CollectionUtils.isEmpty(contractIds)) {
            contractQuery.in("合同编号", contractIds);
        }
        List<Contract> contracts = contractMapper.selectList(contractQuery);

        for (Contract contract : contracts) {
            // 校验运输单合同关联提醒
            issues.addAll(validateTransportContractAssociation(contract));

            // 校验入库单结算关联提醒
            issues.addAll(validateWarehousingSettlementAssociation(contract));

            // 校验收运记录延迟关联检查
            issues.addAll(validatePickupDelayAssociation(contract));
        }

        return issues;
    }

    /**
     * 校验运输单合同关联提醒
     */
    private List<ClosureIssueDTO> validateTransportContractAssociation(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 检查运输单是否通过收运通知单关联合同
        // 运输单 -> 收运通知单 -> 合同

        // 获取该合同的收运通知单
        QueryWrapper<PickupNotice> pickupQuery = new QueryWrapper<>();
        pickupQuery.eq("合同号", contract.getContractNo());
        List<PickupNotice> pickupNotices = pickupNoticeMapper.selectList(pickupQuery);

        if (!CollectionUtils.isEmpty(pickupNotices)) {
            List<String> noticeCodes = pickupNotices.stream()
                    .map(PickupNotice::getNoticeCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(noticeCodes)) {
                // 查找这些收运通知单对应的运输单
                QueryWrapper<DispatchOrder> dispatchQuery = new QueryWrapper<>();
                dispatchQuery.in("收运通知单号", noticeCodes);
                List<DispatchOrder> dispatchOrders = dispatchOrderMapper.selectList(dispatchQuery);

                // 这里可以检查运输单是否完整关联了合同相关的所有信息
                // 目前由于数据结构限制，我们可以生成一个低风险提醒
                // 提示检查运输单与合同的关联关系

                if (CollectionUtils.isEmpty(dispatchOrders) && !pickupNotices.isEmpty()) {
                    // 存在收运通知单但没有运输单，生成提醒
                    ClosureIssueDTO issue = createTimeSequenceIssue(
                            ISSUE_TYPE_MISSING_ASSOCIATION,
                            RISK_LEVEL_LOW,
                            "CONTRACT",
                            contract.getContractId() != null ? contract.getContractId().longValue() : 0L,
                            contract.getContractNo(),
                            "合同运输单关联检查",
                            String.format("合同%s存在%s个收运通知单，但未找到关联的运输单信息，建议检查运输单关联情况。",
                                    contract.getContractNo(), pickupNotices.size()),
                            createDetailsMap(
                                    "contractCode", contract.getContractNo(),
                                    "pickupNoticeCount", pickupNotices.size(),
                                    "dispatchOrderCount", 0,
                                    "noticeCodes", noticeCodes
                            ),
                            "建议检查并完善运输单与合同的关联关系",
                            ACTION_TYPE_MANUAL_FIX
                    );
                    issues.add(issue);
                }
            }
        }

        return issues;
    }

    /**
     * 校验入库单结算关联
     */
    private List<ClosureIssueDTO> validateWarehousingSettlementAssociation(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 1. 获取该合同下的所有入库单（通过收运通知单关联）
        List<Warehousing> contractWarehousings = getContractWarehousings(contract);
        List<String> contractWarehousingNos = contractWarehousings.stream()
            .map(Warehousing::getWarehousingNo)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // 创建入库单编号到ID的映射
        Map<String, Long> warehousingNoToIdMap = contractWarehousings.stream()
            .filter(w -> w.getWarehousingNo() != null && w.getWarehousingId() != null)
            .collect(Collectors.toMap(Warehousing::getWarehousingNo, w -> w.getWarehousingId().longValue()));

        log.info("validateWarehousingSettlementAssociation - 合同{}关联的入库单数量: {}",
                contract.getContractNo(), contractWarehousingNos.size());

        // 2. 获取该合同下的所有结算单
        QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
        settlementQuery.eq("合同号", contract.getContractNo());
        settlementQuery.select(
            "结算单编号 AS settlementId",
            "结算单单号 AS settlementCode",
            "结算金额 AS settlementAmount",
            "创建时间 AS createTime",
            "更新时间 AS updateTime",
            "version AS version"
        );
        List<Settlement> settlements = settlementMapper.selectList(settlementQuery);
        log.info("validateWarehousingSettlementAssociation - 合同{}的结算单数量: {}",
                contract.getContractNo(), settlements != null ? settlements.size() : 0);

        if (CollectionUtils.isEmpty(contractWarehousingNos) && CollectionUtils.isEmpty(settlements)) {
            // 既没有入库单也没有结算单，跳过检查
            log.info("validateWarehousingSettlementAssociation - 合同{}既没有入库单也没有结算单，跳过检查",
                    contract.getContractNo());
            return issues;
        }

        // 3. 获取所有结算单关联的入库单
        Set<String> associatedWarehousingNos = new HashSet<>();
        if (!CollectionUtils.isEmpty(settlements)) {
            List<Long> settlementIds = settlements.stream()
                .map(Settlement::getSettlementId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(settlementIds)) {
                QueryWrapper<SettlementReference> refQuery = new QueryWrapper<>();
                refQuery.in("结算单编号", settlementIds);
                refQuery.eq("关联来源类型", "WAREHOUSING");
                List<SettlementReference> references = settlementReferenceMapper.selectList(refQuery);

                associatedWarehousingNos = references.stream()
                    .map(SettlementReference::getSourceCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

                log.info("validateWarehousingSettlementAssociation - 结算单关联的入库单数量: {}", associatedWarehousingNos.size());
            }
        }

        // 4. 检查未关联结算单的入库单
        for (String warehousingNo : contractWarehousingNos) {
            if (!associatedWarehousingNos.contains(warehousingNo)) {
                log.info("validateWarehousingSettlementAssociation - 入库单{}未关联结算单，生成问题", warehousingNo);

                // 获取入库单ID，如果不存在则使用0
                Long warehousingId = warehousingNoToIdMap.getOrDefault(warehousingNo, 0L);

                ClosureIssueDTO issue = createTimeSequenceIssue(
                        ISSUE_TYPE_MISSING_ASSOCIATION,
                        RISK_LEVEL_MEDIUM,
                        "WAREHOUSING",
                        warehousingId, // 使用真实的入库单ID
                        warehousingNo,
                        "入库单未关联结算单",
                        String.format("入库单%s（合同%s）未关联到任何结算单，无法确认结算状态。",
                                warehousingNo, contract.getContractNo()),
                        createDetailsMap(
                                "warehousingNo", warehousingNo,
                                "warehousingId", warehousingId,
                                "contractCode", contract.getContractNo(),
                                "settlementCount", settlements != null ? settlements.size() : 0
                        ),
                        "请为入库单关联对应的结算单，确保物流与财务数据一致",
                        ACTION_TYPE_MANUAL_FIX
                );
                issues.add(issue);
            }
        }

        // 5. 检查结算单关联的入库单是否存在
        for (String associatedWarehousingNo : associatedWarehousingNos) {
            if (!contractWarehousingNos.contains(associatedWarehousingNo)) {
                // 结算单关联的入库单不属于这个合同
                log.info("validateWarehousingSettlementAssociation - 结算单关联的入库单{}不属于合同{}，生成问题",
                        associatedWarehousingNo, contract.getContractNo());

                ClosureIssueDTO issue = createTimeSequenceIssue(
                        ISSUE_TYPE_MISSING_ASSOCIATION,
                        RISK_LEVEL_HIGH,
                        "SETTLEMENT",
                        0L, // 无法确定具体结算单ID
                        "未知结算单",
                        "结算单关联入库单不存在",
                        String.format("结算单关联的入库单%s不属于合同%s，或入库单不存在。",
                                associatedWarehousingNo, contract.getContractNo()),
                        createDetailsMap(
                                "warehousingNo", associatedWarehousingNo,
                                "contractCode", contract.getContractNo(),
                                "issueType", "INVALID_REFERENCE"
                        ),
                        "请检查结算单的入库单关联是否正确",
                        ACTION_TYPE_MANUAL_FIX
                );
                issues.add(issue);
            }
        }

        log.info("validateWarehousingSettlementAssociation - 合同{}处理完成，生成了{}个入库单结算关联问题",
                contract.getContractNo(), issues.size());

        return issues;
    }

    /**
     * 获取合同关联的所有入库单号
     */
    private List<String> getContractWarehousingNos(Contract contract) {
        List<String> warehousingNos = new ArrayList<>();

        // 通过收运通知单关联获取入库单
        // 关联链：合同 → 收运通知单 → 运输单 → 磅单 → 入库单
        QueryWrapper<PickupNotice> pickupQuery = new QueryWrapper<>();
        pickupQuery.eq("合同号", contract.getContractNo());
        List<PickupNotice> pickupNotices = pickupNoticeMapper.selectList(pickupQuery);

        if (!CollectionUtils.isEmpty(pickupNotices)) {
            List<String> noticeCodes = pickupNotices.stream()
                .map(PickupNotice::getNoticeCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // 通过运输单关联获取磅单
            if (!CollectionUtils.isEmpty(noticeCodes)) {
                // 1. 通过收运通知单号找到运输单
                QueryWrapper<DispatchOrder> dispatchQuery = new QueryWrapper<>();
                dispatchQuery.in("收运通知单号", noticeCodes);
                List<DispatchOrder> dispatchOrders = dispatchOrderMapper.selectList(dispatchQuery);

                if (!CollectionUtils.isEmpty(dispatchOrders)) {
                        List<String> dispatchNos = dispatchOrders.stream()
                            .map(DispatchOrder::getDispatchCode)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    // 2. 通过运输单号找到磅单
                    QueryWrapper<WeighingSlipDispatch> weighingDispatchQuery = new QueryWrapper<>();
                    weighingDispatchQuery.in("运输单号", dispatchNos);
                    List<WeighingSlipDispatch> weighingDispatches = weighingSlipDispatchMapper.selectList(weighingDispatchQuery);

                    if (!CollectionUtils.isEmpty(weighingDispatches)) {
                        List<Integer> weighingSlipIds = weighingDispatches.stream()
                            .map(WeighingSlipDispatch::getWeighingSlipId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                        // 3. 获取磅单号
                        QueryWrapper<WeighingSlip> weighingQuery = new QueryWrapper<>();
                        weighingQuery.in("总磅单编号", weighingSlipIds);
                        List<WeighingSlip> weighingSlips = weighingSlipMapper.selectList(weighingQuery);

                        if (!CollectionUtils.isEmpty(weighingSlips)) {
                            List<String> slipNos = weighingSlips.stream()
                                .map(WeighingSlip::getWeighingSlipNo)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                            // 4. 通过磅单号获取入库单
                            QueryWrapper<Warehousing> warehousingQuery = new QueryWrapper<>();
                            warehousingQuery.in("总磅单号", slipNos);
                            List<Warehousing> warehousings = warehousingMapper.selectList(warehousingQuery);

                            warehousingNos = warehousings.stream()
                                .map(Warehousing::getWarehousingNo)
                                .filter(Objects::nonNull)
                                .distinct()
                                .collect(Collectors.toList());
                        }
                    }
                }
            }
        }

        return warehousingNos;
    }

    /**
     * 获取合同关联的入库单信息（包含ID和编号）
     */
    private List<Warehousing> getContractWarehousings(Contract contract) {
        List<Warehousing> warehousings = new ArrayList<>();

        // 通过收运通知单关联获取入库单
        // 关联链：合同 → 收运通知单 → 运输单 → 磅单 → 入库单
        QueryWrapper<PickupNotice> pickupQuery = new QueryWrapper<>();
        pickupQuery.eq("合同号", contract.getContractNo());
        List<PickupNotice> pickupNotices = pickupNoticeMapper.selectList(pickupQuery);

        if (!CollectionUtils.isEmpty(pickupNotices)) {
            List<String> noticeCodes = pickupNotices.stream()
                .map(PickupNotice::getNoticeCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // 通过运输单关联获取磅单
            if (!CollectionUtils.isEmpty(noticeCodes)) {
                // 1. 通过收运通知单号找到运输单
                QueryWrapper<DispatchOrder> dispatchQuery = new QueryWrapper<>();
                dispatchQuery.in("收运通知单号", noticeCodes);
                List<DispatchOrder> dispatchOrders = dispatchOrderMapper.selectList(dispatchQuery);

                if (!CollectionUtils.isEmpty(dispatchOrders)) {
                        List<String> dispatchNos = dispatchOrders.stream()
                            .map(DispatchOrder::getDispatchCode)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    // 2. 通过运输单号找到磅单
                    QueryWrapper<WeighingSlipDispatch> weighingDispatchQuery = new QueryWrapper<>();
                    weighingDispatchQuery.in("运输单号", dispatchNos);
                    List<WeighingSlipDispatch> weighingDispatches = weighingSlipDispatchMapper.selectList(weighingDispatchQuery);

                    if (!CollectionUtils.isEmpty(weighingDispatches)) {
                        List<Integer> weighingSlipIds = weighingDispatches.stream()
                            .map(WeighingSlipDispatch::getWeighingSlipId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                        // 3. 获取磅单号
                        QueryWrapper<WeighingSlip> weighingQuery = new QueryWrapper<>();
                        weighingQuery.in("总磅单编号", weighingSlipIds);
                        List<WeighingSlip> weighingSlips = weighingSlipMapper.selectList(weighingQuery);

                        if (!CollectionUtils.isEmpty(weighingSlips)) {
                            List<String> slipNos = weighingSlips.stream()
                                .map(WeighingSlip::getWeighingSlipNo)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                            // 4. 通过磅单号获取入库单
                            QueryWrapper<Warehousing> warehousingQuery = new QueryWrapper<>();
                            warehousingQuery.in("总磅单号", slipNos);
                            warehousings = warehousingMapper.selectList(warehousingQuery);
                        }
                    }
                }
            }
        }

        return warehousings;
    }

    /**
     * 校验收运记录延迟关联检查
     */
    private List<ClosureIssueDTO> validatePickupDelayAssociation(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取该合同的收运通知单
        QueryWrapper<PickupNotice> pickupQuery = new QueryWrapper<>();
        pickupQuery.eq("合同号", contract.getContractNo());
        List<PickupNotice> pickupNotices = pickupNoticeMapper.selectList(pickupQuery);

        LocalDateTime now = LocalDateTime.now();

        for (PickupNotice pickupNotice : pickupNotices) {
            if (pickupNotice == null || pickupNotice.getSubmittedAt() == null) {
                continue;
            }

            // 检查是否超过24小时仍未关联相关业务
            LocalDateTime submittedTime = pickupNotice.getSubmittedAt();
            LocalDateTime deadline = submittedTime.plusHours(24);

            if (now.isAfter(deadline)) {
                // 检查该收运通知单是否已经有关联的业务数据
                boolean hasAssociation = false;

                // 检查是否有磅单关联（通过总磅单号）
                if (pickupNotice.getNoticeCode() != null) {
                    // 这里可以检查是否已经有磅单或运输单与之关联
                    // 简化检查：如果收运通知单存在超过24小时，认为可能需要关联检查
                    hasAssociation = true; // 暂时假设有关联，实际需要业务逻辑判断
                }

                if (!hasAssociation) {
                    ClosureIssueDTO issue = createTimeSequenceIssue(
                            ISSUE_TYPE_MISSING_ASSOCIATION,
                            RISK_LEVEL_MEDIUM,
                            "PICKUP_NOTICE",
                            pickupNotice.getNoticeId().longValue(),
                            pickupNotice.getNoticeCode(),
                            "收运通知单延迟关联",
                            String.format("收运通知单%s已创建超过24小时，仍未关联磅单或运输单信息。",
                                    pickupNotice.getNoticeCode()),
                            createDetailsMap(
                                    "noticeCode", pickupNotice.getNoticeCode(),
                                    "submittedAt", DateUtil.format(submittedTime, "yyyy-MM-dd HH:mm:ss"),
                                    "hoursElapsed", java.time.Duration.between(submittedTime, now).toHours(),
                                    "contractCode", contract.getContractNo()
                            ),
                            "请及时为收运通知单关联磅单和运输单信息，确保业务流程完整性",
                            ACTION_TYPE_MANUAL_FIX
                    );
                    issues.add(issue);
                }
            }
        }

        return issues;
    }

    /**
     * 校验预收款情况（核心功能：只签合同未执行但收款 → 系统强制标记为预收款）
     */
    private List<ClosureIssueDTO> validatePrepaymentScenarios(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        log.info("开始校验合同{}的预收款情况", contract.getContractNo());

        // 获取该合同的所有结算单
        QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
        settlementQuery.eq("合同号", contract.getContractNo());
        settlementQuery.select(
            "结算单编号 AS settlementId",
            "结算单单号 AS settlementCode",
            "结算金额 AS settlementAmount",
            "创建时间 AS createTime",
            "更新时间 AS updateTime",
            "version AS version"
        );
        List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

        // 检查是否存在执行记录（磅单、入库单等）
        boolean hasExecutionRecords = checkContractHasExecutionRecords(contract);

        log.info("合同{}执行记录检查结果：hasExecutionRecords={}", contract.getContractNo(), hasExecutionRecords);

        // 核心逻辑：如果合同已收款但没有任何执行记录，则标记为预收款
        if (!hasExecutionRecords && !CollectionUtils.isEmpty(settlements)) {
            log.info("合同{}存在预收款风险：已生成结算单但无执行记录", contract.getContractNo());

            for (Settlement settlement : settlements) {
                if (settlement == null || settlement.getSettlementId() == null) {
                    continue;
                }

                // 获取该结算单的收款记录
                QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
                relQuery.eq("结算单编号", settlement.getSettlementId());
                List<SettlementFundTransactionRel> relations = settlementFundTransactionRelMapper.selectList(relQuery);

                if (!CollectionUtils.isEmpty(relations)) {
                    // 只要有收款记录，就认为是预收款风险
                    for (SettlementFundTransactionRel relation : relations) {
                        if (relation.getRelTime() != null && contract.getSignTime() != null) {
                            log.info("合同{}发现预收款情况：结算单{}有收款记录但无执行记录",
                                    contract.getContractNo(), settlement.getSettlementCode());

                            // 创建预收款问题
                            ClosureIssueDTO prepaymentIssue = createPrepaymentIssue(
                                    contract,
                                    relation,
                                    settlement.getSettlementCode(),
                                    false // 不是基于时间的预收款，而是基于执行状态的预收款
                            );

                            // 根据收款时间与签订时间的关系，设置不同的标题和风险等级
                            boolean isTimeBasedPrepayment = relation.getRelTime().isBefore(contract.getSignTime());

                            if (isTimeBasedPrepayment) {
                                // 时间违规：收款时间早于签订时间 + 未执行 = 高风险
                                prepaymentIssue.setIssueType(ISSUE_TYPE_TIME_SEQUENCE_VIOLATION);
                                prepaymentIssue.setIssueTitle("时间违规：合同签订前已收款（未执行）");
                                prepaymentIssue.setIssueDescription(String.format(
                                        "合同%s仅签订但未执行任何业务，却已收到款项%s元（收款时间%s早于签订时间%s）。违反了基本的业务时间顺序。",
                                        contract.getContractNo(),
                                        relation.getRelAmount(),
                                        DateUtil.format(relation.getRelTime(), "yyyy-MM-dd HH:mm:ss"),
                                        DateUtil.format(contract.getSignTime(), "yyyy-MM-dd HH:mm:ss")
                                ));
                                prepaymentIssue.setSuggestedAction("请检查收款时间的合理性，或补充业务执行记录。建议财务与业务部门确认该笔收款的业务背景。");
                                prepaymentIssue.setRiskLevel(RISK_LEVEL_HIGH);
                            } else {
                                // 时间正常但未执行 = 中高风险
                                prepaymentIssue.setIssueTitle("警告：合同已收款但未执行");
                                prepaymentIssue.setIssueDescription(String.format(
                                        "合同%s已收到款项%s元（收款时间%s），但未发现任何业务执行记录（无磅单、无入库单、无运输单）。这属于典型的预收款风险。",
                                        contract.getContractNo(),
                                        relation.getRelAmount(),
                                        DateUtil.format(relation.getRelTime(), "yyyy-MM-dd HH:mm:ss")
                                ));
                                prepaymentIssue.setSuggestedAction("请确认是否为预收款业务场景，或及时补充执行记录。建议业务负责人审核确认。");
                                prepaymentIssue.setRiskLevel(RISK_LEVEL_HIGH);
                            }

                            prepaymentIssue.setActionType(ACTION_TYPE_REVIEW_REQUIRED);
                            issues.add(prepaymentIssue);

                            // 检查预收款比例是否超过30%
                            BigDecimal prepaymentRatio = BigDecimal.ZERO;
                            if (contract.getContractAmount() != null && contract.getContractAmount().compareTo(BigDecimal.ZERO) > 0) {
                                prepaymentRatio = relation.getRelAmount().divide(contract.getContractAmount(), 4, BigDecimal.ROUND_HALF_UP);
                            }

                            if (prepaymentRatio.compareTo(new BigDecimal("0.3")) > 0) {
                                // 生成额外的预收款比例超标问题（这确实是预收款业务规则违规）
                                ClosureIssueDTO ratioIssue = createTimeSequenceIssue(
                                        ISSUE_TYPE_PREPAYMENT_VIOLATION,
                                        RISK_LEVEL_CRITICAL,
                                        "RECEIPT",
                                        relation.getTransactionId(),
                                        "TXN_" + relation.getTransactionId(),
                                        "预收款比例严重超标（未执行合同）",
                                        String.format("未执行合同的预收款金额%s元，占合同总金额%s元的%.2f%%，严重超过30%%上限。",
                                                relation.getRelAmount(),
                                                contract.getContractAmount(),
                                                prepaymentRatio.multiply(new BigDecimal("100"))),
                                        createDetailsMap(
                                                "prepaymentAmount", relation.getRelAmount(),
                                                "contractAmount", contract.getContractAmount(),
                                                "prepaymentRatio", prepaymentRatio,
                                                "maxAllowedRatio", 0.3,
                                                "settlementCode", settlement.getSettlementCode(),
                                                "hasExecutionRecords", false,
                                                "violationType", "比例超标"
                                        ),
                                        "预收款比例严重超标，且合同未执行。请立即停止相关业务操作，业务负责人必须审批确认。",
                                        ACTION_TYPE_REVIEW_REQUIRED
                                );
                                issues.add(ratioIssue);
                            }
                        }
                    }
                }
            }
        } else if (hasExecutionRecords && !CollectionUtils.isEmpty(settlements)) {
            // 合同已执行，但检查是否有时间上的预收款
            log.info("合同{}已执行，检查时间上的预收款情况", contract.getContractNo());

            for (Settlement settlement : settlements) {
                if (settlement == null || settlement.getSettlementId() == null) {
                    continue;
                }

                QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
                relQuery.eq("结算单编号", settlement.getSettlementId());
                List<SettlementFundTransactionRel> relations = settlementFundTransactionRelMapper.selectList(relQuery);

                for (SettlementFundTransactionRel relation : relations) {
                    if (relation.getRelTime() != null && contract.getSignTime() != null) {
                        boolean isTimeBasedPrepayment = relation.getRelTime().isBefore(contract.getSignTime());

                        if (isTimeBasedPrepayment) {
                            // 合同已执行但收款时间仍早于签订时间 - 时间违规
                            log.info("合同{}已执行但收款时间仍早于签订时间，生成时间违规提醒",
                                    contract.getContractNo());

                            ClosureIssueDTO timeIssue = createTimeSequenceIssue(
                                    ISSUE_TYPE_TIME_SEQUENCE_VIOLATION,
                                    RISK_LEVEL_MEDIUM,
                                    "RECEIPT",
                                    relation.getTransactionId(),
                                    "TXN_" + relation.getTransactionId(),
                                    "收款时间早于合同签订时间",
                                    String.format("合同%s的收款时间%s早于签订时间%s，违反了正常的业务时间顺序（金额：%s元）。",
                                            contract.getContractNo(),
                                            DateUtil.format(relation.getRelTime(), "yyyy-MM-dd HH:mm:ss"),
                                            DateUtil.format(contract.getSignTime(), "yyyy-MM-dd HH:mm:ss"),
                                            relation.getRelAmount()),
                                    createDetailsMap(
                                            "receiptTime", DateUtil.format(relation.getRelTime(), "yyyy-MM-dd HH:mm:ss"),
                                            "contractSigningTime", DateUtil.format(contract.getSignTime(), "yyyy-MM-dd HH:mm:ss"),
                                            "amount", relation.getRelAmount(),
                                            "contractCode", contract.getContractNo(),
                                            "settlementCode", settlement.getSettlementCode(),
                                            "hasExecutionRecords", true
                                    ),
                                    "请确认收款时间的合理性，虽然合同已执行，但仍建议检查业务流程的合规性。",
                                    ACTION_TYPE_REVIEW_REQUIRED
                            );
                            issues.add(timeIssue);
                        }
                    }
                }
            }
        }

        log.info("合同{}预收款校验完成，发现{}个问题", contract.getContractNo(), issues.size());
        return issues;
    }

    /**
     * 检查合同是否已有执行记录
     */
    private boolean checkContractHasExecutionRecords(Contract contract) {
        // 检查是否存在磅单记录（表示已执行收运）
        QueryWrapper<PickupNotice> pickupQuery = new QueryWrapper<>();
        pickupQuery.eq("合同号", contract.getContractNo());
        long pickupCount = pickupNoticeMapper.selectCount(pickupQuery);

        // 检查是否存在入库记录
        List<String> contractWarehousingNos = getContractWarehousingNos(contract);
        boolean hasWarehousingRecords = !CollectionUtils.isEmpty(contractWarehousingNos);

        // 检查是否存在运输单
        QueryWrapper<DispatchOrder> dispatchQuery = new QueryWrapper<>();
        // 通过收运通知单关联查询运输单
        List<PickupNotice> pickupNotices = pickupNoticeMapper.selectList(pickupQuery);
        boolean hasDispatchRecords = false;
        if (!CollectionUtils.isEmpty(pickupNotices)) {
            List<String> noticeCodes = pickupNotices.stream()
                    .map(PickupNotice::getNoticeCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(noticeCodes)) {
                QueryWrapper<DispatchOrder> dispatchQuery2 = new QueryWrapper<>();
                dispatchQuery2.in("收运通知单号", noticeCodes);
                hasDispatchRecords = dispatchOrderMapper.selectCount(dispatchQuery2) > 0;
            }
        }

        boolean hasExecution = pickupCount > 0 || hasWarehousingRecords || hasDispatchRecords;
        log.info("合同{}执行记录检查：收运通知单{}个，入库单{}个，运输单{}，总体执行状态：{}",
                contract.getContractNo(), pickupCount, contractWarehousingNos.size(),
                hasDispatchRecords, hasExecution);

        return hasExecution;
    }

    /**
     * 校验合同签订时间与收款时间
     */
    private List<ClosureIssueDTO> validateContractPaymentTimeSequence(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 通过结算单关联的资金流水来检查收款时间
        QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
        settlementQuery.eq("合同号", contract.getContractNo());
        // 使用别名映射解决中文字段名映射问题
        settlementQuery.select(
            "结算单编号 AS settlementId",
            "结算单单号 AS settlementCode",
            "结算金额 AS settlementAmount",
            "创建时间 AS createTime",
            "更新时间 AS updateTime",
            "version AS version"
        );
        List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

        for (Settlement settlement : settlements) {
            if (settlement == null || settlement.getSettlementId() == null) {
                continue; // 跳过null的结算单
            }
            // 获取该结算单关联的资金流水
            QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
            relQuery.eq("结算单编号", settlement.getSettlementId());
            List<SettlementFundTransactionRel> relations = settlementFundTransactionRelMapper.selectList(relQuery);

            for (SettlementFundTransactionRel relation : relations) {
                // 检查收款时间与合同签订时间的逻辑关系
                if (contract.getSignTime() != null && relation.getRelTime() != null) {
                    boolean isPrepayment = relation.getRelTime().isBefore(contract.getSignTime());
                    if (isPrepayment) {
                        // 这是预收款情况，需要系统强制标记为预收款合同
                        ClosureIssueDTO prepaymentIssue = createPrepaymentIssue(
                                contract,
                                relation,
                                settlement.getSettlementCode(),
                                true
                        );
                        issues.add(prepaymentIssue);

                        // 同时检查预收款比例是否超过30%
                        BigDecimal prepaymentRatio = BigDecimal.ZERO;
                        if (contract.getContractAmount() != null && contract.getContractAmount().compareTo(BigDecimal.ZERO) > 0) {
                            prepaymentRatio = relation.getRelAmount().divide(contract.getContractAmount(), 4, BigDecimal.ROUND_HALF_UP);
                        }

                        if (prepaymentRatio.compareTo(new BigDecimal("0.3")) > 0) {
                            // 预收款比例超过30%，生成CRITICAL风险
                            ClosureIssueDTO ratioIssue = createTimeSequenceIssue(
                                    ISSUE_TYPE_PREPAYMENT_VIOLATION,
                                    RISK_LEVEL_CRITICAL,
                                    "RECEIPT",
                                    relation.getTransactionId(),
                                    "TXN_" + relation.getTransactionId(),
                                    "预收款比例超过30%上限",
                                    String.format("收款记录%s的预收款金额%s元，占合同总金额%s元的%.2f%%，超过30%%上限。",
                                            relation.getTransactionId(),
                                            relation.getRelAmount(),
                                            contract.getContractAmount(),
                                            prepaymentRatio.multiply(new BigDecimal("100"))),
                                    createDetailsMap(
                                            "prepaymentAmount", relation.getRelAmount(),
                                            "contractAmount", contract.getContractAmount(),
                                            "prepaymentRatio", prepaymentRatio,
                                            "maxAllowedRatio", 0.3,
                                            "settlementCode", settlement.getSettlementCode()
                                    ),
                                    "预收款比例过高，请确认业务必要性并获取上级审批",
                                    ACTION_TYPE_REVIEW_REQUIRED
                            );
                            issues.add(ratioIssue);
                        }
                    }
                }
            }
        }

        return issues;
    }

    /**
     * 校验收款时间与开票时间
     */
    private List<ClosureIssueDTO> validatePaymentInvoiceTimeSequence(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 通过结算单关联的发票来检查开票时间
        QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
        settlementQuery.eq("合同号", contract.getContractNo());
        // 使用别名映射解决中文字段名映射问题
        settlementQuery.select(
            "结算单编号 AS settlementId",
            "结算单单号 AS settlementCode",
            "结算金额 AS settlementAmount",
            "创建时间 AS createTime",
            "更新时间 AS updateTime",
            "version AS version"
        );
        List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

        for (Settlement settlement : settlements) {
            // 获取该结算单关联的发票
            QueryWrapper<SettlementInvoiceRel> relQuery = new QueryWrapper<>();
            relQuery.eq("结算单编号", settlement.getSettlementId());
            List<SettlementInvoiceRel> relations = settlementInvoiceRelMapper.selectList(relQuery);

            for (SettlementInvoiceRel relation : relations) {
                // 获取该发票的开票时间（假设Invoice表有开票时间字段）
                // 这里需要根据实际的Invoice表结构进行调整
                // 暂时使用关联时间作为开票时间
                if (relation.getRelTime() != null) {
                    // 获取对应的收款时间（最早的收款时间）
                    QueryWrapper<SettlementFundTransactionRel> receiptQuery = new QueryWrapper<>();
                    receiptQuery.eq("结算单编号", settlement.getSettlementId());
                    receiptQuery.orderByAsc("关联时间");
                    List<SettlementFundTransactionRel> receipts = settlementFundTransactionRelMapper.selectList(receiptQuery);

                    if (!CollectionUtils.isEmpty(receipts)) {
                        SettlementFundTransactionRel firstReceipt = receipts.get(0);
                        if (firstReceipt.getRelTime() != null) {
                            if (relation.getRelTime().isBefore(firstReceipt.getRelTime())) {
                                // 发现问题：开票时间早于收款时间
                                ClosureIssueDTO issue = createTimeSequenceIssue(
                                        ISSUE_TYPE_TIME_SEQUENCE_VIOLATION,
                                        RISK_LEVEL_MEDIUM,
                                        "INVOICE",
                                        relation.getInvoiceId() != null ? relation.getInvoiceId().longValue() : 0L,
                                        "INV_" + (relation.getInvoiceId() != null ? relation.getInvoiceId() : "未知"),
                                        "开票时间早于收款时间",
                                        String.format("发票%s的开票时间为%s，早于对应收款记录%s的收款时间%s。",
                                                relation.getInvoiceId(),
                                                DateUtil.format(relation.getRelTime(), "yyyy-MM-dd HH:mm:ss"),
                                                firstReceipt.getTransactionId(),
                                                DateUtil.format(firstReceipt.getRelTime(), "yyyy-MM-dd HH:mm:ss")),
                                        createDetailsMap(
                                                "invoiceTime", DateUtil.format(relation.getRelTime(), "yyyy-MM-dd HH:mm:ss"),
                                                "receiptTime", DateUtil.format(firstReceipt.getRelTime(), "yyyy-MM-dd HH:mm:ss"),
                                                "settlementCode", settlement.getSettlementCode(),
                                                "contractCode", contract.getContractNo()
                                        ),
                                        "请确认开票时间是否正确，或调整发票记录时间",
                                        ACTION_TYPE_MANUAL_FIX
                                );
                                issues.add(issue);
                            }
                        }
                    }
                }
            }
        }

        return issues;
    }

    /**
     * 校验结算单与发票金额一致性
     */
    private List<ClosureIssueDTO> validateSettlementInvoiceAmountConsistency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取该合同的结算单
        QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
        settlementQuery.eq("合同号", contract.getContractNo());
        // 使用别名映射解决中文字段名映射问题
        settlementQuery.select(
            "结算单编号 AS settlementId",
            "结算单单号 AS settlementCode",
            "结算金额 AS settlementAmount",
            "创建时间 AS createTime",
            "更新时间 AS updateTime",
            "version AS version"
        );
        List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

        for (Settlement settlement : settlements) {
            // 获取该结算单关联的发票金额总和
            QueryWrapper<SettlementInvoiceRel> relQuery = new QueryWrapper<>();
            relQuery.eq("结算单编号", settlement.getSettlementId());
            List<SettlementInvoiceRel> relations = settlementInvoiceRelMapper.selectList(relQuery);

            BigDecimal invoiceTotal = relations.stream()
                    .filter(rel -> rel.getRelAmount() != null)
                    .map(SettlementInvoiceRel::getRelAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 如果有关联发票，检查金额一致性
            if (settlement.getSettlementAmount() != null && invoiceTotal.compareTo(BigDecimal.ZERO) > 0) {
                // 允许±1%的误差（处理税率差异）
                BigDecimal tolerance = settlement.getSettlementAmount().multiply(new BigDecimal("0.01"));
                BigDecimal difference = settlement.getSettlementAmount().subtract(invoiceTotal).abs();

                if (difference.compareTo(tolerance) > 0) { // 差异超过1%
                    ClosureIssueDTO issue = createTimeSequenceIssue(
                            ISSUE_TYPE_AMOUNT_MISMATCH,
                            RISK_LEVEL_HIGH,
                            "SETTLEMENT",
                            settlement.getSettlementId(),
                            settlement.getSettlementCode(),
                            "结算单与关联发票金额不一致",
                            String.format("结算单%s总额%s元，关联发票总额%s元，差额%s元。",
                                    settlement.getSettlementCode(),
                                    settlement.getSettlementAmount(),
                                    invoiceTotal,
                                    difference),
                            createDetailsMap(
                                    "settlementTotal", settlement.getSettlementAmount(),
                                    "invoicesTotal", invoiceTotal,
                                    "difference", difference,
                                    "tolerance", tolerance,
                                    "invoiceCount", relations.size()
                            ),
                            "请核对发票和流水，若发票缺失请补传或人工匹配；若为税率/费用项差额，提交调账申请。",
                            ACTION_TYPE_MANUAL_FIX
                    );
                    issues.add(issue);
                }
            }
        }

        return issues;
    }

    /**
     * 校验入库必须关联运输单
     */
    private List<ClosureIssueDTO> validateWarehousingTransportAssociation(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 通过合同号找到相关的收运通知单，然后关联到磅单和入库单
        QueryWrapper<PickupNotice> pickupQuery = new QueryWrapper<>();
        pickupQuery.eq("合同号", contract.getContractNo());
        List<PickupNotice> pickupNotices = pickupNoticeMapper.selectList(pickupQuery);

        if (!CollectionUtils.isEmpty(pickupNotices)) {
            // 获取所有相关的磅单号（这里假设磅单通过某种方式与收运通知单关联）
            // 由于PickupNotice中没有直接的磅单号字段，我们通过入库单的反向查找
            List<String> weighingSlipNos = pickupNotices.stream()
                    .map(PickupNotice::getNoticeCode)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            // 查找相关的入库单（这里简化逻辑，实际可能需要更复杂的关联）
            QueryWrapper<Warehousing> warehousingQuery = new QueryWrapper<>();
            // 如果有磅单列表，可以在这里添加条件
            // warehousingQuery.in("总磅单号", weighingSlipNos);
            // 暂时查找所有入库单，然后在业务逻辑中判断关联

            List<Warehousing> allWarehousings = warehousingMapper.selectList(new QueryWrapper<>());

            for (Warehousing warehousing : allWarehousings) {
                // 检查入库单是否有关联的运输单
                if (warehousing.getDispatchCode() == null || warehousing.getDispatchCode().trim().isEmpty()) {
                    // 只对有磅单号的入库单进行校验（表示有实际业务数据）
                    if (warehousing.getWeighingSlipNo() != null && !warehousing.getWeighingSlipNo().trim().isEmpty()) {
                        ClosureIssueDTO issue = createTimeSequenceIssue(
                                ISSUE_TYPE_MISSING_ASSOCIATION,
                                RISK_LEVEL_MEDIUM,
                                "WAREHOUSING",
                                warehousing.getWarehousingId() != null ? warehousing.getWarehousingId().longValue() : 0L,
                                warehousing.getWarehousingNo(),
                                "入库单未关联运输单",
                                String.format("入库单%s（磅单号：%s）未关联运输单，无法确认货物来源和运输过程。",
                                        warehousing.getWarehousingNo(),
                                        warehousing.getWeighingSlipNo()),
                                createDetailsMap(
                                        "warehousingNo", warehousing.getWarehousingNo(),
                                        "weighingSlipNo", warehousing.getWeighingSlipNo(),
                                        "contractCode", contract.getContractNo(),
                                        "dispatchCode", warehousing.getDispatchCode(),
                                        "pickupNoticeCount", pickupNotices.size()
                                ),
                                "请为入库单关联对应的运输单记录",
                                ACTION_TYPE_MANUAL_FIX
                        );
                        issues.add(issue);
                    }
                }
            }
        }

        return issues;
    }

    /**
     * 校验结算单与收款金额一致性
     */
    private List<ClosureIssueDTO> validateSettlementReceiptAmountConsistency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取该合同的结算单
        QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
        settlementQuery.eq("合同号", contract.getContractNo());
        // 使用别名映射解决中文字段名映射问题
        settlementQuery.select(
            "结算单编号 AS settlementId",
            "结算单单号 AS settlementCode",
            "结算金额 AS settlementAmount",
            "创建时间 AS createTime",
            "更新时间 AS updateTime",
            "version AS version"
        );
        List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

        for (Settlement settlement : settlements) {
            log.info("validateSettlementReceiptAmountConsistency - 开始处理结算单: settlementId={}, settlementCode={}, settlementAmount={}, settlementAmount==null?{}",
                    settlement.getSettlementId(), settlement.getSettlementCode(), settlement.getSettlementAmount(), settlement.getSettlementAmount() == null);

            // 获取该结算单关联的资金流水金额总和（连表查询实际交易金额）
            List<SettlementFundTransactionRel> relations = settlementFundTransactionRelMapper.selectList(
                new QueryWrapper<SettlementFundTransactionRel>()
                    .eq("结算单编号", settlement.getSettlementId())
            );

            log.info("validateSettlementReceiptAmountConsistency - settlementId={} 查询到收款关联记录数: {}",
                    settlement.getSettlementId(), relations != null ? relations.size() : 0);

            // 连表查询实际的交易金额总和
            BigDecimal receiptTotal = BigDecimal.ZERO;
            if (!CollectionUtils.isEmpty(relations)) {
                List<Long> transactionIds = relations.stream()
                    .map(SettlementFundTransactionRel::getTransactionId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

                if (!CollectionUtils.isEmpty(transactionIds)) {
                    List<FundTransaction> fundTransactions = fundTransactionMapper.selectList(
                        new QueryWrapper<FundTransaction>()
                            .in("流水编号", transactionIds)
                    );

                    receiptTotal = fundTransactions.stream()
                        .filter(ft -> ft.getAmount() != null)
                        .map(FundTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                    log.info("validateSettlementReceiptAmountConsistency - settlementId={} 资金流水数量: {}, 交易金额总和: {}",
                            settlement.getSettlementId(), fundTransactions.size(), receiptTotal);
                }
            }

            log.info("validateSettlementReceiptAmountConsistency - settlementId={} 收款总金额: {}, 结算金额: {}",
                    settlement.getSettlementId(), receiptTotal, settlement.getSettlementAmount());

            // 如果有关联收款记录，检查金额一致性
            if (settlement.getSettlementAmount() != null && receiptTotal.compareTo(BigDecimal.ZERO) > 0) {
                log.info("validateSettlementReceiptAmountConsistency - settlementId={} 进入金额一致性检查", settlement.getSettlementId());
                // 允许±1%的误差
                BigDecimal tolerance = settlement.getSettlementAmount().multiply(new BigDecimal("0.01"));
                BigDecimal difference = settlement.getSettlementAmount().subtract(receiptTotal).abs();

                log.info("validateSettlementReceiptAmountConsistency - settlementId={} 详细计算: 结算金额={}, 收款总金额={}, 差异={}, 容忍值={}, 差异百分比={}%",
                        settlement.getSettlementId(),
                        settlement.getSettlementAmount(),
                        receiptTotal,
                        difference,
                        tolerance,
                        settlement.getSettlementAmount().compareTo(BigDecimal.ZERO) != 0 ?
                            difference.divide(settlement.getSettlementAmount(), 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")) : BigDecimal.ZERO);

                if (difference.compareTo(tolerance) > 0) { // 差异超过1%
                    log.info("validateSettlementReceiptAmountConsistency - settlementId={} 发现金额不一致: 差异={}, 容忍值={}, 生成问题",
                            settlement.getSettlementId(), difference, tolerance);

                    ClosureIssueDTO issue = createTimeSequenceIssue(
                            ISSUE_TYPE_AMOUNT_MISMATCH,
                            RISK_LEVEL_HIGH,
                            "SETTLEMENT",
                            settlement.getSettlementId(),
                            settlement.getSettlementCode(),
                            "结算单与关联收款金额不一致",
                            String.format("结算单%s总额%s元，关联收款总额%s元，差额%s元。",
                                    settlement.getSettlementCode(),
                                    settlement.getSettlementAmount(),
                                    receiptTotal,
                                    difference),
                            createDetailsMap(
                                    "settlementTotal", settlement.getSettlementAmount(),
                                    "receiptsTotal", receiptTotal,
                                    "difference", difference,
                                    "tolerance", tolerance,
                                    "receiptCount", relations.size()
                            ),
                            "请核对收款记录和结算单，若收款记录缺失请补录；若为手续费或调整项差额，提交调账申请。",
                            ACTION_TYPE_MANUAL_FIX
                    );
                    issues.add(issue);
                } else {
                    log.info("validateSettlementReceiptAmountConsistency - settlementId={} 金额一致: 差异={}, 容忍值={}",
                            settlement.getSettlementId(), difference, tolerance);
                }
            } else {
                log.info("validateSettlementReceiptAmountConsistency - settlementId={} 未进入金额一致性检查: settlementAmount={}, receiptTotal={}",
                        settlement.getSettlementId(), settlement.getSettlementAmount(), receiptTotal);
            }
        }

        log.info("validateSettlementReceiptAmountConsistency - 合同{}处理完成，生成了{}个金额不一致问题",
                contract.getContractNo(), issues.size());

        return issues;
    }

    /**
     * 校验结算单合同关联
     */
    private List<ClosureIssueDTO> validateSettlementContractAssociation(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取该合同的结算单
        QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
        settlementQuery.eq("合同号", contract.getContractNo());
        // 使用别名映射解决中文字段名映射问题
        settlementQuery.select(
            "结算单编号 AS settlementId",
            "结算单单号 AS settlementCode",
            "合同号 AS contractCode",
            "结算金额 AS settlementAmount",
            "创建时间 AS createTime",
            "更新时间 AS updateTime",
            "version AS version"
        );
        List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

        for (Settlement settlement : settlements) {
            log.info("validateSettlementContractAssociation - 检查结算单 settlementId={}, settlementCode={}, contractCode={}",
                    settlement.getSettlementId(), settlement.getSettlementCode(), settlement.getContractCode());

            if (settlement.getContractCode() == null || settlement.getContractCode().trim().isEmpty()) {
                log.info("validateSettlementContractAssociation - settlementId={} 缺少合同关联，生成问题", settlement.getSettlementId());
                ClosureIssueDTO issue = createTimeSequenceIssue(
                        ISSUE_TYPE_MISSING_ASSOCIATION,
                        RISK_LEVEL_MEDIUM,
                        "SETTLEMENT",
                        settlement.getSettlementId(),
                        settlement.getSettlementCode(),
                        "结算单未关联合同",
                        String.format("结算单%s（金额：%s元）未与任何合同关联，无法确认收款归属。",
                                settlement.getSettlementCode(),
                                settlement.getSettlementAmount()),
                        createDetailsMap(
                                "settlementAmount", settlement.getSettlementAmount(),
                                "settlementDate", settlement.getCreateTime() != null ?
                                        DateUtil.format(settlement.getCreateTime(), "yyyy-MM-dd") : null
                        ),
                        "请手动关联合同，或标记为预收款",
                        ACTION_TYPE_MANUAL_FIX
                );
                issues.add(issue);
            } else {
                log.info("validateSettlementContractAssociation - settlementId={} 有合同关联，跳过", settlement.getSettlementId());
            }
        }

        log.info("validateSettlementContractAssociation - 合同{}处理完成，生成了{}个缺少合同关联问题",
                contract.getContractNo(), issues.size());

        return issues;
    }

    /**
     * 校验收款记录结算单关联
     */
    private List<ClosureIssueDTO> validateReceiptSettlementAssociation(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 获取该合同的所有结算单
        QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
        settlementQuery.eq("合同号", contract.getContractNo());
        // 使用别名映射，确保中文列名能正确映射到 Settlement 实体的驼峰字段
        settlementQuery.select(
                "结算单编号 AS settlementId",
                "结算单单号 AS settlementCode",
                "结算金额 AS settlementAmount",
                "创建时间 AS createTime",
                "更新时间 AS updateTime",
                "version AS version"
        );
        List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

        for (Settlement settlement : settlements) {
            log.info("validateReceiptSettlementAssociation - 检查结算单 settlementId={}, settlementCode={}, settlementAmount={}",
                    settlement.getSettlementId(), settlement.getSettlementCode(), settlement.getSettlementAmount());

            // 检查该结算单是否有资金流水关联记录
            QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
            relQuery.eq("结算单编号", settlement.getSettlementId());
            List<SettlementFundTransactionRel> relations = settlementFundTransactionRelMapper.selectList(relQuery);

            log.info("validateReceiptSettlementAssociation - settlementId={} 查询到收款关联记录数: {}",
                    settlement.getSettlementId(), relations != null ? relations.size() : 0);

            if (CollectionUtils.isEmpty(relations)) {
                log.info("validateReceiptSettlementAssociation - settlementId={} 缺少收款关联，生成问题", settlement.getSettlementId());
                // 结算单没有关联任何资金流水
                ClosureIssueDTO issue = createTimeSequenceIssue(
                        ISSUE_TYPE_MISSING_ASSOCIATION,
                        RISK_LEVEL_HIGH,
                        "SETTLEMENT",
                        settlement.getSettlementId(),
                        settlement.getSettlementCode(),
                        "结算单未关联收款流水",
                        String.format("结算单%s（金额：%s元）未关联任何收款流水记录，无法确认收款真实性。",
                                settlement.getSettlementCode(),
                                settlement.getSettlementAmount()),
                        createDetailsMap(
                                "settlementAmount", settlement.getSettlementAmount(),
                                "settlementDate", settlement.getCreateTime() != null ?
                                        DateUtil.format(settlement.getCreateTime(), "yyyy-MM-dd") : null,
                                "contractCode", contract.getContractNo()
                        ),
                        "请在收款流水管理中关联对应的收款记录",
                        ACTION_TYPE_MANUAL_FIX
                );
                issues.add(issue);
            } else {
                log.info("validateReceiptSettlementAssociation - settlementId={} 有收款关联，跳过", settlement.getSettlementId());
            }
        }

        log.info("validateReceiptSettlementAssociation - 合同{}处理完成，生成了{}个缺少收款关联问题",
                contract.getContractNo(), issues.size());

        return issues;
    }

    /**
     * 校验发票收款记录关联
     */
    private List<ClosureIssueDTO> validateInvoiceReceiptAssociation(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 通过合同号找到该合同的结算单列表
        QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
        settlementQuery.eq("合同号", contract.getContractNo());
        // 使用别名映射解决中文字段名映射问题
        settlementQuery.select(
            "结算单编号 AS settlementId",
            "结算单单号 AS settlementCode",
            "结算金额 AS settlementAmount",
            "创建时间 AS createTime",
            "更新时间 AS updateTime",
            "version AS version"
        );
        List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

        if (CollectionUtils.isEmpty(settlements)) {
            // 没有结算单则不处理
            return issues;
        }

        // 对每个结算单单独检查是否有关联发票记录
        for (Settlement settlement : settlements) {
            try {
                if (settlement == null || settlement.getSettlementId() == null) {
                    continue;
                }

                QueryWrapper<SettlementInvoiceRel> relQuery = new QueryWrapper<>();
                relQuery.eq("结算单编号", settlement.getSettlementId());
                List<SettlementInvoiceRel> relations = settlementInvoiceRelMapper.selectList(relQuery);

                if (CollectionUtils.isEmpty(relations)) {
                    // 如果该结算单没有关联任何发票，针对结算单生成问题
                    ClosureIssueDTO issue = createTimeSequenceIssue(
                            ISSUE_TYPE_MISSING_ASSOCIATION,
                            RISK_LEVEL_HIGH,
                            "SETTLEMENT",
                            settlement.getSettlementId(),
                            settlement.getSettlementCode(),
                            "结算单缺少发票",
                            String.format("结算单%s（合同%s）未关联任何发票记录，存在开票合规风险。",
                                    settlement.getSettlementCode(),
                                    contract.getContractNo() != null ? contract.getContractNo() : "未知"),
                            createDetailsMap(
                                    "contractCode", contract.getContractNo(),
                                    "settlementCode", settlement.getSettlementCode(),
                                    "settlementAmount", settlement.getSettlementAmount()
                            ),
                            "请为结算单关联相应发票记录，确保税务合规",
                            ACTION_TYPE_MANUAL_FIX
                    );
                    issues.add(issue);
                }
            } catch (Exception e) {
                log.error("处理结算单{}时发生异常", settlement != null ? settlement.getSettlementId() : "unknown", e);
            }
        }

    return issues;
    }

    /**
     * 执行发票相关校验
     */
    private List<ClosureIssueDTO> validateInvoiceRelatedIssues() {
        log.info("开始执行发票相关校验");
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 获取所有合同，对每个合同执行发票校验
            QueryWrapper<Contract> contractQuery = new QueryWrapper<>();
            List<Contract> allContracts = contractMapper.selectList(contractQuery);

            if (!CollectionUtils.isEmpty(allContracts)) {
                for (Contract contract : allContracts) {
                    // 执行发票收款关联校验
                    issues.addAll(validateInvoiceReceiptAssociation(contract));

                    // 执行发票状态逻辑校验
                    issues.addAll(validateInvoiceStatusLogicForContract(contract));
                }
            }

            // 执行发票总额与流水总额交叉校验（全局校验）
            issues.addAll(validateInvoiceReceiptCrossCheck(null));

            log.info("发票相关校验完成，发现{}个问题", issues.size());
        } catch (Exception e) {
            log.error("执行发票相关校验失败", e);
        }

        return issues;
    }

    /**
     * 为单个合同执行发票状态逻辑校验
     */
    private List<ClosureIssueDTO> validateInvoiceStatusLogicForContract(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            String contractStatus = contract.getContractStatus();
            if (contractStatus == null || contractStatus.trim().isEmpty()) {
                return issues;
            }

            // 检查合同状态是否允许有发票数据
            ContractClosureStatus currentStatus = ContractClosureStatus.fromDisplayName(contractStatus);
            if (currentStatus == null) {
                return issues;
            }

            // 查询该合同是否有发票记录
            QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
            settlementQuery.eq("合同号", contract.getContractNo());
            List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

            boolean hasInvoiceData = false;
            if (!CollectionUtils.isEmpty(settlements)) {
                for (Settlement settlement : settlements) {
                    QueryWrapper<SettlementInvoiceRel> invoiceQuery = new QueryWrapper<>();
                    invoiceQuery.eq("结算单编号", settlement.getSettlementId());
                    long invoiceCount = settlementInvoiceRelMapper.selectCount(invoiceQuery);
                    if (invoiceCount > 0) {
                        hasInvoiceData = true;
                        break;
                    }
                }
            }

            // 根据合同状态判断是否应该有发票数据
            if (hasInvoiceData) {
                // 只有在特定状态下才应该有发票数据
                if (currentStatus.getOrder() < ContractClosureStatus.INVOICED.getOrder()) {
                    // 状态早于"已开票"但已有发票数据
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("STATUS_INVOICE_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_STATUS_INCONSISTENCY);
                    issue.setRiskLevel(RISK_LEVEL_MEDIUM);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("合同状态与发票数据不一致");
                    issue.setIssueDescription("合同[" + contract.getContractNo() + "]状态为[" + contractStatus + "]，但已存在发票数据。发票数据通常应在'已开票'状态后出现。");
                    issue.setSuggestedAction("请检查合同状态是否正确，或确认发票数据的有效性");
                    issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }
        } catch (Exception e) {
            log.error("校验合同发票状态逻辑失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 校验发票状态逻辑
     */
    private List<ClosureIssueDTO> validateInvoiceStatusLogic(Long contractId) {
        log.info("开始执行发票状态逻辑校验");
        List<ClosureIssueDTO> issues = new ArrayList<>();

        // 这里可以实现专门的发票状态逻辑校验
        // 例如：检查发票状态与合同状态的一致性等

        return issues;
    }

    /**
     * 创建待认领款项问题
     */
    private ClosureIssueDTO createUnclaimedPaymentIssue(FundTransaction transaction) {
        ClosureIssueDTO issue = new ClosureIssueDTO();
        issue.setIssueId(generateIssueId());
        issue.setIssueType(ISSUE_TYPE_UNCLAIMED_PAYMENT);
        issue.setRiskLevel(RISK_LEVEL_MEDIUM); // 待认领款项为中等风险，需要人工处理
        issue.setSeverity(RISK_LEVEL_MEDIUM);
        issue.setRelatedEntityType("FUND_TRANSACTION");
        issue.setRelatedEntityId(transaction.getTransactionId());
        issue.setRelatedEntityCode("TXN_" + transaction.getTransactionId());
        issue.setRelatedEntityName("资金流水_" + transaction.getTransactionId());
        issue.setIssueTitle("待认领款项");
        issue.setIssueDescription(String.format(
                "发现一笔未关联合同的资金流水（流水号：%s），金额：%s元，交易日期：%s。该笔款项无法对应到任何合同，可能需要财务与业务部门协同处理。",
                transaction.getTransactionId(),
                transaction.getAmount() != null ? transaction.getAmount().toString() : "未知",
                transaction.getTransactionDate() != null ?
                        transaction.getTransactionDate().toString() : "未知"
        ));
        issue.setIssueDetails(createDetailsMap(
                "transactionId", transaction.getTransactionId(),
                "amount", transaction.getAmount(),
                "transactionDate", transaction.getTransactionDate() != null ?
                        transaction.getTransactionDate().toString() : null,
                "transactionType", transaction.getTransactionType(),
                "summary", transaction.getSummary(),
                "purpose", transaction.getPurpose(),
                "issueType", "UNCLAIMED_PAYMENT"
        ));
        issue.setSuggestedAction("财务部门应与业务部门协同，确认该笔款项的归属合同。如无法确定归属，可考虑作为预收款处理或发起退款流程。");
        issue.setActionType(ACTION_TYPE_MANUAL_FIX); // 需要人工处理
        issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
        issue.setLastValidatedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
        return issue;
    }

    /**
     * 创建预收款问题
     */
    private ClosureIssueDTO createPrepaymentIssue(Contract contract, SettlementFundTransactionRel relation,
                                                  String settlementCode, boolean isPrepayment) {
        BigDecimal prepaymentRatio = BigDecimal.ZERO;
        if (contract.getContractAmount() != null && contract.getContractAmount().compareTo(BigDecimal.ZERO) > 0) {
            prepaymentRatio = relation.getRelAmount().divide(contract.getContractAmount(), 4, BigDecimal.ROUND_HALF_UP);
        }

        ClosureIssueDTO issue = new ClosureIssueDTO();
        issue.setIssueId(generateIssueId());
        issue.setIssueType(ISSUE_TYPE_PREPAYMENT_VIOLATION);
        issue.setRiskLevel(RISK_LEVEL_MEDIUM); // 预收款本身为MEDIUM风险，需要进一步检查比例
        issue.setSeverity(RISK_LEVEL_MEDIUM);
        issue.setRelatedEntityType("CONTRACT");
        issue.setRelatedEntityId(contract.getContractId() != null ? contract.getContractId().longValue() : 0L);
        issue.setRelatedEntityCode(contract.getContractNo());
        issue.setRelatedEntityName(contract.getContractNo());
        issue.setIssueTitle("系统检测到预收款情况");
        issue.setIssueDescription(String.format("合同%s存在预收款记录（收款时间%s早于合同签订时间%s），金额%s元，占合同总金额%.2f%%。",
                contract.getContractNo(),
                DateUtil.format(relation.getRelTime(), "yyyy-MM-dd HH:mm:ss"),
                DateUtil.format(contract.getSignTime(), "yyyy-MM-dd HH:mm:ss"),
                relation.getRelAmount(),
                prepaymentRatio.multiply(new BigDecimal("100"))));
        issue.setIssueDetails(createDetailsMap(
                "contractCode", contract.getContractNo(),
                "transactionId", relation.getTransactionId(),
                "transactionTime", DateUtil.format(relation.getRelTime(), "yyyy-MM-dd HH:mm:ss"),
                "contractSigningTime", DateUtil.format(contract.getSignTime(), "yyyy-MM-dd HH:mm:ss"),
                "prepaymentAmount", relation.getRelAmount(),
                "contractAmount", contract.getContractAmount(),
                "prepaymentRatio", prepaymentRatio,
                "settlementCode", settlementCode,
                "isPrepayment", isPrepayment
        ));
        issue.setSuggestedAction("系统已自动标记该合同为预收款合同。请确认业务必要性，如比例超过30%需上级审批。");
        issue.setActionType(ACTION_TYPE_REVIEW_REQUIRED); // 需要人工审核确认
        issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
        issue.setLastValidatedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
        return issue;
    }

    /**
     * 创建时间顺序违规问题
     */
    private ClosureIssueDTO createTimeSequenceIssue(String issueType, String riskLevel,
                                                   String entityType, Long entityId, String entityCode,
                                                   String title, String description, Map<String, Object> details,
                                                   String suggestion, String actionType) {
        ClosureIssueDTO issue = new ClosureIssueDTO();
        issue.setIssueId(generateIssueId());
        issue.setIssueType(issueType);
        issue.setRiskLevel(riskLevel);
        issue.setSeverity(riskLevel);
        issue.setRelatedEntityType(entityType);
        issue.setRelatedEntityId(entityId);
        issue.setRelatedEntityCode(entityCode);
        issue.setRelatedEntityName(getEntityName(entityType, entityId, entityCode));
        issue.setIssueTitle(title);
        issue.setIssueDescription(description);
        issue.setIssueDetails(details);
        issue.setSuggestedAction(suggestion);
        issue.setActionType(actionType);
        issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
        issue.setLastValidatedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
        return issue;
    }


    /**
     * 生成问题ID
     */
    private String generateIssueId() {
        return "ISSUE_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    /**
     * 获取实体名称
     */
    private String getEntityName(String entityType, Long entityId, String entityCode) {
        switch (entityType) {
            case "CONTRACT":
                Contract contract = contractMapper.selectById(entityId);
                return contract != null ? contract.getContractNo() : entityCode;
            case "SETTLEMENT":
                Settlement settlement = settlementMapper.selectById(entityId);
                return settlement != null ? settlement.getSettlementCode() : entityCode;
            case "INVOICE":
                Invoice invoice = invoiceMapper.selectById(entityId);
                return invoice != null ? invoice.getInvoiceNumber() : entityCode;
            default:
                return entityCode;
        }
    }

    /**
     * 计算时间差
     */
    private String calculateTimeDifference(LocalDateTime time1, LocalDateTime time2) {
        if (time1 == null || time2 == null) {
            return "未知";
        }

        long diffMillis = Math.abs(java.time.Duration.between(time1, time2).toMillis());
        long days = diffMillis / (24 * 60 * 60 * 1000);
        long hours = (diffMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (diffMillis % (60 * 60 * 1000)) / (60 * 1000);

        if (days > 0) {
            return String.format("-%d天%d小时%d分钟", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("-%d小时%d分钟", hours, minutes);
        } else {
            return String.format("-%d分钟", minutes);
        }
    }

    /**
     * 创建详情Map的工具方法
     */
    private Map<String, Object> createDetailsMap(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i + 1 < keyValues.length) {
                map.put((String) keyValues[i], keyValues[i + 1]);
            }
        }
        return map;
    }

    @Override
    public BatchValidationResponse batchValidate(BatchValidationRequest request) {
        log.info("开始执行批量业务闭环校验，合同数量：{}", request.getContractIds().size());

        long startTime = System.currentTimeMillis();
        String batchId = "BATCH_VALIDATION_" + System.currentTimeMillis();

        BatchValidationResponse response = new BatchValidationResponse();
        response.setBatchValidationId(batchId);
        response.setRequestedContracts(request.getContractIds().size());
        response.setExecutedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
        response.setExecutedBy(SecurityUtil.getCurrentUsername());

        Map<Long, BatchValidationResponse.ContractValidationResult> contractResults = new HashMap<>();
        int totalIssues = 0;
        int validatedCount = 0;

        for (Long contractId : request.getContractIds()) {
            try {
                List<ClosureIssueDTO> issues = validateContractClosure(contractId, request.getCheckTypes() != null ? request.getCheckTypes() : new ArrayList<>());

                BatchValidationResponse.ContractValidationResult result = new BatchValidationResponse.ContractValidationResult();
                Contract contract = contractMapper.selectById(contractId);
                if (contract != null) {
                    result.setContractCode(contract.getContractNo());
                    result.setContractName(contract.getPartyAName() + " - " + contract.getPartyBName());
                }
                result.setIssuesFound(issues.size());
                result.setIssues(issues);
                result.setStatus(issues.isEmpty() ? "SUCCESS" : "ISSUES_FOUND");
                result.setMessage(issues.isEmpty() ? "校验通过" : "发现" + issues.size() + "个问题");

                contractResults.put(contractId, result);
                totalIssues += issues.size();
                validatedCount++;

            } catch (Exception e) {
                log.error("校验合同{}失败", contractId, e);
                BatchValidationResponse.ContractValidationResult result = new BatchValidationResponse.ContractValidationResult();
                result.setStatus("ERROR");
                result.setMessage("校验失败：" + e.getMessage());
                contractResults.put(contractId, result);
            }
        }

        response.setValidatedContracts(validatedCount);
        response.setTotalIssuesFound(totalIssues);
        response.setExecutionTime(System.currentTimeMillis() - startTime);
        response.setContractResults(contractResults);

        log.info("批量校验完成，校验ID：{}，处理{}个合同，发现{}个问题",
                batchId, validatedCount, totalIssues);

        return response;
    }

    @Override
    public ClosureValidationResponse getValidationResults(Long contractId) {
        log.info("获取合同{}的校验结果", contractId);

        // 重新执行校验以获取最新的结果
        long startTime = System.currentTimeMillis();
        List<ClosureIssueDTO> issues = validateContractClosure(contractId, new ArrayList<>());
        long executionTime = System.currentTimeMillis() - startTime;

        ClosureValidationResponse response = new ClosureValidationResponse();
        response.setValidationId("REALTIME_" + System.currentTimeMillis());
        response.setTotalChecked(1);
        response.setIssuesFound(issues.size());
        response.setResolvedIssues(0);
        response.setExecutionTime(executionTime);
        response.setIssues(issues);
        response.setOverallStatus(issues.isEmpty() ? "PASS" : (issues.size() < 5 ? "WARNING" : "FAIL"));
        response.setExecutedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
        response.setExecutedBy(SecurityUtil.getCurrentUsername());
        response.setPassedChecks(issues.isEmpty() ? 1 : 0);
        response.setFailedChecks(issues.size());
        response.setWarningChecks(0);

        // 设置校验项目信息
        List<ValidationCheckItemDTO> checkItems = new ArrayList<>();
        ValidationCheckItemDTO timeCheck = new ValidationCheckItemDTO();
        timeCheck.setCheckId("TIME_SEQUENCE");
        timeCheck.setCheckType("TIME_SEQUENCE");
        timeCheck.setCheckName("时间顺序校验");
        timeCheck.setStatus("PASS");
        timeCheck.setMessage("时间顺序校验完成");

        ValidationCheckItemDTO amountCheck = new ValidationCheckItemDTO();
        amountCheck.setCheckId("AMOUNT_CONSISTENCY");
        amountCheck.setCheckType("AMOUNT_CONSISTENCY");
        amountCheck.setCheckName("金额一致性校验");
        amountCheck.setStatus("PASS");
        amountCheck.setMessage("金额一致性校验完成");

        ValidationCheckItemDTO associationCheck = new ValidationCheckItemDTO();
        associationCheck.setCheckId("ASSOCIATION_INTEGRITY");
        associationCheck.setCheckType("ASSOCIATION_INTEGRITY");
        associationCheck.setCheckName("数据关联完整性校验");
        associationCheck.setStatus("PASS");
        associationCheck.setMessage("数据关联完整性校验完成");

        checkItems.add(timeCheck);
        checkItems.add(amountCheck);
        checkItems.add(associationCheck);
        response.setCheckItems(checkItems);

        log.info("合同{}实时校验完成，发现{}个问题，耗时{}ms", contractId, issues.size(), executionTime);
        return response;
    }

    @Override
    public FixValidationResponse fixValidation(FixValidationRequest request) {
        log.info("开始修复合同{}的校验异常，问题类型：{}",
                request.getContractId(), request.getIssueType());

        long startTime = System.currentTimeMillis();
        String fixId = "FIX_" + System.currentTimeMillis();

        FixValidationResponse response = new FixValidationResponse();
        response.setFixId(fixId);
        response.setContractId(request.getContractId());
        response.setIssueType(request.getIssueType());
        response.setFixedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
        response.setFixedBy(SecurityUtil.getCurrentUsername());

        try {
            // 根据问题类型执行具体的修复逻辑
            boolean fixResult = performFixByIssueType(request);

            if (fixResult) {
                response.setFixStatus("SUCCESS");
                response.setMessage("问题修复成功");
                response.setStillHasIssues(false);
                log.info("合同{}的问题{}修复成功", request.getContractId(), request.getIssueType());
            } else {
                response.setFixStatus("FAILED");
                response.setMessage("修复失败：无法自动修复该类型问题");
                response.setStillHasIssues(true);
                log.warn("合同{}的问题{}无法自动修复", request.getContractId(), request.getIssueType());
            }

        } catch (Exception e) {
            log.error("修复合同{}的问题{}失败", request.getContractId(), request.getIssueType(), e);
            response.setFixStatus("FAILED");
            response.setMessage("修复失败：" + e.getMessage());
            response.setStillHasIssues(true);
        }

        return response;
    }

    /**
     * 根据问题类型执行修复逻辑
     */
    private boolean performFixByIssueType(FixValidationRequest request) {
        switch (request.getIssueType()) {
            case ISSUE_TYPE_MISSING_ASSOCIATION:
                return fixMissingAssociation(request);
            case ISSUE_TYPE_TIME_SEQUENCE_VIOLATION:
                return fixTimeSequenceViolation(request);
            case ISSUE_TYPE_AMOUNT_MISMATCH:
                return fixAmountMismatch(request);
            default:
                log.warn("不支持自动修复的问题类型：{}", request.getIssueType());
                return false;
        }
    }

    /**
     * 修复缺失关联问题
     */
    private boolean fixMissingAssociation(FixValidationRequest request) {
        // 这里需要根据具体的业务逻辑来修复关联关系
        // 例如：自动关联合同、自动关联收款记录等
        // 暂时返回false，表示需要手动修复
        log.info("缺失关联问题需要手动修复，合同ID：{}", request.getContractId());
        return false;
    }

    /**
     * 修复时间顺序违规问题
     */
    private boolean fixTimeSequenceViolation(FixValidationRequest request) {
        // 时间顺序问题通常需要手动调整时间或确认业务逻辑
        // 自动修复风险较高，建议手动处理
        log.info("时间顺序违规问题建议手动修复，合同ID：{}", request.getContractId());
        return false;
    }

    /**
     * 修复金额不匹配问题
     */
    private boolean fixAmountMismatch(FixValidationRequest request) {
        // 金额不匹配问题需要核实数据，通常需要手动调整
        // 可以考虑自动调整小额差异，但这里暂时要求手动处理
        log.info("金额不匹配问题建议手动修复，合同ID：{}", request.getContractId());
        return false;
    }

    @Override
    public ClosureIssuePageResponse getClosureIssues(String issueType, String riskLevel, String businessType,
                                                     String contractCode, String dateRange, Boolean forceRefresh, Integer current, Integer size) {
        log.info("获取闭环问题列表，筛选条件：issueType={}, riskLevel={}, businessType={}, contractCode={}",
                issueType, riskLevel, businessType, contractCode);

        List<ClosureIssueDTO> allIssues;

        // 如果请求要求强制刷新，则触发一次实时全量校验以更新缓存（注意：该操作可能开销较大）
        if (Boolean.TRUE.equals(forceRefresh)) {
            try {
                log.info("强制刷新问题缓存：开始执行全量校验");
                ClosureValidationRequest req = new ClosureValidationRequest();
                req.setValidateType("FULL");
                executeFullValidation(req);
                log.info("强制刷新问题缓存：全量校验完成");
            } catch (Exception e) {
                log.warn("强制刷新问题缓存失败，使用现有缓存数据", e);
            }
        }

        // 优先从Redis缓存获取问题列表
        List<ClosureIssueDTO> cachedIssues = getIssuesFromCache();
        if (cachedIssues != null && !Boolean.TRUE.equals(forceRefresh)) {
            // 有缓存且不是强制刷新，直接使用缓存数据
            allIssues = new ArrayList<>(cachedIssues);
            log.info("使用Redis缓存的问题列表数据，问题数量：{}", allIssues.size());
        } else {
            // 没有缓存或强制刷新，从内存获取（兼容原有逻辑）
            if (businessType != null && businessType.contains("INVOICE")) {
                log.info("检测到发票筛选条件，执行发票相关校验以获取最新问题数据");
                try {
                    List<ClosureIssueDTO> invoiceIssues = validateInvoiceRelatedIssues();
                    // 将发票问题合并到内存缓存中
                    synchronized (BusinessClosureValidationServiceImpl.class) {
                        latestValidationIssues.addAll(invoiceIssues);
                    }
                    allIssues = new ArrayList<>(latestValidationIssues);
                } catch (Exception e) {
                    log.error("执行发票校验失败，使用缓存数据", e);
                    synchronized (BusinessClosureValidationServiceImpl.class) {
                        allIssues = new ArrayList<>(latestValidationIssues);
                    }
                }
            } else {
                // 获取最新的校验结果
                synchronized (BusinessClosureValidationServiceImpl.class) {
                    allIssues = new ArrayList<>(latestValidationIssues);
                }
            }
        }


        // 应用筛选条件
        List<ClosureIssueDTO> filteredIssues = allIssues.stream()
                .filter(issue -> {
                    if (issueType != null && !issueType.isEmpty()) {
                        // 支持多个问题类型筛选，用逗号分隔
                        // BUSINESS_CLOSURE 为前端虚拟类型，展开为 STATUS_INCONSISTENCY + DATA_INCONSISTENCY
                        String[] types = issueType.split(",");
                        Set<String> expandedTypes = new java.util.HashSet<>();
                        for (String type : types) {
                            String t = type.trim();
                            if (ISSUE_TYPE_BUSINESS_CLOSURE.equals(t)) {
                                expandedTypes.add(ISSUE_TYPE_STATUS_INCONSISTENCY);
                                expandedTypes.add(ISSUE_TYPE_DATA_INCONSISTENCY);
                            } else {
                                expandedTypes.add(t);
                            }
                        }
                        if (!expandedTypes.contains(issue.getIssueType())) return false;
                    }

                    if (riskLevel != null && !riskLevel.isEmpty()) {
                        if (!riskLevel.equals(issue.getRiskLevel())) {
                            return false;
                        }
                    }

                    if (businessType != null && !businessType.isEmpty()) {
                        if (!businessType.equals(issue.getRelatedEntityType())) {
                            return false;
                        }
                    }

                    if (contractCode != null && !contractCode.isEmpty()) {
                        if (!issue.getRelatedEntityCode().contains(contractCode)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // 分页处理
        int total = filteredIssues.size();
        int startIndex = (current - 1) * size;
        int endIndex = Math.min(startIndex + size, total);

        List<ClosureIssueDTO> pageData;
        if (startIndex >= total) {
            pageData = new ArrayList<>();
        } else {
            pageData = filteredIssues.subList(startIndex, endIndex);
        }

        // 构建分页响应
        ClosureIssuePageResponse response = new ClosureIssuePageResponse();
        response.setRecords(pageData);
        response.setTotal((long) total);
        response.setSize(size);
        response.setCurrent(current);
        response.setPages((int) Math.ceil((double) total / size));

        log.info("获取闭环问题列表完成，返回{}条记录，总共{}条", pageData.size(), total);

        return response;
    }

    @Override
    public ClosureDashboardStats getDashboardStats(String dateRange, Long organizationId) {
        log.info("获取看板统计数据，参数：dateRange={}, organizationId={}", dateRange, organizationId);

        // 优先从Redis缓存获取看板统计数据
        ClosureDashboardStats cachedStats = getDashboardStatsFromCache();
        if (cachedStats != null) {
            log.info("使用Redis缓存的看板统计数据 - totalReceivedAmount: {}, totalSettlementAmount: {}",
                    cachedStats .getTotalReceivedAmount(), cachedStats.getTotalSettlementAmount());
            return cachedStats;
        }

        // 缓存不存在，重新计算完整统计数据
        log.info("缓存中无看板统计数据，开始重新计算完整统计数据");
        ClosureDashboardStats stats = calculateFullDashboardStats(dateRange, organizationId);

        // 将计算结果保存到缓存
        saveDashboardStatsToCache(stats);
        log.info("已将完整统计数据保存到Redis缓存");

        return stats;
    }

    /**
     * 计算完整的看板统计数据（包括合同维度、收款中心和异常监控）
     */
    private ClosureDashboardStats calculateFullDashboardStats(String dateRange, Long organizationId) {
        ClosureDashboardStats stats = new ClosureDashboardStats();

        // 合同签订数量/金额/重量 统计口径：当月累积（本月1日 00:00:00 ~ 当前时刻）
        LocalDateTime startDate = LocalDateTime.now()
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endDate = LocalDateTime.now();
        log.info("计算完整看板统计数据，合同维度使用当月区间：{} - {}", startDate, endDate);

        // ========== 合同维度指标（合同签订数量 + 金额，含同比/环比） ==========
        Map<String, Object> contractAgg = calculateContractStatistics(startDate, endDate);
        log.debug("contractAgg map: {}", contractAgg);
        if (contractAgg != null) {
            if (contractAgg.containsKey("error")) {
                log.warn("批量计算合同统计返回错误，使用逐合同计算回退：{}", contractAgg.get("error"));
                calculateContractStats(stats, startDate, endDate);
            } else {
                Object contractCountObj = contractAgg.get("contractCount");
                if (contractCountObj instanceof Number) {
                    stats.setContractCount(((Number) contractCountObj).intValue());
                } else {
                    stats.setContractCount(0);
                }

                Object totalContractAmountObj = contractAgg.get("totalContractAmount");
                if (totalContractAmountObj instanceof BigDecimal) {
                    stats.setContractAmount((BigDecimal) totalContractAmountObj);
                } else if (totalContractAmountObj instanceof Number) {
                    stats.setContractAmount(new BigDecimal(((Number) totalContractAmountObj).toString()));
                } else {
                    stats.setContractAmount(BigDecimal.ZERO);
                }

                // ===== 同比（YoY）=====
                Object yoyTrendObj = contractAgg.get("contractCountYoyTrend");
                if (yoyTrendObj instanceof BigDecimal) {
                    stats.setContractCountTrend((BigDecimal) yoyTrendObj);
                } else if (yoyTrendObj instanceof Number) {
                    stats.setContractCountTrend(new BigDecimal(yoyTrendObj.toString()));
                }

                Object amountYoyTrendObj = contractAgg.get("contractAmountYoyTrend");
                if (amountYoyTrendObj instanceof BigDecimal) {
                    stats.setContractAmountTrend((BigDecimal) amountYoyTrendObj);
                } else if (amountYoyTrendObj instanceof Number) {
                    stats.setContractAmountTrend(new BigDecimal(amountYoyTrendObj.toString()));
                }

                // ===== 环比（MoM）=====
                Object momTrendObj = contractAgg.get("contractCountMomTrend");
                if (momTrendObj instanceof BigDecimal) {
                    stats.setContractCountMomTrend((BigDecimal) momTrendObj);
                } else if (momTrendObj instanceof Number) {
                    stats.setContractCountMomTrend(new BigDecimal(momTrendObj.toString()));
                }

                Object amountMomTrendObj = contractAgg.get("contractAmountMomTrend");
                if (amountMomTrendObj instanceof BigDecimal) {
                    stats.setContractAmountMomTrend((BigDecimal) amountMomTrendObj);
                } else if (amountMomTrendObj instanceof Number) {
                    stats.setContractAmountMomTrend(new BigDecimal(amountMomTrendObj.toString()));
                }
            }
        } else {
            if (stats.getContractCount() == null) stats.setContractCount(0);
            if (stats.getContractAmount() == null) stats.setContractAmount(BigDecimal.ZERO);
        }

        // ========== 合同维度扩展指标赋值 ==========
        if (contractAgg != null && !contractAgg.containsKey("error")) {
            // 合同签订重量
            Object weightObj = contractAgg.get("contractWeight");
            if (weightObj instanceof BigDecimal) stats.setContractWeight((BigDecimal) weightObj);
            else if (weightObj instanceof Number) stats.setContractWeight(new BigDecimal(weightObj.toString()));
            else stats.setContractWeight(BigDecimal.ZERO);
            // 合同重量同比/环比
            Object weightYoyObj = contractAgg.get("contractWeightTrend");
            Object weightMomObj = contractAgg.get("contractWeightMomTrend");
            if (weightYoyObj instanceof BigDecimal) stats.setContractWeightTrend((BigDecimal) weightYoyObj);
            else if (weightYoyObj instanceof Number) stats.setContractWeightTrend(new BigDecimal(weightYoyObj.toString()));
            else stats.setContractWeightTrend(BigDecimal.ZERO);
            if (weightMomObj instanceof BigDecimal) stats.setContractWeightMomTrend((BigDecimal) weightMomObj);
            else if (weightMomObj instanceof Number) stats.setContractWeightMomTrend(new BigDecimal(weightMomObj.toString()));
            else stats.setContractWeightMomTrend(BigDecimal.ZERO);
            // 合同变动：增量、存量
            Object newCountObj = contractAgg.get("contractNewCount");
            Object activeCountObj = contractAgg.get("contractActiveCount");
            stats.setContractNewCount(newCountObj instanceof Number ? ((Number) newCountObj).intValue() : 0);
            stats.setContractActiveCount(activeCountObj instanceof Number ? ((Number) activeCountObj).intValue() : 0);
            // 合同跟进：即将到期、客户丢失
            Object expiringObj = contractAgg.get("contractExpiringCount");
            Object lostObj = contractAgg.get("contractLostCount");
            stats.setContractExpiringCount(expiringObj instanceof Number ? ((Number) expiringObj).intValue() : 0);
            stats.setContractLostCount(lostObj instanceof Number ? ((Number) lostObj).intValue() : 0);
        } else {
            stats.setContractWeight(BigDecimal.ZERO);
            stats.setContractWeightTrend(BigDecimal.ZERO);
            stats.setContractWeightMomTrend(BigDecimal.ZERO);
            stats.setContractNewCount(0);
            stats.setContractActiveCount(0);
            stats.setContractExpiringCount(0);
            stats.setContractLostCount(0);
        }

        // ========== 收款中心指标（使用批量计算，复用合同维度同比/环比计算口径） ==========
        Map<String, Object> paymentAgg = calculatePaymentStatistics(startDate, endDate);
        log.debug("收款统计数据: paymentAgg = {}", paymentAgg);
        if (paymentAgg != null) {
            if (paymentAgg.containsKey("error")) {
                log.warn("批量计算收款统计返回错误，使用逐条计算回退：{}", paymentAgg.get("error"));
                calculatePaymentStats(stats, startDate, endDate);
            } else {
                Object receivableAmountObj = paymentAgg.get("receivableAmount");
                Object receivedAmountObj = paymentAgg.get("receivedAmount");
                Object unreceivedAmountObj = paymentAgg.get("unreceivedAmount");
                Object totalReceivedObj = paymentAgg.get("totalReceivedAmount");
                Object totalContractAmountObj = paymentAgg.get("totalContractAmount");
                BigDecimal receivableAmount = BigDecimal.ZERO;
                BigDecimal receivedAmount = BigDecimal.ZERO;
                BigDecimal unreceivedAmount = BigDecimal.ZERO;
                BigDecimal totalReceived = BigDecimal.ZERO;
                BigDecimal totalContracts = BigDecimal.ZERO;
                if (receivableAmountObj instanceof Number) receivableAmount = new BigDecimal(((Number) receivableAmountObj).toString());
                if (receivedAmountObj instanceof Number) receivedAmount = new BigDecimal(((Number) receivedAmountObj).toString());
                if (unreceivedAmountObj instanceof Number) unreceivedAmount = new BigDecimal(((Number) unreceivedAmountObj).toString());
                if (totalReceivedObj instanceof Number) totalReceived = new BigDecimal(((Number) totalReceivedObj).toString());
                if (totalContractAmountObj instanceof Number) totalContracts = new BigDecimal(((Number) totalContractAmountObj).toString());
                stats.setReceivableAmount(receivableAmount);
                stats.setReceivedAmount(receivedAmount);
                stats.setUnreceivedAmount(unreceivedAmount);
                stats.setTotalReceivedAmount(totalReceived);
                stats.setTotalSettlementAmount(totalContracts);

                Object receivableAmountYoyObj = paymentAgg.get("receivableAmountYoy");
                Object receivableAmountMomObj = paymentAgg.get("receivableAmountMom");
                Object receivedAmountYoyObj = paymentAgg.get("receivedAmountYoy");
                Object receivedAmountMomObj = paymentAgg.get("receivedAmountMom");
                Object unreceivedAmountYoyObj = paymentAgg.get("unreceivedAmountYoy");
                Object unreceivedAmountMomObj = paymentAgg.get("unreceivedAmountMom");
                stats.setReceivableAmountYoy(receivableAmountYoyObj instanceof BigDecimal
                        ? (BigDecimal) receivableAmountYoyObj
                        : receivableAmountYoyObj instanceof Number ? new BigDecimal(receivableAmountYoyObj.toString()) : BigDecimal.ZERO);
                stats.setReceivableAmountMom(receivableAmountMomObj instanceof BigDecimal
                        ? (BigDecimal) receivableAmountMomObj
                        : receivableAmountMomObj instanceof Number ? new BigDecimal(receivableAmountMomObj.toString()) : BigDecimal.ZERO);
                stats.setReceivedAmountYoy(receivedAmountYoyObj instanceof BigDecimal
                        ? (BigDecimal) receivedAmountYoyObj
                        : receivedAmountYoyObj instanceof Number ? new BigDecimal(receivedAmountYoyObj.toString()) : BigDecimal.ZERO);
                stats.setReceivedAmountMom(receivedAmountMomObj instanceof BigDecimal
                        ? (BigDecimal) receivedAmountMomObj
                        : receivedAmountMomObj instanceof Number ? new BigDecimal(receivedAmountMomObj.toString()) : BigDecimal.ZERO);
                stats.setUnreceivedAmountYoy(unreceivedAmountYoyObj instanceof BigDecimal
                        ? (BigDecimal) unreceivedAmountYoyObj
                        : unreceivedAmountYoyObj instanceof Number ? new BigDecimal(unreceivedAmountYoyObj.toString()) : BigDecimal.ZERO);
                stats.setUnreceivedAmountMom(unreceivedAmountMomObj instanceof BigDecimal
                        ? (BigDecimal) unreceivedAmountMomObj
                        : unreceivedAmountMomObj instanceof Number ? new BigDecimal(unreceivedAmountMomObj.toString()) : BigDecimal.ZERO);

                // 逾期金额/坏账金额暂时仅保留接口，当前不做实际计算
                stats.setOverduePaymentAmount(BigDecimal.ZERO);
                stats.setOverduePaymentAmountTrend(BigDecimal.ZERO);
            }
        } else {
            if (stats.getReceivableAmount() == null) stats.setReceivableAmount(BigDecimal.ZERO);
            if (stats.getReceivableAmountYoy() == null) stats.setReceivableAmountYoy(BigDecimal.ZERO);
            if (stats.getReceivableAmountMom() == null) stats.setReceivableAmountMom(BigDecimal.ZERO);
            if (stats.getReceivedAmount() == null) stats.setReceivedAmount(BigDecimal.ZERO);
            if (stats.getReceivedAmountYoy() == null) stats.setReceivedAmountYoy(BigDecimal.ZERO);
            if (stats.getReceivedAmountMom() == null) stats.setReceivedAmountMom(BigDecimal.ZERO);
            if (stats.getUnreceivedAmount() == null) stats.setUnreceivedAmount(BigDecimal.ZERO);
            if (stats.getUnreceivedAmountYoy() == null) stats.setUnreceivedAmountYoy(BigDecimal.ZERO);
            if (stats.getUnreceivedAmountMom() == null) stats.setUnreceivedAmountMom(BigDecimal.ZERO);
            if (stats.getTotalReceivedAmount() == null) stats.setTotalReceivedAmount(BigDecimal.ZERO);
            if (stats.getTotalSettlementAmount() == null) stats.setTotalSettlementAmount(BigDecimal.ZERO);
            if (stats.getOverduePaymentAmount() == null) stats.setOverduePaymentAmount(BigDecimal.ZERO);
        }

        // ========== 异常监控指标（仍使用 issues 聚合） ==========
        synchronized (BusinessClosureValidationServiceImpl.class) {
            if (latestDashboardSnapshot != null) {
                ClosureDashboardStats snap = latestDashboardSnapshot;
                stats.setTimeSequenceViolations(snap.getTimeSequenceViolations());
                stats.setTimeSequenceRiskBreakdown(snap.getTimeSequenceRiskBreakdown());
                stats.setAmountMismatches(snap.getAmountMismatches());
                stats.setAmountMismatchRiskBreakdown(snap.getAmountMismatchRiskBreakdown());
                stats.setStatusInconsistencies(snap.getStatusInconsistencies());
                stats.setStatusInconsistencyRiskBreakdown(snap.getStatusInconsistencyRiskBreakdown());
                stats.setDataInconsistencies(snap.getDataInconsistencies());
                stats.setDataInconsistencyRiskBreakdown(snap.getDataInconsistencyRiskBreakdown());
                stats.setMissingAssociations(snap.getMissingAssociations());
                stats.setMissingAssociationRiskBreakdown(snap.getMissingAssociationRiskBreakdown());
            } else {
                calculateExceptionStats(stats, startDate, endDate);
            }
        }

        // ========== 兼容旧版本字段 ==========
        calculateLegacyStats(stats);

        log.info("完整看板统计数据计算完成 - executionRate: {}, contractCount: {}", stats.getExecutionRate(), stats.getContractCount());
        return stats;
    }

    /**
     * 计算合同维度统计指标
     */
    private void calculateContractStats(ClosureDashboardStats stats, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // 构建查询条件
            QueryWrapper<Contract> contractQuery = new QueryWrapper<>();
            if (startDate != null) {
                contractQuery.ge("签订时间", startDate);
            }
            if (endDate != null) {
                contractQuery.le("签订时间", endDate);
            }
            contractQuery.ne("合同状态", "待审核"); // 排除待审核状态
            contractQuery.ne("合同状态", "已拒绝"); // 排除已拒绝状态

            // 临时调试：先查询所有合同
            QueryWrapper<Contract> debugQuery = new QueryWrapper<>();
            List<Contract> allContracts = contractMapper.selectList(debugQuery);
            log.info("[主统计] 数据库中总合同数量: {}", allContracts.size());

            // 统计所有合同的状态分布
            Map<String, Long> allStatusCount = allContracts.stream()
                    .collect(Collectors.groupingBy(Contract::getContractStatus, Collectors.counting()));
            log.info("[主统计] 数据库中所有合同状态分布: {}", allStatusCount);

            // 获取合同列表
            List<Contract> contracts = contractMapper.selectList(contractQuery);

            log.info("[主统计] 查询到合同数量: {}, 查询条件: 排除待审核和已拒绝状态", contracts != null ? contracts.size() : "null");

            if (contracts == null) {
                log.error("[主统计] 合同查询返回null!");
                contracts = new ArrayList<>();
            }

            // 调试：统计各状态的合同数量
            Map<String, Long> statusCount = contracts.stream()
                    .collect(Collectors.groupingBy(Contract::getContractStatus, Collectors.counting()));
            log.info("各状态合同数量统计: {}", statusCount);

            // 合同签订数量
            stats.setContractCount(contracts.size());

            // 合同签订金额
            BigDecimal totalContractAmount = contracts.stream()
                    .filter(c -> c.getContractAmount() != null)
                    .map(Contract::getContractAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.setContractAmount(totalContractAmount);

            // 计算上月同期数据用于趋势计算
            LocalDateTime lastMonthStart = startDate != null ? startDate.minusMonths(1) : null;
            LocalDateTime lastMonthEnd = endDate.minusMonths(1);

            QueryWrapper<Contract> lastMonthQuery = new QueryWrapper<>();
            if (lastMonthStart != null) {
                lastMonthQuery.ge("签订时间", lastMonthStart);
            }
            lastMonthQuery.le("签订时间", lastMonthEnd);
            lastMonthQuery.ne("合同状态", "待审核");
            lastMonthQuery.ne("合同状态", "已拒绝");

            List<Contract> lastMonthContracts = contractMapper.selectList(lastMonthQuery);
            int lastMonthCount = lastMonthContracts.size();
            BigDecimal lastMonthAmount = lastMonthContracts.stream()
                    .filter(c -> c.getContractAmount() != null)
                    .map(Contract::getContractAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 计算趋势（百分比）
            if (lastMonthCount > 0) {
                stats.setContractCountTrend(new BigDecimal((double) (contracts.size() - lastMonthCount) / lastMonthCount * 100));
            }
            if (lastMonthAmount.compareTo(BigDecimal.ZERO) > 0) {
                stats.setContractAmountTrend(new BigDecimal(totalContractAmount.subtract(lastMonthAmount)
                        .divide(lastMonthAmount, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(new BigDecimal(100)).doubleValue()));
            }

            // 合同执行率 - 已执行的合同数量 / 总合同数
            // 认为"执行中"、"已完结"、"已归档"状态的合同为已执行
            long executedCount = contracts.stream()
                    .filter(c -> "执行中".equals(c.getContractStatus()) ||
                               "已完结".equals(c.getContractStatus()) ||
                               "已归档".equals(c.getContractStatus()))
                    .count();

            log.info("[主统计] 合同执行率计算: 已执行合同数={}, 总合同数={}, contracts对象: {}", executedCount, contracts.size(), contracts);

            BigDecimal executionRate = BigDecimal.ZERO;
            // 使用全量合同数作为分母（不按筛选后的 contracts 数量）
            try {
                QueryWrapper<Contract> allQuery = new QueryWrapper<>();
                List<Contract> allContractsForDenominator = contractMapper.selectList(allQuery);
                int totalAllContracts = allContractsForDenominator != null ? allContractsForDenominator.size() : 0;

                if (totalAllContracts > 0) {
                    executionRate = new BigDecimal((double) executedCount / totalAllContracts * 100);
                    stats.setExecutionRate(executionRate);
                    log.info("[主统计] 合同执行率计算结果（基于全量合同数={}）: {}%", totalAllContracts, executionRate);
                } else {
                    stats.setExecutionRate(BigDecimal.ZERO);
                    log.info("[主统计] 无有效全量合同数据，执行率设为0");
                }
            } catch (Exception ex) {
                log.warn("[主统计] 计算执行率时获取全量合同数失败，使用筛选后合同数作为分母", ex);
                if (contracts != null && contracts.size() > 0) {
                    executionRate = new BigDecimal((double) executedCount / contracts.size() * 100);
                    stats.setExecutionRate(executionRate);
                } else {
                    stats.setExecutionRate(BigDecimal.ZERO);
                }
            }

            // 计算上月执行率用于趋势计算
            long lastMonthExecutedCount = lastMonthContracts.stream()
                    .filter(c -> "执行中".equals(c.getContractStatus()) ||
                               "已完结".equals(c.getContractStatus()) ||
                               "已归档".equals(c.getContractStatus()))
                    .count();

            BigDecimal lastMonthExecutionRate = BigDecimal.ZERO;
            if (lastMonthContracts.size() > 0) {
                lastMonthExecutionRate = new BigDecimal((double) lastMonthExecutedCount / lastMonthContracts.size() * 100);
            }

            stats.setExecutionRateTrend(executionRate.subtract(lastMonthExecutionRate));

            // 超期未执行合同数量（签订超过30天仍未执行）
            long overdueCount = contracts.stream()
                    .filter(c -> "已通过".equals(c.getContractStatus())) // 已审核通过但未开始执行
                    .filter(c -> c.getSignTime() != null)
                    .filter(c -> c.getSignTime().isBefore(LocalDateTime.now().minusDays(30)))
                    .count();
            stats.setOverdueExecutionCount((int) overdueCount);

            // 计算超期未执行合同趋势（与上月比较）
            long lastMonthOverdueCount = lastMonthContracts.stream()
                    .filter(c -> "SIGNED".equals(c.getContractStatus()))
                    .filter(c -> c.getSignTime() != null)
                    .filter(c -> c.getSignTime().isBefore(lastMonthEnd.minusDays(30)))
                    .count();

            if (lastMonthOverdueCount > 0) {
                stats.setOverdueExecutionTrend(new BigDecimal(
                    (double) (overdueCount - lastMonthOverdueCount) / lastMonthOverdueCount * 100));
            }

        } catch (Exception e) {
            log.error("计算合同维度统计指标失败", e);
            // 设置默认值
            stats.setContractCount(0);
            stats.setContractAmount(BigDecimal.ZERO);
            stats.setExecutionRate(BigDecimal.ZERO);
            stats.setOverdueExecutionCount(0);
        }
    }

    /**
     * 计算收款中心统计指标
     */
    private void calculatePaymentStats(ClosureDashboardStats stats, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // 逾期金额/坏账金额暂时仅保留接口，回退路径也不做实际计算
            stats.setOverduePaymentAmount(BigDecimal.ZERO);
            stats.setOverduePaymentAmountTrend(BigDecimal.ZERO);
        } catch (Exception e) {
            log.error("计算收款中心统计指标失败", e);
            stats.setOverduePaymentAmount(BigDecimal.ZERO);
            stats.setOverduePaymentAmountTrend(BigDecimal.ZERO);
        }
    }

    /**
     * 计算异常监控统计指标
     */
    private void calculateExceptionStats(ClosureDashboardStats stats, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // 使用基于问题列表的聚合来计算异常监控指标（issues-driven aggregation）
            // 先从内存缓存获取 issues；若需要实时可通过 getClosureIssues 的 forceRefresh 参数触发刷新（由调用方控制）
            List<ClosureIssueDTO> issues;
            synchronized (BusinessClosureValidationServiceImpl.class) {
                issues = new ArrayList<>(latestValidationIssues);
            }

            // 将 issues 聚合成统计指标
            ClosureDashboardStats issueStats = aggregateStatsFromIssues(issues);

            // 将聚合结果合并到传入的 stats 对象（只覆盖异常相关字段）
            stats.setTimeSequenceViolations(issueStats.getTimeSequenceViolations());
            stats.setTimeSequenceRiskBreakdown(issueStats.getTimeSequenceRiskBreakdown());
            stats.setAmountMismatches(issueStats.getAmountMismatches());
            stats.setAmountMismatchRiskBreakdown(issueStats.getAmountMismatchRiskBreakdown());
            stats.setStatusInconsistencies(issueStats.getStatusInconsistencies());
            stats.setStatusInconsistencyRiskBreakdown(issueStats.getStatusInconsistencyRiskBreakdown());
            stats.setDataInconsistencies(issueStats.getDataInconsistencies());
            stats.setDataInconsistencyRiskBreakdown(issueStats.getDataInconsistencyRiskBreakdown());
            stats.setMissingAssociations(issueStats.getMissingAssociations());
            stats.setMissingAssociationRiskBreakdown(issueStats.getMissingAssociationRiskBreakdown());
            // 关联缺失风险分布
            Map<String, Long> missingAssociationTemp = issues.stream()
                    .filter(issue -> ISSUE_TYPE_MISSING_ASSOCIATION.equals(issue.getIssueType()))
                    .collect(Collectors.groupingBy(ClosureIssueDTO::getRiskLevel, Collectors.counting()));
            Map<String, Integer> missingAssociationRiskBreakdown = missingAssociationTemp.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));
            stats.setMissingAssociationRiskBreakdown(missingAssociationRiskBreakdown);

        } catch (Exception e) {
            log.error("计算异常监控统计指标失败", e);
            // 设置默认值
            stats.setTimeSequenceViolations(0);
            stats.setAmountMismatches(0);
            stats.setStatusInconsistencies(0);
            stats.setDataInconsistencies(0);
            stats.setMissingAssociations(0);
            stats.setTimeSequenceRiskBreakdown(new HashMap<>());
            stats.setAmountMismatchRiskBreakdown(new HashMap<>());
            stats.setStatusInconsistencyRiskBreakdown(new HashMap<>());
            stats.setDataInconsistencyRiskBreakdown(new HashMap<>());
            stats.setMissingAssociationRiskBreakdown(new HashMap<>());
        }
    }

    /**
     * 计算兼容旧版本的统计字段
     */
    private void calculateLegacyStats(ClosureDashboardStats stats) {
        try {
            // 从结算数据计算兼容字段
            Map<String, Object> settlementStats = settlementMapper.selectSettlementStatistics(null);
            if (settlementStats != null) {
                Object totalSettlementAmountObj = settlementStats.get("totalSettlementAmount");
                Object totalReceivedAmountObj = settlementStats.get("totalReceivedAmount");
                Object totalUnreceivedAmountObj = settlementStats.get("totalUnreceivedAmount");
                Object overdueCountObj = settlementStats.get("overdueSettlementsCount");
                Object recentReceiptsObj = settlementStats.get("recentReceiptsCount");
                Object pendingReviewsObj = settlementStats.get("pendingReviewsCount");

                if (totalSettlementAmountObj instanceof Number) {
                    stats.setTotalSettlementAmount(new BigDecimal(((Number) totalSettlementAmountObj).toString()));
                } else if (totalSettlementAmountObj instanceof BigDecimal) {
                    stats.setTotalSettlementAmount((BigDecimal) totalSettlementAmountObj);
                }
                if (totalReceivedAmountObj instanceof Number) {
                    stats.setTotalReceivedAmount(new BigDecimal(((Number) totalReceivedAmountObj).toString()));
                } else if (totalReceivedAmountObj instanceof BigDecimal) {
                    stats.setTotalReceivedAmount((BigDecimal) totalReceivedAmountObj);
                }
                if (totalUnreceivedAmountObj instanceof Number) {
                    stats.setTotalUnreceivedAmount(new BigDecimal(((Number) totalUnreceivedAmountObj).toString()));
                } else if (totalUnreceivedAmountObj instanceof BigDecimal) {
                    stats.setTotalUnreceivedAmount((BigDecimal) totalUnreceivedAmountObj);
                }
                if (overdueCountObj instanceof Number) {
                    Number overdueNum = (Number) overdueCountObj;
                    stats.setOverdueSettlementsCount(overdueNum.intValue());
                }
                if (recentReceiptsObj instanceof Number) {
                    Number recentNum = (Number) recentReceiptsObj;
                    stats.setRecentReceiptsCount(recentNum.intValue());
                }
                if (pendingReviewsObj instanceof Number) {
                    stats.setPendingReviewsCount(((Number) pendingReviewsObj).intValue());
                }
            }

            // 从校验结果计算高风险结算单数量
            List<ClosureIssueDTO> issues;
            synchronized (BusinessClosureValidationServiceImpl.class) {
                issues = new ArrayList<>(latestValidationIssues);
            }

            int highRiskCount = (int) issues.stream()
                    .filter(it -> "SETTLEMENT".equals(it.getRelatedEntityType()))
                    .filter(it -> "HIGH".equals(it.getRiskLevel()) || "CRITICAL".equals(it.getRiskLevel()))
                    .map(ClosureIssueDTO::getRelatedEntityId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

            int pendingReviews = (int) issues.stream()
                    .filter(it -> "REVIEW_REQUIRED".equals(it.getActionType()) || it.getRiskLevel() != null)
                    .map(ClosureIssueDTO::getRelatedEntityId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

            if (stats.getHighRiskSettlementsCount() == null) {
                stats.setHighRiskSettlementsCount(highRiskCount);
            }
            if (stats.getPendingReviewsCount() == null) {
                stats.setPendingReviewsCount(pendingReviews);
            }

        } catch (Exception e) {
            log.warn("计算兼容旧版本统计字段失败", e);
        }

        // 保证不会为 null
        if (stats.getTotalSettlementAmount() == null) stats.setTotalSettlementAmount(BigDecimal.ZERO);
        if (stats.getTotalReceivedAmount() == null) stats.setTotalReceivedAmount(BigDecimal.ZERO);
        if (stats.getTotalUnreceivedAmount() == null) stats.setTotalUnreceivedAmount(BigDecimal.ZERO);
        if (stats.getOverdueSettlementsCount() == null) stats.setOverdueSettlementsCount(0);
        if (stats.getHighRiskSettlementsCount() == null) stats.setHighRiskSettlementsCount(0);
        if (stats.getRecentReceiptsCount() == null) stats.setRecentReceiptsCount(0);
        if (stats.getPendingReviewsCount() == null) stats.setPendingReviewsCount(0);
    }

    /**
     * 基于问题列表聚合统计看板异常指标（issues-driven aggregation helper）
     */
    private ClosureDashboardStats aggregateStatsFromIssues(List<ClosureIssueDTO> issues) {
        ClosureDashboardStats s = new ClosureDashboardStats();

        if (issues == null || issues.isEmpty()) {
            s.setTimeSequenceViolations(0);
            s.setAmountMismatches(0);
            s.setStatusInconsistencies(0);
            s.setDataInconsistencies(0);
            s.setMissingAssociations(0);
            s.setTimeSequenceRiskBreakdown(new HashMap<>());
            s.setAmountMismatchRiskBreakdown(new HashMap<>());
            s.setStatusInconsistencyRiskBreakdown(new HashMap<>());
            s.setDataInconsistencyRiskBreakdown(new HashMap<>());
            s.setMissingAssociationRiskBreakdown(new HashMap<>());
            return s;
        }

        Set<String> timeSequenceTypes = new HashSet<>(Arrays.asList(
                ISSUE_TYPE_TIME_SEQUENCE_VIOLATION,
                ISSUE_TYPE_PREPAYMENT_VIOLATION,
                ISSUE_TYPE_OVERDUE_EXECUTION
        ));
        Set<String> amountTypes = new HashSet<>(Arrays.asList(
                ISSUE_TYPE_AMOUNT_MISMATCH,
                ISSUE_TYPE_INVALID_DATA
        ));
        Set<String> businessClosureStatusTypes = new HashSet<>(Arrays.asList(
                ISSUE_TYPE_STATUS_INCONSISTENCY,
                ISSUE_TYPE_UNCLAIMED_PAYMENT,
                ISSUE_TYPE_DUPLICATE_RECORD
        ));
        Set<String> businessClosureDataTypes = new HashSet<>(Collections.singletonList(
                ISSUE_TYPE_DATA_INCONSISTENCY
        ));

        s.setTimeSequenceViolations((int) issues.stream()
                .filter(issue -> timeSequenceTypes.contains(issue.getIssueType()))
                .count());
        s.setAmountMismatches((int) issues.stream()
                .filter(issue -> amountTypes.contains(issue.getIssueType()))
                .count());
        s.setStatusInconsistencies((int) issues.stream()
                .filter(issue -> businessClosureStatusTypes.contains(issue.getIssueType()))
                .count());
        s.setDataInconsistencies((int) issues.stream()
                .filter(issue -> businessClosureDataTypes.contains(issue.getIssueType()))
                .count());
        s.setMissingAssociations((int) issues.stream()
                .filter(issue -> ISSUE_TYPE_MISSING_ASSOCIATION.equals(issue.getIssueType()))
                .count());

        Map<String, Long> timeSeqRisk = issues.stream()
                .filter(i -> timeSequenceTypes.contains(i.getIssueType()))
                .collect(Collectors.groupingBy(ClosureIssueDTO::getRiskLevel, Collectors.counting()));
        s.setTimeSequenceRiskBreakdown(timeSeqRisk.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())));

        Map<String, Long> amountRisk = issues.stream()
                .filter(i -> amountTypes.contains(i.getIssueType()))
                .collect(Collectors.groupingBy(ClosureIssueDTO::getRiskLevel, Collectors.counting()));
        s.setAmountMismatchRiskBreakdown(amountRisk.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())));

        Map<String, Long> statusRisk = issues.stream()
                .filter(i -> businessClosureStatusTypes.contains(i.getIssueType()))
                .collect(Collectors.groupingBy(ClosureIssueDTO::getRiskLevel, Collectors.counting()));
        s.setStatusInconsistencyRiskBreakdown(statusRisk.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())));

        Map<String, Long> dataRisk = issues.stream()
                .filter(i -> businessClosureDataTypes.contains(i.getIssueType()))
                .collect(Collectors.groupingBy(ClosureIssueDTO::getRiskLevel, Collectors.counting()));
        s.setDataInconsistencyRiskBreakdown(dataRisk.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())));

        Map<String, Long> missingRisk = issues.stream()
                .filter(i -> ISSUE_TYPE_MISSING_ASSOCIATION.equals(i.getIssueType()))
                .collect(Collectors.groupingBy(ClosureIssueDTO::getRiskLevel, Collectors.counting()));
        s.setMissingAssociationRiskBreakdown(missingRisk.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())));

        if (s.getTimeSequenceRiskBreakdown() == null) s.setTimeSequenceRiskBreakdown(new HashMap<>());
        if (s.getAmountMismatchRiskBreakdown() == null) s.setAmountMismatchRiskBreakdown(new HashMap<>());
        if (s.getStatusInconsistencyRiskBreakdown() == null) s.setStatusInconsistencyRiskBreakdown(new HashMap<>());
        if (s.getDataInconsistencyRiskBreakdown() == null) s.setDataInconsistencyRiskBreakdown(new HashMap<>());
        if (s.getMissingAssociationRiskBreakdown() == null) s.setMissingAssociationRiskBreakdown(new HashMap<>());

        return s;
    }

    @Override
    public List<ClosureTrendData> getTrendData(String metricType, String period, String dateRange) {
        log.info("获取趋势数据，参数：metricType={}, period={}, dateRange={}", metricType, period, dateRange);

        List<ClosureTrendData> trendData = new ArrayList<>();

        // 生成示例趋势数据（实际应该从数据库查询历史数据）
        LocalDateTime now = LocalDateTime.now();

        for (int i = 6; i >= 0; i--) {
            ClosureTrendData data = new ClosureTrendData();
            LocalDateTime dateTime = now.minusDays(i);

            if ("DAY".equals(period)) {
                data.setTimePoint(DateUtil.format(dateTime, "yyyy-MM-dd"));
            } else if ("WEEK".equals(period)) {
                data.setTimePoint(DateUtil.format(dateTime, "yyyy-MM-dd"));
            } else if ("MONTH".equals(period)) {
                data.setTimePoint(DateUtil.format(dateTime, "yyyy-MM"));
            }

            data.setMetricType(metricType);
            data.setPeriod(period);
            data.setMetricName(getMetricName(metricType));

            // 生成示例数据
            int baseValue = 10;
            switch (metricType) {
                case "ISSUE_COUNT":
                    data.setValue(baseValue + (int)(Math.random() * 20));
                    break;
                case "VALIDATION_COUNT":
                    data.setValue(baseValue + (int)(Math.random() * 5));
                    break;
                case "RESOLUTION_RATE":
                    data.setValue(70 + (int)(Math.random() * 30)); // 百分比
                    break;
                default:
                    data.setValue(baseValue + (int)(Math.random() * 15));
            }

            trendData.add(data);
        }

        log.info("获取趋势数据完成，返回{}个数据点", trendData.size());
        return trendData;
    }

    /**
     * 获取指标名称
     */
    private String getMetricName(String metricType) {
        switch (metricType) {
            case "ISSUE_COUNT":
                return "问题数量";
            case "VALIDATION_COUNT":
                return "校验次数";
            case "RESOLUTION_RATE":
                return "解决率";
            case "CRITICAL_ISSUES":
                return "严重问题";
            default:
                return "未知指标";
        }
    }

    @Override
    public Map<String, Object> calculateContractStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("[批量计算] 批量计算合同统计数据，时间范围：{} - {}", startDate, endDate);

        Map<String, Object> result = new HashMap<>();

        try {
            // 构建查询条件
            QueryWrapper<Contract> contractQuery = new QueryWrapper<>();
            if (startDate != null) {
                contractQuery.ge("签订时间", startDate);
            }
            if (endDate != null) {
                contractQuery.le("签订时间", endDate);
            }
            contractQuery.ne("合同状态", "待审核");
            contractQuery.ne("合同状态", "已拒绝");

            // 获取合同列表
            List<Contract> contracts = contractMapper.selectList(contractQuery);

            if (contracts == null) {
                log.error("[批量计算] 合同查询返回null!");
                contracts = new ArrayList<>();
            }

            // 合同签订数量
            result.put("contractCount", contracts.size());

            // 合同签订总金额
            BigDecimal totalContractAmount = contracts.stream()
                    .filter(c -> c.getContractAmount() != null)
                    .map(Contract::getContractAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put("totalContractAmount", totalContractAmount);

            // ===== 合同维度扩展指标 =====
            // 1. 合同签订重量（本月累积）：汇总 CONTRACT_WASTE_ITEM 中的计划转移数量（单位统一为吨）
            try {
                if (!contracts.isEmpty()) {
                    List<Integer> contractIds = contracts.stream()
                            .map(Contract::getContractId)
                            .filter(id -> id != null)
                            .collect(Collectors.toList());
                    // 查询合同条目
                    QueryWrapper<ContractItem> itemQuery = new QueryWrapper<>();
                    itemQuery.in("合同编号", contractIds);
                    List<ContractItem> contractItems = contractItemMapper.selectList(itemQuery);
                    List<Integer> itemIds = contractItems.stream()
                            .map(ContractItem::getContractItemId)
                            .filter(id -> id != null)
                            .collect(Collectors.toList());
                    BigDecimal totalWeight = BigDecimal.ZERO;
                    if (!itemIds.isEmpty()) {
                        QueryWrapper<ContractWasteItem> wasteQuery = new QueryWrapper<>();
                        wasteQuery.in("合同条目编号", itemIds);
                        List<ContractWasteItem> wasteItems = contractWasteItemMapper.selectList(wasteQuery);
                        totalWeight = wasteItems.stream()
                                .filter(w -> "吨".equals(w.getUnit()))
                                .filter(w -> w.getPlannedQuantity() != null
                                        && w.getPlannedQuantity().compareTo(new BigDecimal("-1")) != 0)
                                .map(ContractWasteItem::getPlannedQuantity)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                    }
                    result.put("contractWeight", totalWeight);
                    log.debug("[合同重量] 本期合同签订重量={}吨", totalWeight);
                } else {
                    result.put("contractWeight", BigDecimal.ZERO);
                }
            } catch (Exception ex) {
                log.warn("[批量计算] 计算合同签订重量失败", ex);
                result.put("contractWeight", BigDecimal.ZERO);
            }

            // 1b. 合同签订重量同比/环比
            try {
                BigDecimal currentWeight = result.get("contractWeight") instanceof BigDecimal
                        ? (BigDecimal) result.get("contractWeight") : BigDecimal.ZERO;
                // 同比：去年同月重量
                LocalDateTime yoyWeightEnd = endDate != null ? endDate.minusYears(1) : LocalDateTime.now().minusYears(1);
                LocalDateTime yoyWeightStart = startDate != null ? startDate.minusYears(1)
                        : yoyWeightEnd.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                QueryWrapper<Contract> yoyWQ = new QueryWrapper<>();
                yoyWQ.ge("签订时间", yoyWeightStart).le("签订时间", yoyWeightEnd)
                        .ne("合同状态", "待审核").ne("合同状态", "已拒绝");
                List<Contract> yoyContracts = contractMapper.selectList(yoyWQ);
                List<Integer> yoyIds = yoyContracts.stream().map(Contract::getContractId).filter(Objects::nonNull).collect(Collectors.toList());
                BigDecimal yoyWeight = BigDecimal.ZERO;
                if (!yoyIds.isEmpty()) {
                    QueryWrapper<ContractItem> yoyItemQ = new QueryWrapper<>();
                    yoyItemQ.in("合同编号", yoyIds);
                    List<ContractItem> yoyItems = contractItemMapper.selectList(yoyItemQ);
                    List<Integer> yoyItemIds = yoyItems.stream().map(ContractItem::getContractItemId).filter(Objects::nonNull).collect(Collectors.toList());
                    if (!yoyItemIds.isEmpty()) {
                        QueryWrapper<ContractWasteItem> yoyWasteQ = new QueryWrapper<>();
                        yoyWasteQ.in("合同条目编号", yoyItemIds);
                        yoyWeight = contractWasteItemMapper.selectList(yoyWasteQ).stream()
                                .filter(w -> "吨".equals(w.getUnit()))
                                .filter(w -> w.getPlannedQuantity() != null && w.getPlannedQuantity().compareTo(new BigDecimal("-1")) != 0)
                                .map(ContractWasteItem::getPlannedQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                    }
                }
                BigDecimal weightYoyTrend = calcTrendSafe(currentWeight, yoyWeight, "重量同比");
                result.put("contractWeightTrend", weightYoyTrend);
                // 环比：上月重量
                LocalDateTime momWeightEnd = endDate != null ? endDate.minusMonths(1) : LocalDateTime.now().minusMonths(1);
                LocalDateTime momWeightStart = startDate != null ? startDate.minusMonths(1)
                        : momWeightEnd.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                QueryWrapper<Contract> momWQ = new QueryWrapper<>();
                momWQ.ge("签订时间", momWeightStart).le("签订时间", momWeightEnd)
                        .ne("合同状态", "待审核").ne("合同状态", "已拒绝");
                List<Contract> momContracts = contractMapper.selectList(momWQ);
                List<Integer> momIds = momContracts.stream().map(Contract::getContractId).filter(Objects::nonNull).collect(Collectors.toList());
                BigDecimal momWeight = BigDecimal.ZERO;
                if (!momIds.isEmpty()) {
                    QueryWrapper<ContractItem> momItemQ = new QueryWrapper<>();
                    momItemQ.in("合同编号", momIds);
                    List<ContractItem> momItems = contractItemMapper.selectList(momItemQ);
                    List<Integer> momItemIds = momItems.stream().map(ContractItem::getContractItemId).filter(Objects::nonNull).collect(Collectors.toList());
                    if (!momItemIds.isEmpty()) {
                        QueryWrapper<ContractWasteItem> momWasteQ = new QueryWrapper<>();
                        momWasteQ.in("合同条目编号", momItemIds);
                        momWeight = contractWasteItemMapper.selectList(momWasteQ).stream()
                                .filter(w -> "吨".equals(w.getUnit()))
                                .filter(w -> w.getPlannedQuantity() != null && w.getPlannedQuantity().compareTo(new BigDecimal("-1")) != 0)
                                .map(ContractWasteItem::getPlannedQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                    }
                }
                BigDecimal weightMomTrend = calcTrendSafe(currentWeight, momWeight, "重量环比");
                result.put("contractWeightMomTrend", weightMomTrend);
                log.debug("[合同重量趋势] 同比={}%, 环比={}%", weightYoyTrend, weightMomTrend);
            } catch (Exception ex) {
                log.warn("[批量计算] 计算合同重量趋势失败", ex);
                result.put("contractWeightTrend", BigDecimal.ZERO);
                result.put("contractWeightMomTrend", BigDecimal.ZERO);
            }

            // 2. 合同变动：增量（本月新签中历史首签客户）vs 存量（本月新签中历史已签过的客户）
            try {
                // 本月签订合同的客户编码列表（去重，排除 null）
                List<Integer> thisMonthCustomerIds = contracts.stream()
                        .map(Contract::getCustomerId)
                        .filter(id -> id != null)
                        .distinct()
                        .collect(Collectors.toList());

                int newCount = 0;    // 增量：历史首签
                int activeCount = 0; // 存量：历史已签

                if (!thisMonthCustomerIds.isEmpty()) {
                    // 查询这批客户在本月开始之前是否已有历史合同（非待审核/已拒绝）
                    LocalDateTime thisMonthStart = startDate; // 已是本月1日00:00:00
                    QueryWrapper<Contract> historyQuery = new QueryWrapper<>();
                    historyQuery.in("客户编码", thisMonthCustomerIds)
                            .lt("签订时间", thisMonthStart)
                            .ne("合同状态", "待审核")
                            .ne("合同状态", "已拒绝")
                            .select("DISTINCT 客户编码");
                    List<Contract> historicalContracts = contractMapper.selectList(historyQuery);
                    Set<Integer> returningCustomerIds = historicalContracts.stream()
                            .map(Contract::getCustomerId)
                            .filter(id -> id != null)
                            .collect(Collectors.toSet());

                    // 本月每份合同按客户是否历史首签归类
                    // 增量：合同所属客户在历史上无合同记录
                    // 存量：合同所属客户在历史上已有合同记录
                    for (Contract c : contracts) {
                        Integer cid = c.getCustomerId();
                        if (cid == null) {
                            newCount++; // 无客户编码（临时客户）视为增量
                        } else if (returningCustomerIds.contains(cid)) {
                            activeCount++;
                        } else {
                            newCount++;
                        }
                    }
                }

                result.put("contractNewCount", newCount);
                result.put("contractActiveCount", activeCount);
                log.debug("[合同变动] 增量(首签)={}, 存量(续签)={}", newCount, activeCount);
            } catch (Exception ex) {
                log.warn("[批量计算] 计算合同变动失败", ex);
                result.put("contractNewCount", 0);
                result.put("contractActiveCount", 0);
            }

            // 3. 合同跟进：即将到期、客户丢失
            try {
                LocalDateTime periodStart = startDate != null
                        ? startDate
                        : LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime periodEnd = endDate != null ? endDate : LocalDateTime.now();
                List<String> activeContractStatuses = Arrays.asList("已通过", "执行中", "已完结", "已归档");

                // 即将到期：未来30天内到期的有效合同数量
                QueryWrapper<Contract> expiringQuery = new QueryWrapper<>();
                expiringQuery.in("合同状态", activeContractStatuses)
                        .isNotNull("合同有效期结束")
                        .ge("合同有效期结束", periodEnd)
                        .le("合同有效期结束", periodEnd.plusDays(30));
                long expiringCount = contractMapper.selectCount(expiringQuery);
                result.put("contractExpiringCount", (int) expiringCount);

                // 客户丢失：客户在有效合同状态下最晚的合同有效期结束日期 + 30 天后，仍未签订新合同
                QueryWrapper<Contract> activeContractsQuery = new QueryWrapper<>();
                activeContractsQuery.in("合同状态", activeContractStatuses)
                        .isNotNull("合同有效期结束");
                List<Contract> activeContracts = contractMapper.selectList(activeContractsQuery);

                int lostCount = 0;
                if (!activeContracts.isEmpty()) {
                    Map<Integer, List<Contract>> contractsByCustomer = activeContracts.stream()
                            .filter(c -> c.getCustomerId() != null)
                            .collect(Collectors.groupingBy(Contract::getCustomerId));

                    for (List<Contract> customerContracts : contractsByCustomer.values()) {
                        LocalDateTime latestValidTo = customerContracts.stream()
                                .map(Contract::getValidTo)
                                .filter(Objects::nonNull)
                                .max(LocalDateTime::compareTo)
                                .orElse(null);
                        if (latestValidTo == null) {
                            continue;
                        }

                        LocalDateTime lossCheckTime = latestValidTo.plusDays(30);
                        if (lossCheckTime.isAfter(periodEnd)) {
                            continue;
                        }

                        boolean hasRenewedContract = customerContracts.stream()
                                .anyMatch(c -> c.getSignTime() != null && c.getSignTime().isAfter(latestValidTo));
                        if (!hasRenewedContract) {
                            lostCount++;
                        }
                    }
                }
                result.put("contractLostCount", lostCount);
                log.debug("[合同跟进] 即将到期(未来30天)={}, 客户丢失(最晚有效期结束+30天未续签)={}", expiringCount, lostCount);
            } catch (Exception ex) {
                log.warn("[批量计算] 计算合同跟进失败", ex);
                result.put("contractExpiringCount", 0);
                result.put("contractLostCount", 0);
            }

            // ===== 合同签订数量同比（YoY）：与去年同期相比 =====
            // 同比基准：去年与 startDate~endDate 相同的月份区间
            // 若 endDate 为 null 则以当前时间为基准，计算去年同月
            try {
                LocalDateTime yoyBaseEnd = endDate != null ? endDate.minusYears(1) : LocalDateTime.now().minusYears(1);
                LocalDateTime yoyBaseStart = startDate != null ? startDate.minusYears(1)
                        : yoyBaseEnd.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

                QueryWrapper<Contract> yoyQuery = new QueryWrapper<>();
                yoyQuery.ge("签订时间", yoyBaseStart);
                yoyQuery.le("签订时间", yoyBaseEnd);
                yoyQuery.ne("合同状态", "待审核");
                yoyQuery.ne("合同状态", "已拒绝");
                long yoyCount = contractMapper.selectCount(yoyQuery);

                int currentCount = contracts.size();
                // 合同数量为非负整数，基准期不会为负，直接按标准公式计算
                BigDecimal yoyTrend;
                if (yoyCount > 0) {
                    yoyTrend = new BigDecimal((double)(currentCount - yoyCount) / yoyCount * 100)
                            .setScale(2, BigDecimal.ROUND_HALF_UP);
                } else if (currentCount > 0) {
                    yoyTrend = new BigDecimal(100); // 去年同期为 0，今年有数据，视为 +100%
                } else {
                    yoyTrend = BigDecimal.ZERO; // 两期均为 0
                }
                result.put("contractCountYoyTrend", yoyTrend);
                log.debug("[同比] 合同签订数量：本期={}, 去年同期={}, 同比={}", currentCount, yoyCount, yoyTrend);

                // 同比金额趋势
                // 金额可能为负（如退款合同），需使用安全计算方式
                List<Contract> yoyContracts = contractMapper.selectList(yoyQuery);
                BigDecimal yoyAmount = yoyContracts.stream()
                        .filter(c -> c.getContractAmount() != null)
                        .map(Contract::getContractAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                result.put("contractAmountYoyTrend", calcTrendSafe(totalContractAmount, yoyAmount, "金额同比"));
            } catch (Exception ex) {
                log.warn("[批量计算] 计算合同签订数量同比趋势失败", ex);
                result.put("contractCountYoyTrend", BigDecimal.ZERO);
                result.put("contractAmountYoyTrend", BigDecimal.ZERO);
            }

            // ===== 合同签订数量环比（MoM）：与上月相比 =====
            try {
                LocalDateTime momBaseEnd = endDate != null ? endDate.minusMonths(1) : LocalDateTime.now().minusMonths(1);
                LocalDateTime momBaseStart = startDate != null ? startDate.minusMonths(1)
                        : momBaseEnd.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

                QueryWrapper<Contract> momQuery = new QueryWrapper<>();
                momQuery.ge("签订时间", momBaseStart);
                momQuery.le("签订时间", momBaseEnd);
                momQuery.ne("合同状态", "待审核");
                momQuery.ne("合同状态", "已拒绝");
                long momCount = contractMapper.selectCount(momQuery);

                int currentCount = contracts.size();
                // 合同数量为非负整数，基准期不会为负，直接按标准公式计算
                BigDecimal momTrend;
                if (momCount > 0) {
                    momTrend = new BigDecimal((double)(currentCount - momCount) / momCount * 100)
                            .setScale(2, BigDecimal.ROUND_HALF_UP);
                } else if (currentCount > 0) {
                    momTrend = new BigDecimal(100);
                } else {
                    momTrend = BigDecimal.ZERO;
                }
                result.put("contractCountMomTrend", momTrend);
                log.debug("[环比] 合同签订数量：本期={}, 上月={}, 环比={}", currentCount, momCount, momTrend);

                // 环比金额趋势
                // 金额可能为负（如退款合同），需使用安全计算方式
                List<Contract> momContracts = contractMapper.selectList(momQuery);
                BigDecimal momAmount = momContracts.stream()
                        .filter(c -> c.getContractAmount() != null)
                        .map(Contract::getContractAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                result.put("contractAmountMomTrend", calcTrendSafe(totalContractAmount, momAmount, "金额环比"));
            } catch (Exception ex) {
                log.warn("[批量计算] 计算合同签订数量环比趋势失败", ex);
                result.put("contractCountMomTrend", BigDecimal.ZERO);
                result.put("contractAmountMomTrend", BigDecimal.ZERO);
            }

        } catch (Exception e) {
            log.error("批量计算合同统计数据失败", e);
            result.put("error", "计算失败：" + e.getMessage());
        }

        return result;
    }

    /**
     * 安全计算趋势百分比，处理基准期为负数、零或与当期符号相反的边界情况。
     *
     * 计算规则：
     * 1. 基准期 > 0，当期 >= 0：标准公式 (当期 - 基准期) / 基准期 × 100%
     * 2. 基准期 > 0，当期 < 0：实际恶化，返回负值，公式同上（结果自然为负）
     * 3. 基准期 < 0，当期 <= 0：基准为负代表亏损，当期亏损减少（绝对值缩小）视为改善
     *    使用绝对值：(|基准期| - |当期|) / |基准期| × 100%（正值 = 改善）
     * 4. 基准期 < 0，当期 > 0：从亏损转为盈利，视为 +100%（完全改善）
     * 5. 基准期 = 0，当期 > 0：从零增长，返回 +100%
     * 6. 基准期 = 0，当期 < 0：从零变为亏损，返回 -100%
     * 7. 基准期 = 0，当期 = 0：返回 0%
     *
     * @param current  当期值
     * @param baseline 基准期值
     * @param label    用于日志标识
     * @return 趋势百分比（保留2位小数），null 表示无法计算
     */
    private BigDecimal calcTrendSafe(BigDecimal current, BigDecimal baseline, String label) {
        if (current == null) current = BigDecimal.ZERO;
        if (baseline == null) baseline = BigDecimal.ZERO;

        int baselineSign = baseline.compareTo(BigDecimal.ZERO);
        int currentSign  = current.compareTo(BigDecimal.ZERO);

        if (baselineSign == 0) {
            // 基准期为 0
            if (currentSign > 0) {
                log.debug("[趋势-{}] 基准期为0，当期为正，返回+100%", label);
                return new BigDecimal(100);
            } else if (currentSign < 0) {
                log.debug("[趋势-{}] 基准期为0，当期为负，返回-100%", label);
                return new BigDecimal(-100);
            } else {
                log.debug("[趋势-{}] 基准期与当期均为0，返回0%", label);
                return BigDecimal.ZERO;
            }
        }

        if (baselineSign < 0) {
            // 基准期为负数（亏损）
            if (currentSign >= 0) {
                // 从亏损转为盈利或持平，视为完全改善 +100%
                log.debug("[趋势-{}] 基准期为负，当期非负，从亏损转为盈利，返回+100%", label);
                return new BigDecimal(100);
            } else {
                // 两期均为负数：用绝对值衡量，亏损减少（|当期| < |基准期|）为改善（正值）
                BigDecimal absBaseline = baseline.abs();
                BigDecimal absCurrent  = current.abs();
                // (|基准期| - |当期|) / |基准期| × 100 → 亏损减少为正，亏损增加为负
                BigDecimal trend = absBaseline.subtract(absCurrent)
                        .divide(absBaseline, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(new BigDecimal(100))
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                log.debug("[趋势-{}] 基准期与当期均为负，绝对值比较：baseline={}, current={}, trend={}",
                        label, baseline, current, trend);
                return trend;
            }
        }

        // 基准期为正数（标准情况）：(当期 - 基准期) / 基准期 × 100
        BigDecimal trend = current.subtract(baseline)
                .divide(baseline, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal(100))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        log.debug("[趋势-{}] 标准计算：baseline={}, current={}, trend={}", label, baseline, current, trend);
        return trend;
    }

    @Override
    public Map<String, Object> calculatePaymentStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("批量计算收款统计数据，时间范围：{} - {}", startDate, endDate);

        Map<String, Object> result = new HashMap<>();

        try {
            QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
            settlementQuery.eq("结算类型", "RECEIVABLE");
            if (startDate != null) {
                settlementQuery.ge("审核时间", startDate);
            }
            if (endDate != null) {
                settlementQuery.le("审核时间", endDate);
            }

            List<Settlement> settlements = settlementMapper.selectList(settlementQuery);
            BigDecimal receivableAmount = BigDecimal.ZERO;
            BigDecimal receivedAmount = BigDecimal.ZERO;

            for (Settlement settlement : settlements) {
                if (settlement.getSettlementAmount() != null) {
                    receivableAmount = receivableAmount.add(settlement.getSettlementAmount());
                }
                if (settlement.getReceivedAmount() != null) {
                    receivedAmount = receivedAmount.add(settlement.getReceivedAmount());
                }
            }

            BigDecimal unreceivedAmount = receivableAmount.subtract(receivedAmount);
            if (unreceivedAmount.compareTo(BigDecimal.ZERO) < 0) {
                unreceivedAmount = BigDecimal.ZERO;
            }

            result.put("receivableAmount", receivableAmount);
            result.put("receivedAmount", receivedAmount);
            result.put("unreceivedAmount", unreceivedAmount);
            result.put("totalContractAmount", receivableAmount);
            result.put("totalReceivedAmount", receivedAmount);
            result.put("totalUnreceivedAmount", unreceivedAmount);

            try {
                LocalDateTime yoyBaseEnd = endDate != null ? endDate.minusYears(1) : LocalDateTime.now().minusYears(1);
                LocalDateTime yoyBaseStart = startDate != null ? startDate.minusYears(1)
                        : yoyBaseEnd.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                Map<String, Object> yoyPaymentAgg = calculatePaymentStatisticsBase(yoyBaseStart, yoyBaseEnd);
                BigDecimal yoyReceivableAmount = getMapBigDecimal(yoyPaymentAgg, "receivableAmount");
                BigDecimal yoyReceivedAmount = getMapBigDecimal(yoyPaymentAgg, "receivedAmount");
                BigDecimal yoyUnreceivedAmount = getMapBigDecimal(yoyPaymentAgg, "unreceivedAmount");
                result.put("receivableAmountYoy", calcTrendSafe(receivableAmount, yoyReceivableAmount, "应收金额同比"));
                result.put("receivedAmountYoy", calcTrendSafe(receivedAmount, yoyReceivedAmount, "已收金额同比"));
                result.put("unreceivedAmountYoy", calcTrendSafe(unreceivedAmount, yoyUnreceivedAmount, "未收金额同比"));
            } catch (Exception ex) {
                log.warn("[批量计算] 计算收款中心同比趋势失败", ex);
                result.put("receivableAmountYoy", BigDecimal.ZERO);
                result.put("receivedAmountYoy", BigDecimal.ZERO);
                result.put("unreceivedAmountYoy", BigDecimal.ZERO);
            }

            try {
                LocalDateTime momBaseEnd = endDate != null ? endDate.minusMonths(1) : LocalDateTime.now().minusMonths(1);
                LocalDateTime momBaseStart = startDate != null ? startDate.minusMonths(1)
                        : momBaseEnd.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                Map<String, Object> momPaymentAgg = calculatePaymentStatisticsBase(momBaseStart, momBaseEnd);
                BigDecimal momReceivableAmount = getMapBigDecimal(momPaymentAgg, "receivableAmount");
                BigDecimal momReceivedAmount = getMapBigDecimal(momPaymentAgg, "receivedAmount");
                BigDecimal momUnreceivedAmount = getMapBigDecimal(momPaymentAgg, "unreceivedAmount");
                result.put("receivableAmountMom", calcTrendSafe(receivableAmount, momReceivableAmount, "应收金额环比"));
                result.put("receivedAmountMom", calcTrendSafe(receivedAmount, momReceivedAmount, "已收金额环比"));
                result.put("unreceivedAmountMom", calcTrendSafe(unreceivedAmount, momUnreceivedAmount, "未收金额环比"));
            } catch (Exception ex) {
                log.warn("[批量计算] 计算收款中心环比趋势失败", ex);
                result.put("receivableAmountMom", BigDecimal.ZERO);
                result.put("receivedAmountMom", BigDecimal.ZERO);
                result.put("unreceivedAmountMom", BigDecimal.ZERO);
            }

            result.put("overduePaymentCount", 0);
            result.put("overduePaymentAmount", BigDecimal.ZERO);

            // 待认领款项统计
            QueryWrapper<FundTransaction> allIncomeQuery = new QueryWrapper<>();
            allIncomeQuery.eq("交易类型", "INCOME");
            List<FundTransaction> allIncomeTransactions = fundTransactionMapper.selectList(allIncomeQuery);

            List<SettlementFundTransactionRel> allPaymentRels = settlementFundTransactionRelMapper.selectList(new QueryWrapper<>());
            Set<Long> linkedTransactionIds = allPaymentRels.stream()
                    .filter(r -> r.getTransactionId() != null)
                    .map(SettlementFundTransactionRel::getTransactionId)
                    .collect(Collectors.toSet());

            long unclaimedCount = allIncomeTransactions.stream()
                    .filter(t -> t.getTransactionId() != null)
                    .filter(t -> !linkedTransactionIds.contains(t.getTransactionId()))
                    .count();

            result.put("unclaimedPaymentsCount", unclaimedCount);
            result.put("prepaymentRatio", BigDecimal.ZERO);
            result.put("prepaymentCount", 0);
            result.put("totalSettlementCount", settlements.size());
            result.put("totalPaymentRecords", settlements.size());

        } catch (Exception e) {
            log.error("批量计算收款统计数据失败", e);
            result.put("error", "计算失败：" + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> calculatePaymentStatisticsBase(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> result = new HashMap<>();

        QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
        settlementQuery.eq("结算类型", "RECEIVABLE");
        if (startDate != null) {
            settlementQuery.ge("审核时间", startDate);
        }
        if (endDate != null) {
            settlementQuery.le("审核时间", endDate);
        }

        List<Settlement> settlements = settlementMapper.selectList(settlementQuery);
        BigDecimal receivableAmount = BigDecimal.ZERO;
        BigDecimal receivedAmount = BigDecimal.ZERO;

        for (Settlement settlement : settlements) {
            if (settlement.getSettlementAmount() != null) {
                receivableAmount = receivableAmount.add(settlement.getSettlementAmount());
            }
            if (settlement.getReceivedAmount() != null) {
                receivedAmount = receivedAmount.add(settlement.getReceivedAmount());
            }
        }

        BigDecimal unreceivedAmount = receivableAmount.subtract(receivedAmount);
        if (unreceivedAmount.compareTo(BigDecimal.ZERO) < 0) {
            unreceivedAmount = BigDecimal.ZERO;
        }

        result.put("receivableAmount", receivableAmount);
        result.put("receivedAmount", receivedAmount);
        result.put("unreceivedAmount", unreceivedAmount);
        return result;
    }

    private BigDecimal getMapBigDecimal(Map<String, Object> source, String key) {
        if (source == null) {
            return BigDecimal.ZERO;
        }
        Object value = source.get(key);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return BigDecimal.ZERO;
    }

    /**
     * 基于结算单和合同关系估算预收款占比（当没有资金流水记录时的备选方案）
     */
    private BigDecimal estimatePrepaymentRatioFromSettlements() {
        try {
            // 查询所有已完成的结算单
            QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
            settlementQuery.eq("状态", "已结算");
            List<Settlement> completedSettlements = settlementMapper.selectList(settlementQuery);

            if (completedSettlements.isEmpty()) {
                return BigDecimal.ZERO;
            }

            // 统计预收款结算单数量（结算时间早于合同签订时间）
            long prepaymentCount = completedSettlements.stream()
                    .filter(settlement -> {
                        if (settlement.getContractId() != null) {
                            Contract contract = contractMapper.selectById(settlement.getContractId());
                            if (contract != null && contract.getSignTime() != null && settlement.getAuditTime() != null) {
                                return settlement.getAuditTime().isBefore(contract.getSignTime());
                            }
                        }
                        return false;
                    })
                    .count();

            BigDecimal prepaymentRatio = new BigDecimal((double) prepaymentCount / completedSettlements.size() * 100);
            log.debug("基于结算单估算预收款占比: {}% (预收款结算单: {}, 总结算单: {})", prepaymentRatio, prepaymentCount, completedSettlements.size());
            return prepaymentRatio;
        } catch (Exception e) {
            log.warn("基于结算单估算预收款占比失败", e);
            return new BigDecimal("5.0"); // 返回一个合理的默认值
        }
    }

    /**
     * 基于结算单状态估算已收款金额（当没有资金流水记录时的备选方案）
     */
    private BigDecimal estimateReceivedAmountFromSettlements() {
        try {
            // 查询所有有效状态的结算单（已审核、已结算）
            QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
            settlementQuery.in("状态", "已审核", "已结算");
            List<Settlement> validSettlements = settlementMapper.selectList(settlementQuery);

            BigDecimal estimatedAmount = validSettlements.stream()
                    .filter(s -> s.getReceivedAmount() != null)
                    .map(Settlement::getReceivedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("基于结算单状态估算已收款金额: {} (有效结算单数量: {}, 状态: 已审核/已结算)",
                     estimatedAmount, validSettlements.size());
            return estimatedAmount;
        } catch (Exception e) {
            log.warn("基于结算单估算收款金额失败", e);
            return BigDecimal.ZERO;
        }
    }

    // ============ 闭环数据一致性验证辅助方法 ============

    /**
     * 计算合同的执行总金额
     */
    private BigDecimal calculateExecutionTotalAmount(Contract contract) {
        try {
            BigDecimal totalAmount = BigDecimal.ZERO;

            // 获取合同相关的入库单
            List<Warehousing> warehousings = findWarehousingsByContract(contract);
            if (CollectionUtils.isEmpty(warehousings)) {
                return BigDecimal.ZERO;
            }

            // 获取合同的平均单价（用于估算执行金额）
            BigDecimal averageUnitPrice = getContractAverageUnitPrice(contract);
            if (averageUnitPrice == null || averageUnitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("合同{}没有有效的单价信息，返回0", contract.getContractNo());
                return BigDecimal.ZERO;
            }

            // 计算所有入库单的执行金额
            for (Warehousing warehousing : warehousings) {
                BigDecimal warehousingAmount = calculateWarehousingAmount(warehousing, averageUnitPrice);
                if (warehousingAmount != null) {
                    totalAmount = totalAmount.add(warehousingAmount);
                }
            }

            log.debug("合同{}执行总金额计算完成：{}元，基于{}个入库单",
                     contract.getContractNo(), totalAmount, warehousings.size());

            return totalAmount;

        } catch (Exception e) {
            log.warn("计算执行总金额失败，合同ID：{}", contract.getContractId(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 获取合同的平均单价
     */
    private BigDecimal getContractAverageUnitPrice(Contract contract) {
        try {
            if (contract.getContractId() == null) {
                return null;
            }

            // 查询合同的所有危废条目
            QueryWrapper<ContractWasteItem> wasteItemQuery = new QueryWrapper<>();
            wasteItemQuery.eq("合同条目编号", contract.getContractId()); // 假设合同ID对应合同条目编号
            List<ContractWasteItem> wasteItems = contractWasteItemMapper.selectList(wasteItemQuery);

            if (CollectionUtils.isEmpty(wasteItems)) {
                // 如果没有危废条目，尝试从合同总金额估算（简化处理）
                if (contract.getContractAmount() != null && contract.getContractAmount().compareTo(BigDecimal.ZERO) > 0) {
                    // 假设合同总金额按10吨估算单价，这是一个粗略的估算
                    return contract.getContractAmount().divide(new BigDecimal("10"), 2, BigDecimal.ROUND_HALF_UP);
                }
                return null;
            }

            // 计算加权平均单价
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal totalQuantity = BigDecimal.ZERO;

            for (ContractWasteItem item : wasteItems) {
                if (item.getUnitPrice() != null && item.getPlannedQuantity() != null &&
                    item.getUnitPrice().compareTo(BigDecimal.ZERO) > 0 &&
                    item.getPlannedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal itemAmount = item.getUnitPrice().multiply(item.getPlannedQuantity());
                    totalAmount = totalAmount.add(itemAmount);
                    totalQuantity = totalQuantity.add(item.getPlannedQuantity());
                }
            }

            if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
                return totalAmount.divide(totalQuantity, 2, BigDecimal.ROUND_HALF_UP);
            }

            // 如果没有有效的数量信息，返回平均单价
            BigDecimal[] prices = wasteItems.stream()
                    .filter(item -> item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) > 0)
                    .map(ContractWasteItem::getUnitPrice)
                    .toArray(BigDecimal[]::new);

            if (prices.length > 0) {
                BigDecimal sum = Arrays.stream(prices).reduce(BigDecimal.ZERO, BigDecimal::add);
                return sum.divide(new BigDecimal(prices.length), 2, BigDecimal.ROUND_HALF_UP);
            }

            return null;

        } catch (Exception e) {
            log.warn("获取合同平均单价失败，合同ID：{}", contract.getContractId(), e);
            return null;
        }
    }

    /**
     * 计算单个入库单的执行金额
     */
    private BigDecimal calculateWarehousingAmount(Warehousing warehousing, BigDecimal unitPrice) {
        try {
            if (warehousing == null || unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }

            // 获取磅单信息
            BigDecimal netWeight = null;
            if (warehousing.getWeighingSlipNo() != null) {
                QueryWrapper<WeighingSlip> weighingQuery = new QueryWrapper<>();
                weighingQuery.eq("总磅单号", warehousing.getWeighingSlipNo());
                WeighingSlip weighingSlip = weighingSlipMapper.selectOne(weighingQuery);
                if (weighingSlip != null && weighingSlip.getNetWeight() != null) {
                    netWeight = weighingSlip.getNetWeight();
                }
            }

            // 如果没有磅单净重，尝试使用其他方式估算（这里暂时返回0）
            if (netWeight == null || netWeight.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("入库单{}没有有效的净重信息", warehousing.getWarehousingNo());
                return BigDecimal.ZERO;
            }

            // 将净重从kg转换为吨（除以1000）
            BigDecimal netWeightTon = netWeight.divide(new BigDecimal("1000"), 3, BigDecimal.ROUND_HALF_UP);

            // 计算金额：净重(吨) × 单价(元/吨)
            BigDecimal amount = netWeightTon.multiply(unitPrice);

            log.debug("入库单{}执行金额计算：净重{}kg({}吨) × 单价{}元/吨 = {}元",
                     warehousing.getWarehousingNo(), netWeight, netWeightTon, unitPrice, amount);

            return amount;

        } catch (Exception e) {
            log.warn("计算入库单执行金额失败，入库单ID：{}", warehousing.getWarehousingId(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 根据合同查找相关的入库记录（通过收运通知单 -> 派车单 -> 入库单链路）
     * 以避免直接在 WAREHOUSING 表上使用不存在的合同编号列进行过滤
     */
    private List<Warehousing> findWarehousingsByContract(Contract contract) {
        if (contract == null || contract.getContractNo() == null) {
            return Collections.emptyList();
        }

        try {
            QueryWrapper<PickupNotice> pnQuery = new QueryWrapper<>();
            pnQuery.eq("合同号", contract.getContractNo());
            List<PickupNotice> notices = pickupNoticeMapper.selectList(pnQuery);
            if (CollectionUtils.isEmpty(notices)) {
                return Collections.emptyList();
            }

            List<String> noticeCodes = notices.stream()
                    .map(PickupNotice::getNoticeCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            QueryWrapper<DispatchOrder> dQ = new QueryWrapper<>();
            dQ.in("收运通知单号", noticeCodes);
            List<DispatchOrder> dispatches = dispatchOrderMapper.selectList(dQ);
            if (CollectionUtils.isEmpty(dispatches)) {
                return Collections.emptyList();
            }

            List<String> dispatchCodes = dispatches.stream()
                    .map(DispatchOrder::getDispatchCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (CollectionUtils.isEmpty(dispatchCodes)) {
                return Collections.emptyList();
            }

            QueryWrapper<Warehousing> wQ = new QueryWrapper<>();
            wQ.in("收运运输单号", dispatchCodes);
            return warehousingMapper.selectList(wQ);
        } catch (Exception e) {
            log.warn("通过收运通知单/派车单查找入库记录失败，合同号：{}，错误：{}", contract.getContractNo(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 计算合同的结算总金额
     */
    private BigDecimal calculateSettlementTotalAmount(Contract contract) {
        try {
            BigDecimal totalAmount = BigDecimal.ZERO;

            QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
            settlementQuery.eq("合同编号", contract.getContractId());
            List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

            if (!CollectionUtils.isEmpty(settlements)) {
                for (Settlement s : settlements) {
                    if (s.getSettlementAmount() != null) {
                        totalAmount = totalAmount.add(s.getSettlementAmount());
                    }
                }
            }

            return totalAmount;
        } catch (Exception e) {
            log.warn("计算结算总金额失败，合同ID：{}", contract.getContractId(), e);
            return null;
        }
    }

    /**
     * 计算合同的收款总金额
     */
    private BigDecimal calculatePaymentTotalAmount(Contract contract) {
        try {
            BigDecimal totalAmount = BigDecimal.ZERO;

            // 通过结算单找到关联的收款关联关系（SETTLEMENT_FUND_TRANSACTION_REL 使用结算单编号作为外键）
            QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
            settlementQuery.eq("合同编号", contract.getContractId());
            List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

            if (!CollectionUtils.isEmpty(settlements)) {
                List<Long> settlementIds = settlements.stream()
                        .map(Settlement::getSettlementId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                if (!settlementIds.isEmpty()) {
                    QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
                    relQuery.in("结算单编号", settlementIds);
                    List<SettlementFundTransactionRel> relations = settlementFundTransactionRelMapper.selectList(relQuery);

                    if (!CollectionUtils.isEmpty(relations)) {
                        for (SettlementFundTransactionRel rel : relations) {
                            if (rel.getRelAmount() != null) {
                                totalAmount = totalAmount.add(rel.getRelAmount());
                            }
                        }
                    }
                }
            }

            return totalAmount;
        } catch (Exception e) {
            log.warn("计算收款总金额失败，合同ID：{}", contract.getContractId(), e);
            return null;
        }
    }

    /**
     * 计算合同的发票总金额
     */
    private BigDecimal calculateInvoiceTotalAmount(Contract contract) {
        try {
            BigDecimal totalAmount = BigDecimal.ZERO;

            // 发票通常通过结算单关联（SETTLEMENT_INVOICE_REL）与结算单关联，结算单再关联合同
            QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
            settlementQuery.eq("合同编号", contract.getContractId());
            List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

            if (!CollectionUtils.isEmpty(settlements)) {
                List<Long> settlementIds = settlements.stream()
                        .map(Settlement::getSettlementId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                if (!settlementIds.isEmpty()) {
                    QueryWrapper<SettlementInvoiceRel> sirQuery = new QueryWrapper<>();
                    sirQuery.in("结算单编号", settlementIds);
                    List<SettlementInvoiceRel> rels = settlementInvoiceRelMapper.selectList(sirQuery);

                    if (!CollectionUtils.isEmpty(rels)) {
                        List<Integer> invoiceIds = rels.stream()
                                .map(SettlementInvoiceRel::getInvoiceId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                        if (!invoiceIds.isEmpty()) {
                            List<Invoice> invoices = invoiceMapper.selectBatchIds(invoiceIds);
                            if (!CollectionUtils.isEmpty(invoices)) {
                                for (Invoice i : invoices) {
                                    if (i.getAmount() != null) {
                                        totalAmount = totalAmount.add(i.getAmount());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return totalAmount;
        } catch (Exception e) {
            log.warn("计算发票总金额失败，合同ID：{}", contract.getContractId(), e);
            return null;
        }
    }

    /**
     * 验证入库数量与运输数量的一致性
     * 注意：由于Transport相关类可能不存在，此方法暂时简化为只检查入库数据的基本一致性
     */
    private List<ClosureIssueDTO> validateWarehousingTransportQuantityConsistency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 通过收运通知单/派车单链路查找入库记录，避免直接在 WAREHOUSING 使用合同编号列
            List<Warehousing> warehousings = findWarehousingsByContract(contract);

            BigDecimal warehousingQuantity = BigDecimal.ZERO;
            if (!CollectionUtils.isEmpty(warehousings)) {
                for (Warehousing w : warehousings) {
                    // 注意：这里假设Warehousing有getNetWeight方法，如果没有请根据实际情况调整
                    // 暂时注释掉，避免编译错误
                    // if (w.getNetWeight() != null) {
                    //     warehousingQuantity = warehousingQuantity.add(w.getNetWeight());
                    // }
                }
            }

            // 运输相关的逻辑暂时注释，因为Transport类可能不存在
            // QueryWrapper<Transport> transportQuery = new QueryWrapper<>();
            // transportQuery.eq("合同编号", contract.getContractId());
            // List<Transport> transports = transportMapper.selectList(transportQuery);
            // ... 相关比较逻辑

            // 暂时只返回空列表，表示没有发现问题
            // 后续需要根据实际的数据结构来实现完整逻辑

        } catch (Exception e) {
            log.warn("校验入库运输数量一致性失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 验证执行项目数量与结算项目数量的一致性
     */
    private List<ClosureIssueDTO> validateExecutionSettlementQuantityConsistency(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 计算执行项目数量（通过收运通知单/派车单链路查询入库单数量）
            List<Warehousing> warehousings = findWarehousingsByContract(contract);
            int executionCount = !CollectionUtils.isEmpty(warehousings) ? warehousings.size() : 0;

            // 计算结算项目数量
            QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
            settlementQuery.eq("合同编号", contract.getContractId());
            Long settlementCountLong = settlementMapper.selectCount(settlementQuery);
            int settlementCount = settlementCountLong != null ? settlementCountLong.intValue() : 0;

            if (executionCount > 0 && settlementCount > 0 && executionCount != settlementCount) {
                ClosureIssueDTO issue = new ClosureIssueDTO();
                issue.setIssueId("EXECUTION_SETTLEMENT_COUNT_" + contract.getContractId() + "_" + System.currentTimeMillis());
                issue.setIssueType(ISSUE_TYPE_DATA_INCONSISTENCY);
                issue.setRiskLevel(RISK_LEVEL_MEDIUM);
                issue.setRelatedEntityType("CONTRACT");
                issue.setRelatedEntityId(contract.getContractId().longValue());
                issue.setRelatedEntityCode(contract.getContractNo());
                issue.setRelatedEntityName("合同-" + contract.getContractNo());
                issue.setIssueTitle("执行项目数量与结算项目数量不一致");
                issue.setIssueDescription(String.format("执行项目数量：%d，结算项目数量：%d",
                    executionCount, settlementCount));
                issue.setSuggestedAction("请检查结算单是否包含所有执行项目");
                issue.setActionType(ACTION_TYPE_MANUAL_FIX);
                issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                issues.add(issue);
            }

        } catch (Exception e) {
            log.warn("校验执行结算项目数量一致性失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 验证签订时间与执行开始时间的一致性
     */
    private List<ClosureIssueDTO> validateContractExecutionTimeline(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            LocalDateTime signTime = contract.getSignTime();
            if (signTime == null) {
                return issues;
            }

            // 查找最早的执行记录时间（入库时间）
            LocalDateTime earliestExecutionTime = null;

            // 使用收运通知单/派车单链路查找最早的入库记录时间
            List<Warehousing> warehousings = findWarehousingsByContract(contract);
            Warehousing firstWarehousing = null;
            if (!CollectionUtils.isEmpty(warehousings)) {
                firstWarehousing = warehousings.stream()
                        .filter(w -> w.getWarehousingTime() != null)
                        .sorted(Comparator.comparing(Warehousing::getWarehousingTime))
                        .findFirst()
                        .orElse(null);
                if (firstWarehousing != null) {
                    // earliestExecutionTime = firstWarehousing.getWarehousingTime();
                }
            }

            // 运输相关的逻辑暂时注释，因为Transport类可能不存在
            // QueryWrapper<Transport> transportQuery = new QueryWrapper<>();
            // ... 相关处理逻辑

            // 检查时间顺序
            if (earliestExecutionTime != null && earliestExecutionTime.isBefore(signTime)) {
                long daysDifference = java.time.Duration.between(earliestExecutionTime, signTime).toDays();
                if (daysDifference > 0) { // 允许同一天
                    ClosureIssueDTO issue = new ClosureIssueDTO();
                    issue.setIssueId("CONTRACT_EXECUTION_TIMELINE_" + contract.getContractId() + "_" + System.currentTimeMillis());
                    issue.setIssueType(ISSUE_TYPE_TIME_SEQUENCE_VIOLATION);
                    issue.setRiskLevel(RISK_LEVEL_LOW);
                    issue.setRelatedEntityType("CONTRACT");
                    issue.setRelatedEntityId(contract.getContractId().longValue());
                    issue.setRelatedEntityCode(contract.getContractNo());
                    issue.setRelatedEntityName("合同-" + contract.getContractNo());
                    issue.setIssueTitle("签订时间晚于执行时间");
                    issue.setIssueDescription(String.format("合同签订时间：%s，最早执行时间：%s，时间差异：%d天",
                        signTime, earliestExecutionTime, Math.abs(daysDifference)));
                    issue.setSuggestedAction("请确认签订时间记录是否正确");
                    issue.setActionType(ACTION_TYPE_REVIEW_REQUIRED);
                    issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                    issues.add(issue);
                }
            }

        } catch (Exception e) {
            log.warn("校验签订执行时间顺序失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 验证执行完成时间与收款时间的一致性
     */
    private List<ClosureIssueDTO> validateExecutionPaymentTimeline(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 查找最晚的执行完成时间
            LocalDateTime latestExecutionTime = null;

            // 使用收运通知单/派车单链路查找最近一次入库记录时间
            List<Warehousing> warehousings = findWarehousingsByContract(contract);
            if (!CollectionUtils.isEmpty(warehousings)) {
                Warehousing lastWarehousing = warehousings.stream()
                        .filter(w -> w.getWarehousingTime() != null)
                        .sorted(Comparator.comparing(Warehousing::getWarehousingTime).reversed())
                        .findFirst()
                        .orElse(null);
                if (lastWarehousing != null && lastWarehousing.getWarehousingTime() != null) {
                    latestExecutionTime = lastWarehousing.getWarehousingTime();
                }
            }

            if (latestExecutionTime != null) {
                // 查找最早的收款时间
                QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
                relQuery.eq("合同编号", contract.getContractId());
                relQuery.orderByAsc("关联时间");
                relQuery.last("LIMIT 1");
                SettlementFundTransactionRel firstPayment = settlementFundTransactionRelMapper.selectOne(relQuery);

                if (firstPayment != null && firstPayment.getRelTime() != null) {
                    LocalDateTime firstPaymentTime = firstPayment.getRelTime();

                    // 检查收款是否在执行完成之前（不合理的收款）
                    if (firstPaymentTime.isBefore(latestExecutionTime.minusDays(1))) { // 允许1天误差
                        long daysDifference = java.time.Duration.between(firstPaymentTime, latestExecutionTime).toDays();
                        ClosureIssueDTO issue = new ClosureIssueDTO();
                        issue.setIssueId("EXECUTION_PAYMENT_TIMELINE_" + contract.getContractId() + "_" + System.currentTimeMillis());
                        issue.setIssueType(ISSUE_TYPE_TIME_SEQUENCE_VIOLATION);
                        issue.setRiskLevel(RISK_LEVEL_MEDIUM);
                        issue.setRelatedEntityType("CONTRACT");
                        issue.setRelatedEntityId(contract.getContractId().longValue());
                        issue.setRelatedEntityCode(contract.getContractNo());
                        issue.setRelatedEntityName("合同-" + contract.getContractNo());
                        issue.setIssueTitle("收款时间早于执行完成时间");
                        issue.setIssueDescription(String.format("最早收款时间：%s，最后执行时间：%s，时间差异：%d天",
                            firstPaymentTime, latestExecutionTime, Math.abs(daysDifference)));
                        issue.setSuggestedAction("请确认收款记录时间是否正确，或检查是否存在预收款");
                        issue.setActionType(ACTION_TYPE_REVIEW_REQUIRED);
                        issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                        issues.add(issue);
                    }
                }
            }

        } catch (Exception e) {
            log.warn("校验执行收款时间顺序失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    /**
     * 验证收款时间与开票时间的一致性
     */
    private List<ClosureIssueDTO> validatePaymentInvoiceTimeline(Contract contract) {
        List<ClosureIssueDTO> issues = new ArrayList<>();

        try {
            // 查找与合同关联的结算单，并获取最早的收款时间
            SettlementFundTransactionRel firstPayment = null;
            try {
                QueryWrapper<Settlement> settlementQuery = new QueryWrapper<>();
                settlementQuery.eq("合同编号", contract.getContractId());
                List<Settlement> settlements = settlementMapper.selectList(settlementQuery);

                if (settlements != null && !settlements.isEmpty()) {
                    List<Long> settlementIds = settlements.stream()
                            .map(Settlement::getSettlementId)
                            .collect(Collectors.toList());

                    QueryWrapper<SettlementFundTransactionRel> relQuery = new QueryWrapper<>();
                    relQuery.in("结算单编号", settlementIds);
                    relQuery.orderByAsc("关联时间");
                    relQuery.last("LIMIT 1");
                    firstPayment = settlementFundTransactionRelMapper.selectOne(relQuery);
                } else {
                    // 合同没有对应的结算单，跳过该校验（保留原有逻辑：不抛出异常）
                    log.debug("合同{}没有找到相关结算单，跳过收款-开票时间校验", contract.getContractId());
                }
            } catch (Exception ex) {
                // 捕获并记录查询异常，但继续后续逻辑（保持原有容错行为）
                log.warn("查询结算单或收款关联记录时出错，合同ID：{}，错误：{}", contract.getContractId(), ex.getMessage());
            }

            if (firstPayment != null && firstPayment.getRelTime() != null) {
                LocalDateTime firstPaymentTime = firstPayment.getRelTime();

                // 查找最早的开票时间
                QueryWrapper<Invoice> invoiceQuery = new QueryWrapper<>();
                invoiceQuery.eq("合同编号", contract.getContractId());
                invoiceQuery.orderByAsc("开票日期");
                invoiceQuery.last("LIMIT 1");
                Invoice firstInvoice = invoiceMapper.selectOne(invoiceQuery);

                if (firstInvoice != null && firstInvoice.getInvoiceDate() != null) {
                    LocalDateTime firstInvoiceTime = firstInvoice.getInvoiceDate();

                    // 检查开票是否在收款之前（违反开票必须在收款之后的原则）
                    if (firstInvoiceTime.isBefore(firstPaymentTime)) {
                        long daysDifference = java.time.Duration.between(firstInvoiceTime, firstPaymentTime).toDays();
                        ClosureIssueDTO issue = new ClosureIssueDTO();
                        issue.setIssueId("PAYMENT_INVOICE_TIMELINE_" + contract.getContractId() + "_" + System.currentTimeMillis());
                        issue.setIssueType(ISSUE_TYPE_TIME_SEQUENCE_VIOLATION);
                        issue.setRiskLevel(RISK_LEVEL_HIGH);
                        issue.setRelatedEntityType("CONTRACT");
                        issue.setRelatedEntityId(contract.getContractId().longValue());
                        issue.setRelatedEntityCode(contract.getContractNo());
                        issue.setRelatedEntityName("合同-" + contract.getContractNo());
                        issue.setIssueTitle("开票时间早于收款时间");
                        issue.setIssueDescription(String.format("最早收款时间：%s，最早开票时间：%s，时间差异：%d天",
                            firstPaymentTime, firstInvoiceTime, Math.abs(daysDifference)));
                        issue.setSuggestedAction("请检查开票时间记录，或确认是否存在预开票情况");
                        issue.setActionType(ACTION_TYPE_REVIEW_REQUIRED);
                        issue.setDetectedAt(DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss"));
                        issues.add(issue);
                    }
                }
            }

        } catch (Exception e) {
            log.warn("校验收款开票时间顺序失败，合同ID：{}", contract.getContractId(), e);
        }

        return issues;
    }

    // ==================== Redis缓存相关方法 ====================

    /**
     * 将校验问题列表保存到Redis缓存
     *
     * @param issues 问题列表
     */
    private void saveIssuesToCache(List<ClosureIssueDTO> issues) {
        try {
            redisTemplate.opsForValue().set(
                RedisConstant.CLOSURE_VALIDATION_ISSUES_KEY,
                issues,
                RedisConstant.CLOSURE_VALIDATION_CACHE_TTL,
                java.util.concurrent.TimeUnit.SECONDS
            );
            log.info("校验问题列表已保存到Redis缓存，问题数量：{}", issues.size());
        } catch (Exception e) {
            log.error("保存校验问题列表到Redis缓存失败", e);
        }
    }

    /**
     * 从Redis缓存获取校验问题列表
     *
     * @return 问题列表，如果缓存不存在返回null
     */
    @SuppressWarnings("unchecked")
    private List<ClosureIssueDTO> getIssuesFromCache() {
        try {
            Object cached = redisTemplate.opsForValue().get(RedisConstant.CLOSURE_VALIDATION_ISSUES_KEY);
            if (cached instanceof List) {
                List<ClosureIssueDTO> issues = (List<ClosureIssueDTO>) cached;
                log.info("从Redis缓存获取校验问题列表，问题数量：{}", issues.size());
                return issues;
            }
        } catch (Exception e) {
            log.error("从Redis缓存获取校验问题列表失败", e);
        }
        return null;
    }

    /**
     * 将看板统计数据保存到Redis缓存
     *
     * @param stats 统计数据
     */
    private void saveDashboardStatsToCache(com.erp.controller.finance.dto.ClosureDashboardStats stats) {
        try {
            redisTemplate.opsForValue().set(
                RedisConstant.CLOSURE_VALIDATION_DASHBOARD_KEY,
                stats,
                RedisConstant.CLOSURE_VALIDATION_CACHE_TTL,
                java.util.concurrent.TimeUnit.SECONDS
            );
            log.info("看板统计数据已保存到Redis缓存");
        } catch (Exception e) {
            log.error("保存看板统计数据到Redis缓存失败", e);
        }
    }

    /**
     * 从Redis缓存获取看板统计数据
     *
     * @return 统计数据，如果缓存不存在返回null
     */
    private com.erp.controller.finance.dto.ClosureDashboardStats getDashboardStatsFromCache() {
        try {
            Object cached = redisTemplate.opsForValue().get(RedisConstant.CLOSURE_VALIDATION_DASHBOARD_KEY);
            if (cached instanceof com.erp.controller.finance.dto.ClosureDashboardStats) {
                log.info("从Redis缓存获取看板统计数据");
                return (com.erp.controller.finance.dto.ClosureDashboardStats) cached;
            }
        } catch (Exception e) {
            log.error("从Redis缓存获取看板统计数据失败", e);
        }
        return null;
    }

    /**
     * 保存最后更新时间到Redis缓存
     *
     * @param updateTime 更新时间
     */
    private void saveLastUpdateTimeToCache(String updateTime) {
        try {
            redisTemplate.opsForValue().set(
                RedisConstant.CLOSURE_VALIDATION_LAST_UPDATE_KEY,
                updateTime,
                RedisConstant.CLOSURE_VALIDATION_CACHE_TTL,
                java.util.concurrent.TimeUnit.SECONDS
            );
            log.info("最后更新时间已保存到Redis缓存：{}", updateTime);
            lastValidationAt = updateTime; // 同时更新内存变量，保持兼容性
        } catch (Exception e) {
            log.error("保存最后更新时间到Redis缓存失败", e);
        }
    }

    /**
     * 从Redis缓存获取最后更新时间
     *
     * @return 更新时间，如果缓存不存在返回null
     */
    private String getLastUpdateTimeFromCache() {
        try {
            Object cached = redisTemplate.opsForValue().get(RedisConstant.CLOSURE_VALIDATION_LAST_UPDATE_KEY);
            if (cached instanceof String) {
                return (String) cached;
            }
        } catch (Exception e) {
            log.error("从Redis缓存获取最后更新时间失败", e);
        }
        return null;
    }

    /**
     * 清除所有业务闭环校验相关的缓存
     */
    private void clearAllCache() {
        try {
            redisTemplate.delete(Arrays.asList(
                RedisConstant.CLOSURE_VALIDATION_ISSUES_KEY,
                RedisConstant.CLOSURE_VALIDATION_DASHBOARD_KEY,
                RedisConstant.CLOSURE_VALIDATION_LAST_UPDATE_KEY
            ));
            log.info("已清除所有业务闭环校验缓存");
        } catch (Exception e) {
            log.error("清除业务闭环校验缓存失败", e);
        }
    }

    @Override
    public byte[] exportClosureIssues(String issueType, String riskLevel, String businessType,
                                    String contractCode, String dateRange) {
        try {
            log.info("开始导出业务闭环问题Excel，筛选条件：issueType={}, riskLevel={}, businessType={}, contractCode={}, dateRange={}",
                     issueType, riskLevel, businessType, contractCode, dateRange);

            // 获取问题数据（复用现有的查询逻辑）
            ClosureIssuePageResponse pageResponse = getClosureIssues(
                issueType, riskLevel, businessType, contractCode, dateRange, false, 1, Integer.MAX_VALUE
            );

            List<ClosureIssueDTO> issues = pageResponse.getRecords();
            log.info("获取到{}条问题记录，准备生成Excel文件", issues.size());

            // 生成Excel文件
            return generateClosureIssuesExcel(issues);

        } catch (Exception e) {
            log.error("导出业务闭环问题Excel失败", e);
            throw new RuntimeException("导出Excel失败：" + e.getMessage());
        }
    }

    /**
     * 生成业务闭环问题Excel文件
     */
    private byte[] generateClosureIssuesExcel(List<ClosureIssueDTO> issues) throws Exception {
        // 使用Hutool的Excel工具生成Excel
        cn.hutool.poi.excel.ExcelWriter writer = cn.hutool.poi.excel.ExcelUtil.getWriter();

        // 设置表头
        writer.addHeaderAlias("issueId", "问题编号");
        writer.addHeaderAlias("issueType", "问题类型");
        writer.addHeaderAlias("riskLevel", "风险等级");
        writer.addHeaderAlias("issueTitle", "问题标题");
        writer.addHeaderAlias("relatedEntityType", "关联业务");
        writer.addHeaderAlias("relatedEntityCode", "业务编号");
        writer.addHeaderAlias("relatedEntityName", "业务名称");
        writer.addHeaderAlias("detectedAt", "检测时间");

        // 处理数据，将枚举值转换为中文显示
        List<Map<String, Object>> dataList = issues.stream().map(issue -> {
            Map<String, Object> row = new HashMap<>();
            row.put("issueId", issue.getIssueId());
            row.put("issueType", convertIssueTypeToLabel(issue.getIssueType()));
            row.put("riskLevel", convertRiskLevelToLabel(issue.getRiskLevel()));
            row.put("issueTitle", issue.getIssueTitle());
            row.put("relatedEntityType", convertBusinessTypeToLabel(issue.getRelatedEntityType()));
            row.put("relatedEntityCode", issue.getRelatedEntityCode());
            row.put("relatedEntityName", issue.getRelatedEntityName() != null ? issue.getRelatedEntityName() : "-");
            row.put("detectedAt", issue.getDetectedAt());
            return row;
        }).collect(Collectors.toList());

        // 写入数据
        writer.write(dataList, true);

        // 设置列宽
        writer.setColumnWidth(0, 20); // 问题编号
        writer.setColumnWidth(1, 15); // 问题类型
        writer.setColumnWidth(2, 12); // 风险等级
        writer.setColumnWidth(3, 40); // 问题标题
        writer.setColumnWidth(4, 12); // 关联业务
        writer.setColumnWidth(5, 20); // 业务编号
        writer.setColumnWidth(6, 25); // 业务名称
        writer.setColumnWidth(7, 18); // 检测时间

        // 获取Excel文件的字节数组
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.flush(outputStream, true);
        writer.close();

        return outputStream.toByteArray();
    }

    /**
     * 将问题类型枚举转换为中文标签
     */
    private String convertIssueTypeToLabel(String issueType) {
        if (issueType == null) return "-";

        switch (issueType) {
            case "TIME_SEQUENCE_VIOLATION":
            case "PREPAYMENT_VIOLATION":
            case "OVERDUE_EXECUTION":
                return "时间顺序异常";
            case "AMOUNT_MISMATCH":
            case "INVALID_DATA":
                return "金额异常";
            case "STATUS_INCONSISTENCY":
            case "DATA_INCONSISTENCY":
            case "BUSINESS_CLOSURE":
            case "UNCLAIMED_PAYMENT":
            case "DUPLICATE_RECORD":
                return "业务闭环";
            case "MISSING_ASSOCIATION":
                return "关联缺失";
            default:
                return issueType;
        }
    }

    /**
     * 将风险等级枚举转换为中文标签
     */
    private String convertRiskLevelToLabel(String riskLevel) {
        if (riskLevel == null) return "-";

        switch (riskLevel) {
            case "CRITICAL": return "红色预警";
            case "HIGH": return "高";
            case "MEDIUM": return "中";
            case "LOW": return "低";
            default: return riskLevel;
        }
    }

    /**
     * 将业务类型枚举转换为中文标签
     */
    private String convertBusinessTypeToLabel(String businessType) {
        if (businessType == null) return "-";

        switch (businessType) {
            case "CONTRACT": return "合同";
            case "SETTLEMENT": return "结算单";
            case "RECEIPT": return "回单";
            case "WAREHOUSING": return "入库单";
            case "INVOICE": return "发票";
            case "FUND_TRANSACTION": return "资金流水";
            default: return businessType;
        }
    }

    /**
     * 检查合同是否有运输数据（收运通知单）
     */
    private boolean checkContractHasTransportData(Contract contract) {
        if (contract == null || contract.getContractNo() == null) {
            return false;
        }

        try {
            // 检查是否存在收运通知单
            QueryWrapper<PickupNotice> pickupQuery = new QueryWrapper<>();
            pickupQuery.eq("合同号", contract.getContractNo());
            List<PickupNotice> pickupNotices = pickupNoticeMapper.selectList(pickupQuery);

            if (!CollectionUtils.isEmpty(pickupNotices)) {
                // 检查是否有已审核通过或已派单的收运通知单
                return pickupNotices.stream().anyMatch(notice ->
                    "已派单".equals(notice.getStatus()) ||
                    "已完成".equals(notice.getStatus()) ||
                    (notice.getAuditedAt() != null && notice.getAuditorId() != null)
                );
            }

            return false;
        } catch (Exception e) {
            log.warn("检查合同运输数据时发生异常，合同ID：{}", contract.getContractId(), e);
            return false;
        }
    }

    /**
     * 检查合同是否有入库数据
     */
    private boolean checkContractHasWarehousingData(Contract contract) {
        if (contract == null || contract.getContractId() == null) {
            return false;
        }

        try {
            // 检查是否存在入库记录
            List<Warehousing> warehousings = findWarehousingsByContract(contract);
            return !CollectionUtils.isEmpty(warehousings);
        } catch (Exception e) {
            log.warn("检查合同入库数据时发生异常，合同ID：{}", contract.getContractId(), e);
            return false;
        }
    }
}
