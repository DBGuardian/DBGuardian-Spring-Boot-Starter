package com.erp.controller.report.dto;

import lombok.Data;

import java.util.List;

/**
 * 资金余额变动趋势 - 请求参数
 *
 * 功能描述：管理看板资金余额变动折线图查询请求
 * 入参：{ dateRange, startDate, endDate, accountIds }
 * url地址：/api/report/fund/balance-trend
 * 请求方式：POST
 */
@Data
public class FundBalanceTrendRequest {

    /**
     * 时间粒度：day=按天（当月每天）/ month=按月（当年每月）/ year=按年（近5年）
     */
    private String dateRange = "day";

    /**
     * 自定义开始日期（YYYY-MM-DD），传入则覆盖 dateRange 的默认计算
     */
    private String startDate;

    /**
     * 自定义结束日期（YYYY-MM-DD）
     */
    private String endDate;

    /**
     * 指定账户 ID 列表；不传则查询所有启用账户
     */
    private List<Long> accountIds;
}
