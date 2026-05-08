package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 危险废物名录分页响应
 *
 * 字段级权限控制已由字段注解迁移为外部配置，此处仅保留字段本身定义。
 */
@Data
public class HazardousWastePageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 条目编号
     */
    private Integer itemId;

    /**
     * 废物类别
     */
    private String wasteCategory;

    /**
     * 废物类别名称
     */
    private String wasteCategoryName;

    /**
     * 行业来源
     */
    private String industrySource;

    /**
     * 废物代码
     */
    private String wasteCode;

    /**
     * 危险废物名称
     */
    private String wasteName;

    /**
     * 危险特性
     */
    private String hazardCharacteristic;

    /**
     * 客户引用数量
     */
    private Long customerCount = 0L;

    /**
     * 报价单引用数量
     */
    private Long quotationCount = 0L;

    /**
     * 入库单引用数量
     */
    private Long warehousingCount = 0L;

    /**
     * 库存引用数量
     */
    private Long stockCount = 0L;

    /**
     * 是否可用
     */
    private Boolean available;

    /**
     * 创建人编码（操作人ID）
     */
    private Integer creatorId;

    /**
     * 创建人姓名
     */
    private String createUserName;
}


