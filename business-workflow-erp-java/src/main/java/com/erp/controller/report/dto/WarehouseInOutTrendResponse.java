package com.erp.controller.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 仓库出入库数量变动趋势 - 响应数据
 *
 * 功能描述：入库/出库危废重量随时间变动的柱线混合图数据
 * 返回参数：{ startDate, endDate, granularity, fromCache, computedAt, xAxis, series, summary }
 * url地址：/api/report/warehouse/in-out-trend
 * 请求方式：POST
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseInOutTrendResponse {

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

    /** 横轴日期标签数组，按时间升序 */
    private List<String> xAxis;

    /** 兼容字段：与 xAxis 相同，供前端 xaxis（小写）读取 */
    private List<String> xaxis;

    /** 设置横轴数据（同时写入 xAxis 和 xaxis 两个字段，确保前后端兼容） */
    public void setXAxis(List<String> xAxis) {
        this.xAxis = xAxis;
        this.xaxis = xAxis;
    }

    /** 系列数据，固定 3 条（顺序：IN / OUT / TOTAL） */
    private List<WarehouseInOutSeries> series;

    /** 汇总统计 */
    private WarehouseInOutSummary summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseInOutSeries {

        /** 系列名称：入库 / 出库 / 总量 */
        private String name;

        /** 类型标识：IN / OUT / TOTAL */
        private String type;

        /** 与 xAxis 一一对应的重量（吨，保留 3 位小数；无数据为 0） */
        private List<BigDecimal> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseInOutSummary {

        /** 查询区间内入库总重量（吨） */
        private BigDecimal totalInWeight;

        /** 查询区间内出库总重量（吨） */
        private BigDecimal totalOutWeight;

        /** 查询区间内出入库总量（吨） */
        private BigDecimal totalMovementWeight;

        /** 最新时间节点入库重量（吨） */
        private BigDecimal currentPeriodInWeight;

        /** 最新时间节点出库重量（吨） */
        private BigDecimal currentPeriodOutWeight;

        /** 最新时间节点出入库总量（吨） */
        private BigDecimal currentPeriodMovementWeight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseInOutCountDTO {

        /** 时间粒度标签（如 2026-03 / 2026-03-17 / 2026） */
        private String dateLabel;

        /** 本期入库总重量（吨） */
        private BigDecimal inWeight;

        /** 本期出库总重量（吨） */
        private BigDecimal outWeight;
    }
}
