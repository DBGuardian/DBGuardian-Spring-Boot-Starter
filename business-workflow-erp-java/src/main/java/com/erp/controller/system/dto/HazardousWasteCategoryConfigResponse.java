package com.erp.controller.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 危险废物类别限额配置响应
 *
 * 字段级权限控制已由字段注解迁移为外部配置，此处仅保留字段本身定义。
 */
@Data
public class HazardousWasteCategoryConfigResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 废物类别编号
     */
    private Integer categoryId;

    /**
     * 废物类别
     */
    private String wasteCategory;

    /**
     * 废物类别名称
     */
    private String wasteCategoryName;

    /**
     * 创建人编码（操作人ID）
     */
    private Integer creatorId;

    /**
     * 创建人姓名（操作人）
     */
    private String createUserName;

    /**
     * 限额
     */
    private BigDecimal limitAmount;

    /**
     * 限额开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date limitStartTime;

    /**
     * 限额结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date limitEndTime;

    /**
     * 该类别下的危废条目数量
     */
    private Long wasteItemCount;
}






