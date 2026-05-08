package com.erp.controller.finance.dto;

import lombok.Data;

/**
 * 业务闭环趋势数据DTO
 *
 * @author ERP System
 * @date 2025-02-06
 */
@Data
public class ClosureTrendData {

    /**
     * 时间点（格式：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）
     */
    private String timePoint;

    /**
     * 指标值
     */
    private Integer value;

    /**
     * 指标名称
     */
    private String metricName;

    /**
     * 指标类型
     */
    private String metricType;

    /**
     * 时间周期（DAY/WEEK/MONTH）
     */
    private String period;

    /**
     * 环比增长率（可选）
     */
    private Double growthRate;

    /**
     * 具体数据详情（JSON格式）
     */
    private String details;
}
