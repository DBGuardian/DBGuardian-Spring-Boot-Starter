package com.erp.controller.report.dto;

import lombok.Data;

/**
 * 应收账款余额变动趋势 - 请求参数
 *
 * 功能描述：查询各计价类型的应收账款余额变动趋势图表数据
 * 入参：{ startDate, endDate, granularity }
 * url地址：/api/report/receivable/balance-trend
 * 请求方式：POST
 */
@Data
public class ReceivableBalanceTrendRequest {

    /**
     * 查询开始日期（yyyy-MM-dd），不传则默认当年1月1日
     */
    private String startDate;

    /**
     * 查询结束日期（yyyy-MM-dd），不传则默认今天
     */
    private String endDate;

    /**
     * 时间粒度：day=按天 / month=按月（默认）/ year=按年
     */
    private String granularity = "month";
}
