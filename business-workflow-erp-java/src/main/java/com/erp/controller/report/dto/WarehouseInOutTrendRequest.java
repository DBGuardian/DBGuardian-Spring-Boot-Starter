package com.erp.controller.report.dto;

import lombok.Data;

/**
 * 仓库出入库数量变动趋势 - 请求参数
 *
 * 功能描述：按时间粒度聚合入库、出库危废重量，返回柱线混合图数据
 * 入参：{ startDate, endDate, granularity }
 * url地址：/api/report/warehouse/in-out-trend
 * 请求方式：POST
 */
@Data
public class WarehouseInOutTrendRequest {

    /**
     * 查询开始日期（yyyy-MM-dd），不传则按粒度推算默认值
     */
    private String startDate;

    /**
     * 查询结束日期（yyyy-MM-dd），不传则默认今天
     */
    private String endDate;

    /**
     * 时间粒度：day=按日（当月每天）/ month=按月（当年每月，默认）/ year=按年（近5年）
     */
    private String granularity = "month";
}
