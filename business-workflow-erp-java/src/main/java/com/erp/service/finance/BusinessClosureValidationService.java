package com.erp.service.finance;

import com.erp.controller.finance.dto.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 业务闭环校验服务接口
 *
 * @author ERP System
 * @date 2025-02-05
 */
public interface BusinessClosureValidationService {

    /**
     * 执行全量业务闭环校验
     *
     * @param request 校验请求参数
     * @return 校验结果
     */
    ClosureValidationResponse executeFullValidation(ClosureValidationRequest request);

    /**
     * 校验指定合同的业务闭环
     *
     * @param contractId 合同ID
     * @param checkItems 校验项目，不传则执行所有校验
     * @return 发现的问题列表
     */
    List<ClosureIssueDTO> validateContractClosure(Long contractId, List<String> checkItems);

    /**
     * 执行时间顺序校验
     *
     * @param contractIds 合同ID列表，不传则校验所有合同
     * @return 发现的时间顺序问题列表
     */
    List<ClosureIssueDTO> validateTimeSequence(List<Long> contractIds);

    /**
     * 执行金额一致性校验
     *
     * @param contractIds 合同ID列表，不传则校验所有合同
     * @return 发现的金额不一致问题列表
     */
    List<ClosureIssueDTO> validateAmountConsistency(List<Long> contractIds);

    /**
     * 执行数据关联完整性校验
     *
     * @param contractIds 合同ID列表，不传则校验所有合同
     * @return 发现的关联缺失问题列表
     */
    List<ClosureIssueDTO> validateAssociationIntegrity(List<Long> contractIds);

    /**
     * 执行待认领款项校验（收款无法对应合同）
     *
     * @return 发现的待认领款项问题列表
     */
    List<ClosureIssueDTO> validateUnclaimedPayments();

    List<ClosureIssueDTO> validateStatusConsistency(List<Long> contractIds);

    /**
     * 获取校验项目详情
     *
     * @return 所有校验项目的详细信息
     */
    List<ValidationCheckItemDTO> getValidationCheckItems();

    /**
     * 批量校验多个合同的业务闭环
     *
     * @param request 批量校验请求
     * @return 批量校验结果
     */
    BatchValidationResponse batchValidate(BatchValidationRequest request);

    /**
     * 获取指定合同的校验结果
     *
     * @param contractId 合同ID
     * @return 校验结果
     */
    ClosureValidationResponse getValidationResults(Long contractId);

    /**
     * 修复校验异常
     *
     * @param request 修复请求
     * @return 修复结果
     */
    FixValidationResponse fixValidation(FixValidationRequest request);

    /**
     * 获取闭环问题列表
     *
     * @param issueType 问题类型
     * @param riskLevel 风险等级
     * @param businessType 业务类型
     * @param contractCode 合同编号
     * @param dateRange 时间范围
     * @param current 当前页
     * @param size 页大小
     * @return 问题列表分页结果
     */
    ClosureIssuePageResponse getClosureIssues(String issueType, String riskLevel, String businessType,
                                             String contractCode, String dateRange, Boolean forceRefresh, Integer current, Integer size);

    /**
     * 获取看板统计数据（用于 KPI 卡片）
     *
     * @param dateRange 时间范围
     * @param organizationId 组织ID
     * @return 仪表盘统计数据
     */
    ClosureDashboardStats getDashboardStats(String dateRange, Long organizationId);

    /**
     * 获取趋势数据
     *
     * @param metricType 指标类型
     * @param period 时间周期
     * @param dateRange 时间范围
     * @return 趋势数据列表
     */
    List<ClosureTrendData> getTrendData(String metricType, String period, String dateRange);

    /**
     * 批量计算合同统计数据（优化性能）
     *
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 合同统计汇总数据
     */
    Map<String, Object> calculateContractStatistics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 批量计算收款统计数据（优化性能）
     *
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 收款统计汇总数据
     */
    Map<String, Object> calculatePaymentStatistics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 导出业务闭环问题列表为Excel
     *
     * @param issueType 问题类型筛选
     * @param riskLevel 风险等级筛选
     * @param businessType 业务类型筛选
     * @param contractCode 合同编号筛选
     * @param dateRange 时间范围筛选
     * @return Excel文件字节数组
     */
    byte[] exportClosureIssues(String issueType, String riskLevel, String businessType,
                              String contractCode, String dateRange);
}
