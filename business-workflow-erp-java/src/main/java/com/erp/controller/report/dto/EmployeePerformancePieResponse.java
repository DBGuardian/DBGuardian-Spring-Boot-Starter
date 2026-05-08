package com.erp.controller.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 员工业绩占比饼图 - 响应数据
 *
 * 功能描述：各在职员工在指定维度的业绩数据及占比
 * 返回参数：{ dimension, dimensionLabel, unit, startDate, endDate, fromCache, computedAt, total, list }
 * url地址：/api/report/employee-performance/pie
 * 请求方式：POST
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePerformancePieResponse {

    /** 当前维度枚举值 */
    private String dimension;

    /** 维度中文标签 */
    private String dimensionLabel;

    /** 数值单位（份/吨/元） */
    private String unit;

    /** 实际查询开始日期 */
    private String startDate;

    /** 实际查询结束日期 */
    private String endDate;

    /** 是否来自 Redis 缓存 */
    private boolean fromCache;

    /** 数据计算时间（ISO 8601 格式） */
    private String computedAt;

    /** 所有员工该维度数据总量 */
    private BigDecimal total;

    /** 员工数据列表（已过滤零值，按 value DESC 排序） */
    private List<EmployeePerformanceItem> list;

    // ─────────────────────────────── 内部类 ───────────────────────────────

    /**
     * 单个员工业绩数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeePerformanceItem {

        /** 员工编码 */
        private Integer employeeId;

        /** 员工姓名 */
        private String employeeName;

        /** 员工部门 */
        private String dept;

        /** 该员工的数值（份/吨/元） */
        private BigDecimal value;

        /** 占比百分比（保留2位小数） */
        private BigDecimal percentage;
    }

    /**
     * Mapper 查询结果 DTO（内部使用）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeRawDTO {

        /** 员工编码 */
        private Integer employeeId;

        /** 员工姓名 */
        private String employeeName;

        /** 员工部门 */
        private String dept;

        /** 该员工的原始数值 */
        private BigDecimal value;
    }
}
