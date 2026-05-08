package com.erp.controller.report.dto;

import lombok.Data;

/**
 * 员工业绩占比饼图 - 请求参数
 *
 * 功能描述：根据维度统计每位在职员工的业绩数据及占比，支持按日期区间筛选，返回饼状图所需数据
 * 入参：{ dimension, dateRange, startDate, endDate }
 * url地址：/api/report/employee-performance/pie
 * 请求方式：POST
 */
@Data
public class EmployeePerformancePieRequest {

    /**
     * 统计维度：
     *   CONTRACT_COUNT    - 合同签订数量（份）
     *   WAREHOUSE_WEIGHT  - 入库重量（吨）
     *   SETTLEMENT_AMOUNT - 结算金额（元）
     */
    private String dimension = "CONTRACT_COUNT";

    /**
     * 日期范围模式：
     *   CURRENT_YEAR - 当年（默认）
     *   ALL          - 全部时间（不限日期）
     */
    private String dateRange = "CURRENT_YEAR";

    /**
     * 查询开始日期（yyyy-MM-dd），不传则根据 dateRange 自动填充
     */
    private String startDate;

    /**
     * 查询结束日期（yyyy-MM-dd），不传则根据 dateRange 自动填充
     */
    private String endDate;
}
