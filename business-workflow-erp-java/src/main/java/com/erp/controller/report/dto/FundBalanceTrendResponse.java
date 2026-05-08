package com.erp.controller.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资金余额变动趋势 - 响应数据
 *
 * 功能描述：管理看板资金余额变动折线图响应，包含横轴日期、多条折线序列及账户余额摘要
 * 返回参数：{ dateRange, startDate, endDate, fromCache, computedAt, xAxis, series, accounts, totalCurrentBalance }
 * url地址：/api/report/fund/balance-trend
 * 请求方式：POST
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundBalanceTrendResponse {

    /** 请求的时间维度 */
    private String dateRange;

    /** 实际查询开始日期 */
    private String startDate;

    /** 实际查询结束日期 */
    private String endDate;

    /** 是否来自缓存 */
    private Boolean fromCache;

    /** 数据计算时间（ISO 8601 格式） */
    private String computedAt;

    /** 横轴日期标签数组，按时间升序 */
    private List<String> xAxis;

    /** 折线数据列表，第一条固定为总余额 */
    private List<BalanceTrendSeries> series;

    /** 各账户当前最新余额摘要 */
    private List<AccountBalanceSummary> accounts;

    /** 所有账户余额合计（元） */
    private BigDecimal totalCurrentBalance;

    // ─────────────────────────────── 内部类 ───────────────────────────────

    /**
     * 单条折线数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceTrendSeries {
        /** 账户ID，0 表示总余额合计 */
        private Long accountId;
        /** 账户名称 */
        private String accountName;
        /** 账户类型：TOTAL / BANK / PETTY_CASH / CASH */
        private String accountType;
        /** 建议颜色，总余额固定 #409eff，其余由前端分配 */
        private String color;
        /** 与 xAxis 一一对应的余额快照数组（单位：元） */
        private List<BigDecimal> data;
        /**
         * today 模式专用：与 xAxis 一一对应的交易详情
         * index 0 为起始余额（null），index 1+ 对应每笔交易
         */
        private List<FundTransaction> transactions;
    }

    /**
     * 账户余额摘要（用于底部卡片展示）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountBalanceSummary {
        /** 账户ID */
        private Long accountId;
        /** 账户名称 */
        private String accountName;
        /** 账户类型：BANK / PETTY_CASH / CASH */
        private String accountType;
        /** 当前最新余额（元） */
        private BigDecimal currentBalance;
    }

    /**
     * today 模式下每笔交易信息（用于前端 tooltip 展示）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundTransaction {
        /** 交易时间（HH:mm:ss） */
        private String time;
        /** 交易金额（正=收入，负=支出，单位：元） */
        private BigDecimal amount;
        /** 交易后总余额（单位：元） */
        private BigDecimal balance;
        /** 交易摘要/备注 */
        private String remark;
        /** 交易类型：INCOME=收入 EXPENSE=支出 */
        private String type;
        /**
         * 是否为起始/占位点（非真实交易，用于替代 null 避免 Jackson non_null 过滤）
         * true = 起始点或截止当前占位点，false / 不存在 = 真实交易
         */
        private Boolean startPoint;
    }

    /**
     * Mapper 查询结果 DTO（内部使用）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayFlowDTO {
        /** 账户ID */
        private Long accountId;
        /** 交易日期（YYYY-MM-DD） */
        private String transactionDate;
        /** 当日收入合计 */
        private BigDecimal dayIncome;
        /** 当日支出合计 */
        private BigDecimal dayExpenditure;
    }

    /**
     * 账户期初余额查询结果 DTO（内部使用）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountInitialDTO {
        /** 账户ID */
        private Long accountId;
        /** 期初余额 */
        private BigDecimal initialBalance;
        /** 账期开始日期 */
        private String periodStart;
    }

    /**
     * 按年查询余额结果 DTO（year 维度专用）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearlyBalanceDTO {
        /** 账户ID */
        private Long accountId;
        /** 最后已结账月的下一个月期初余额 */
        private BigDecimal initialBalance;
        /** 对应的原始年份（即已结账账期所在年份） */
        private Integer year;
        /** 该年最后已结账的月份 */
        private Integer closedMonth;
    }

    /**
     * 按笔交易查询结果 DTO（today 模式专用）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionFlowDTO {
        /** 账户ID */
        private Long accountId;
        /** 交易时间（HH:mm:ss） */
        private String transactionTime;
        /** 交易金额（始终为正数） */
        private BigDecimal amount;
        /** 交易类型：INCOME / EXPENDITURE */
        private String transactionType;
        /** 交易备注 */
        private String remark;
    }
}
