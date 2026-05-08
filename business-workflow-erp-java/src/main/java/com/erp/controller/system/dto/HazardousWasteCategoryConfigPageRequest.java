package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 废物类别限额配置分页查询请求
 */
@Data
public class HazardousWasteCategoryConfigPageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码，默认第1页
     */
    private Long current = 1L;

    /**
     * 每页数量，默认10条
     */
    private Long size = 10L;

    /**
     * 废物类别（模糊查询）
     */
    private String wasteCategory;

    /**
     * 废物类别名称（模糊查询）
     */
    private String wasteCategoryName;

    /**
     * 排序字段（categoryId, wasteCategory, wasteCategoryName）
     */
    private String orderBy;

    /**
     * 排序方向（asc, desc）
     */
    private String orderDirection;
}







