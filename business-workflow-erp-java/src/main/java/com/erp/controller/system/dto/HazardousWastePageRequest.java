package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 危险废物名录分页查询请求
 */
@Data
public class HazardousWastePageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private Long current = 1L;

    /**
     * 每页数量
     */
    private Long size = 10L;

    /**
     * 搜索关键词（废物代码/危险废物/废物类别）
     */
    private String keyword;

    /**
     * 废物代码（模糊匹配）
     */
    private String wasteCode;

    /**
     * 危险废物名称（模糊匹配）
     */
    private String wasteName;

    /**
     * 危险特性筛选
     */
    private String hazardCharacteristic;

    /**
     * 废物类别筛选
     */
    private String wasteCategory;

    /**
     * 废物类别名称筛选
     */
    private String wasteCategoryName;

    /**
     * 行业来源筛选
     */
    private String industrySource;

    /**
     * 是否可用筛选
     */
    private Boolean available;

    /**
     * 排序字段（例如：itemId, wasteCategory, wasteCategoryName, industrySource, wasteCode, wasteName, hazardCharacteristic, available）
     */
    private String orderBy;

    /**
     * 排序方向（asc: 升序, desc: 降序）
     */
    private String orderDirection;

    /**
     * 数据范围过滤（仅导出/查询时使用）：
     * - viewScope=SELF 时，前端传入当前员工ID，后端只返回该员工创建的数据。
     * - viewScope=ALL 或超级管理员时，后端忽略此字段，返回全部数据。
     * 后端 Service 层会对此参数进行安全校验并强制覆盖，防止越权。
     */
    private Integer creatorFilter;
}


