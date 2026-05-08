package com.erp.controller.finance.dto;

import lombok.Data;

import java.util.Map;

/**
 * 闭环统计响应DTO
 *
 * @author ERP System
 * @date 2025-02-06
 */
@Data
public class ClosureStatisticsResponse {

    /**
     * 时间范围
     */
    private String dateRange;

    /**
     * 组织ID
     */
    private Long organizationId;

    /**
     * 总校验次数
     */
    private Integer totalValidations;

    /**
     * 发现问题总数
     */
    private Integer totalIssues;

    /**
     * 已解决的问题数
     */
    private Integer resolvedIssues;

    /**
     * 问题解决率
     */
    private Double resolutionRate;

    /**
     * 各类型问题统计
     */
    private Map<String, Integer> issuesByType;

    /**
     * 各风险等级问题统计
     */
    private Map<String, Integer> issuesByRiskLevel;

    /**
     * 趋势数据
     */
    private Map<String, Object> trendData;
}
