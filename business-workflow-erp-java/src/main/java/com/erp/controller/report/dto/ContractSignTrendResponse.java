package com.erp.controller.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 合同签订数量变动趋势 - 响应数据
 *
 * 功能描述：各计价类型合同签订数量随时间变动的柱线混合图数据
 * 返回参数：{ startDate, endDate, granularity, fromCache, computedAt, xAxis, series, summary }
 * url地址：/api/report/contract/sign-trend
 * 请求方式：POST
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractSignTrendResponse {

    /** 实际查询开始日期 */
    private String startDate;

    /** 实际查询结束日期 */
    private String endDate;

    /** 时间粒度：day / month / year */
    private String granularity;

    /** 是否来自 Redis 缓存 */
    private boolean fromCache;

    /** 数据计算时间（ISO 8601 格式） */
    private String computedAt;

    /** 横轴日期标签数组，按时间升序（大写驼峰，供前端 xAxis 读取） */
    private List<String> xAxis;

    /** 兼容字段：与 xAxis 相同，供前端 xaxis（小写）读取 */
    private List<String> xaxis;

    /** 设置横轴数据（同时写入 xAxis 和 xaxis 两个字段，确保前后端兼容） */
    public void setXAxis(List<String> xAxis) {
        this.xAxis = xAxis;
        this.xaxis = xAxis;
    }

    /** 各计价类型系列数据，固定 4 条（顺序：PACKAGE / UNIT / MIXED / TOTAL） */
    private List<ContractSignSeries> series;

    /** 汇总统计 */
    private ContractSignSummary summary;

    // ─────────────────────────────── 内部类 ───────────────────────────────

    /**
     * 单条计价类型系列数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractSignSeries {

        /** 计价类型名称：总价包干 / 按量结算 / 混合计价 / 总数量 */
        private String name;

        /** 类型标识：PACKAGE / UNIT / MIXED */
        private String pricingType;

        /** 与 xAxis 一一对应的各时间节点签订数量（整数，无数据为 0） */
        private List<Integer> data;
    }

    /**
     * 汇总统计数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractSignSummary {

        /** 查询区间内签订合同总数（三类之和） */
        private int totalCount;

        /** 查询区间内总价包干合同总数 */
        private int packageCount;

        /** 查询区间内按量结算合同总数 */
        private int unitCount;

        /** 查询区间内混合计价合同总数 */
        private int mixedCount;

        /** 最新时间节点签订总数 */
        private int currentPeriodTotal;

        /** 最新时间节点总价包干数 */
        private int currentPeriodPackage;

        /** 最新时间节点按量结算数 */
        private int currentPeriodUnit;

        /** 最新时间节点混合计价数 */
        private int currentPeriodMixed;
    }

    /**
     * Mapper 查询结果 DTO（内部使用）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractSignCountDTO {

        /** 时间粒度标签（如 2026-03 / 2026-03-17 / 2026） */
        private String dateLabel;

        /** 计价类型标识：PACKAGE / UNIT / MIXED */
        private String pricingType;

        /** 本期签订合同数量 */
        private int count;
    }
}
