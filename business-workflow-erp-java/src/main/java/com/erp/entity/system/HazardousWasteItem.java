package com.erp.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 危险废物名录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("HAZARDOUS_WASTE_ITEM")
public class HazardousWasteItem extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 条目编号
     */
    @TableId(value = "条目编号", type = IdType.AUTO)
    private Integer itemId;

    /**
     * 废物类别编号
     */
    @TableField("废物类别编号")
    private Integer categoryId;

    /**
     * 行业来源
     */
    @TableField("行业来源")
    private String industrySource;

    /**
     * 废物代码
     */
    @TableField("废物代码")
    private String wasteCode;

    /**
     * 危险废物名称
     */
    @TableField("危险废物")
    private String wasteName;

    /**
     * 危险特性
     */
    @TableField("危险特性")
    private String hazardCharacteristic;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Integer creatorId;

    /**
     * 创建人姓名（来自EMPLOYEE表，数据库不存储）
     */
    @TableField(exist = false)
    private String creatorName;

    /**
     * 是否可用
     */
    @TableField("是否可用")
    private Boolean available;

    /**
     * 废物类别编码（存在类别表中，响应使用，数据库不存储）
     */
    @TableField(exist = false)
    private String wasteCategory;

    /**
     * 废物类别名称（存在类别表中，响应使用，数据库不存储）
     */
    @TableField(exist = false)
    private String wasteCategoryName;

    /**
     * 创建时间 - 数据库表中不存在此字段，排除映射
     */
    @TableField(exist = false)
    private LocalDateTime createTime;


    /**
     * 更新时间 - 数据库表中不存在此字段，排除映射
     */
    @TableField(exist = false)
    private LocalDateTime updateTime;
}


