package com.erp.controller.report.dto;

import lombok.Data;

/**
 * 危险废物限额分析 - 请求参数
 *
 * 功能描述：查询危险废物限额、合同签订量、实际收运量、联单确认量的汇总数据
 * 入参：{ dateRangeMode, startDate, endDate, year, wasteCategories }
 * url地址：/api/report/waste/limit-analysis
 * 请求方式：POST
 */
@Data
public class WasteLimitAnalysisRequest {

    /**
     * 时间模式
     * CURRENT_YEAR = 当年模式（自定义日期范围，必须在当年内）
     * PREVIOUS_YEAR = 往年模式（按年份查询）
     */
    private String dateRangeMode = "CURRENT_YEAR";

    /**
     * 自定义开始日期（yyyy-MM-dd），CURRENT_YEAR模式必填
     */
    private String startDate;

    /**
     * 自定义结束日期（yyyy-MM-dd），CURRENT_YEAR模式必填
     */
    private String endDate;

    /**
     * 年份（PREVIOUS_YEAR模式必填）
     */
    private Integer year;

    /**
     * 废物类别筛选（如 ["HW01", "HW02"]），不传则查询所有类别
     */
    private String[] wasteCategories;
}
