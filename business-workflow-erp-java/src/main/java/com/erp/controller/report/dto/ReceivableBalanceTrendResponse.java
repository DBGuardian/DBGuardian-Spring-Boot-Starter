package com.erp.controller.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 应收账款余额变动趋势 - 响应数据
 *
 * 功能描述：各计价类型应收账款余额随时间变动的折线图数据
 * 返回参数：{ startDate, endDate, granularity, computedAt, xAxis, series, summary }
 * url地址：/api/report/receivable/balance-trend
 * 请求方式：POST
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceivableBalanceTrendResponse {

    /** 实际查询开始日期 */
    private String startDate;

    /** 实际查询结束日期 */
    private String endDate;

    /** 时间粒度：day / month / year */
    private String granularity;

    /** 数据计算时间（ISO 8601 格式） */
    private String computedAt;

    /** 是否来自缓存；true=缓存命中，false=实时计算 */
    private Boolean fromCache;

    /** 横轴日期标签数组，按时间升序（兼容前端 xaxis 小写字段名） */
    private List<String> xAxis;

    /** 兼容字段：与 xAxis 相同，供前端 xaxis（小写）读取 */
    private List<String> xaxis;

    /** 设置横轴数据（同时写入 xAxis 和 xaxis 两个字段，确保前后端兼容） */
    public void setXaxis(List<String> xAxis) {
        this.xAxis = xAxis;
        this.xaxis = xAxis;
    }

    /** 折线数据列表，第一条固定为「总金额」 */
    private List<ReceivableSeries> series;

    /** 当前余额汇总 */
    private ReceivableSummary summary;

    // ─────────────────────────────── 内部类 ───────────────────────────────

    /**
     * 单条折线数据（一种计价类型）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceivableSeries {

        /**
         * 折线名称：总金额 / 总价包干 / 按量结算 / 混合计价
         */
        private String name;

        /**
         * 计价类型标识：TOTAL / PACKAGE / UNIT / MIXED
         */
        private String pricingType;

        /**
         * 与 xAxis 一一对应的累计应收账款余额数组（单位：元）
         * 余额 = 截至该日期所有结算单金额 - 截至该日期所有已收款金额
         */
        private List<BigDecimal> data;

        /**
         * 截至当前最新余额（元）
         */
        private BigDecimal currentBalance;
    }

    /**
     * 当前余额汇总（用于底部卡片展示）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceivableSummary {

        /** 总应收账款余额（元）*/
        private BigDecimal totalReceivable;

        /** 总价包干应收余额（元）*/
        private BigDecimal packageReceivable;

        /** 按量结算应收余额（元）*/
        private BigDecimal unitReceivable;

        /** 混合计价应收余额（元）*/
        private BigDecimal mixedReceivable;
    }

    /**
     * 变动明细 DTO（Mapper 内部查询结果，非对外字段）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceivableChangeDTO {

        /** 日期标签（根据粒度格式化，如 2024-03 / 2024-03-15） */
        private String dateLabel;

        /** 计价类型标识：PACKAGE / UNIT / MIXED */
        private String pricingType;

        /** 本期结算金额合计（应收增加，正数） */
        private BigDecimal settlementAmount;

        /** 本期已收款合计（应收减少，正数） */
        private BigDecimal receivedAmount;
    }

    /**
     * 合同计价类型查询结果 DTO（Mapper 内部使用）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractPricingDTO {

        /** 合同编号（INT PK，优先用于 JOIN） */
        private Integer contractId;

        /** 合同号（VARCHAR，当合同编号为 NULL 时降级匹配） */
        private String contractNo;

        /**
         * 计价类型标识：PACKAGE / UNIT / MIXED
         * 判断规则（基于合同下所有 CONTRACT_ITEM.报价模式）：
         *   - 只有 PACKAGE 条目 → PACKAGE
         *   - 只有 UNIT 条目   → UNIT
         *   - 同时存在         → MIXED
         */
        private String pricingType;
    }
}
