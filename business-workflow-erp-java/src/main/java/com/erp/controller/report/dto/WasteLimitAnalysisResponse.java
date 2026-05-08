package com.erp.controller.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 危险废物限额分析 - 响应数据
 *
 * 功能描述：返回每个废物类别的限额、合同签订量、实际收运量、联单确认量
 * 返回参数：{ startDate, endDate, fromCache, computedAt, categories, summary }
 * url地址：/api/report/waste/limit-analysis
 * 请求方式：POST
 *
 * 前端使用说明：
 * - categories 数组已按 limit 升序排序，图表直接使用即可
 * - 堆叠条形图通过 categories[i].categoryName 作为横坐标
 * - 通过 categories[i].limit / pending / manifest 提取各系列数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WasteLimitAnalysisResponse {

    /** 实际查询开始日期 */
    private String startDate;

    /** 实际查询结束日期 */
    private String endDate;

    /** 是否来自 Redis 缓存 */
    private boolean fromCache;

    /** 数据计算时间（ISO 8601 格式） */
    private String computedAt;

    /** 废物类别列表（按限额升序排列） */
    private List<WasteCategoryDTO> categories;

    /**
     * 系列数据数组，与 categories 一一对应
     * 每个元素包含该类别的三个指标：limit（限额）、pending（合同未收运）、manifest（已生成联单）
     * 前端通过 seriesData.map(s => s.limit) 等提取各系列数据
     */
    private List<WasteLimitSeries> series;

    /** 汇总统计 */
    private WasteLimitSummary summary;

    // ─────────────────────────────── 内部类 ───────────────────────────────

    /**
     * 废物类别数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WasteCategoryDTO {

        /** 废物类别编号 */
        private Integer categoryId;

        /** 废物类别编码（如 HW01、SW01） */
        private String categoryCode;

        /** 废物类别名称（如医疗废物、冶炼废渣） */
        private String categoryName;

        /** 限额量（吨，null 表示无限额） */
        private Double wasteLimit;

        /** 限额开始时间 */
        private String limitStartDate;

        /** 限额结束时间 */
        private String limitEndDate;

        /** 合同签订量（计划转移数量，吨） */
        private Double planned;

        /** 实际收运量（实际入库数量，吨） */
        private Double actual;

        /** 合同未收运量 = planned - actual（吨） */
        private Double pending;

        /** 已生成联单量（确认数量，吨） */
        private Double manifest;

        /** 限额使用率（百分比，actual / limit * 100，无限额时显示 0） */
        private Double usageRate;
    }

    /**
     * 汇总统计数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WasteLimitSummary {

        /** 涉及废物类别总数 */
        private Integer totalCategories;

        /** 总限额量（吨） */
        private Double totalLimit;

        /** 总合同签订量（吨） */
        private Double totalPlanned;

        /** 总实际收运量（吨） */
        private Double totalActual;

        /** 总合同未收运量（吨） */
        private Double totalPending;

        /** 总已生成联单量（吨） */
        private Double totalManifest;

        /** 总限额使用率（百分比） */
        private Double limitUsageRate;
    }

    /**
     * 系列数据（单个废物类别的三个指标）
     * 用于ECharts堆叠条形图的series配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WasteLimitSeries {

        /** 限额量（吨） */
        private Double wasteLimit;

        /** 合同未收运量（吨） */
        private Double pending;

        /** 已生成联单量（吨） */
        private Double manifest;
    }
}
