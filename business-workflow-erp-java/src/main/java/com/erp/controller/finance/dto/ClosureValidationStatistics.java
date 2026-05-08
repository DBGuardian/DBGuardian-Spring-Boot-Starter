package com.erp.controller.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 业务闭环校验统计信息DTO
 *
 * @author ERP System
 * @date 2025-02-06
 */
@Data
public class ClosureValidationStatistics {

    /**
     * 总校验次数
     */
    private Integer totalValidations;

    /**
     * 成功校验次数
     */
    private Integer successfulValidations;

    /**
     * 失败校验次数
     */
    private Integer failedValidations;

    /**
     * 发现问题总数
     */
    private Integer totalIssuesFound;

    /**
     * 已解决的问题数
     */
    private Integer resolvedIssues;

    /**
     * 未解决的问题数
     */
    private Integer unresolvedIssues;

    /**
     * CRITICAL风险问题数
     */
    private Integer criticalIssues;

    /**
     * HIGH风险问题数
     */
    private Integer highRiskIssues;

    /**
     * MEDIUM风险问题数
     */
    private Integer mediumRiskIssues;

    /**
     * LOW风险问题数
     */
    private Integer lowRiskIssues;

    /**
     * 涉及合同数量
     */
    private Integer contractsValidated;

    /**
     * 涉及结算单数量
     */
    private Integer settlementsValidated;

    /**
     * 平均校验耗时（毫秒）
     */
    private Long averageValidationTime;

    /**
     * 最近校验时间
     */
    private String lastValidationTime;

    /**
     * 统计时间范围开始
     */
    private String statisticsStartDate;

    /**
     * 统计时间范围结束
     */
    private String statisticsEndDate;
}
