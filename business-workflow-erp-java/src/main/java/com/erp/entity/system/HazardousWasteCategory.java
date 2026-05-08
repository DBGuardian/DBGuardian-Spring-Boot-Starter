package com.erp.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 危险废物类别实体，对应表 HAZARDOUS_WASTE_CATEGORY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("HAZARDOUS_WASTE_CATEGORY")
public class HazardousWasteCategory extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 创建时间 - 数据库表中不存在此字段，排除映射
     */
    @TableField(exist = false)
    private LocalDateTime createTime;

    /**
     * 废物类别编号
     */
    @TableId(value = "废物类别编号", type = IdType.AUTO)
    private Integer categoryId;

    /**
     * 废物类别，如：HW01 医疗废物 / SW01 冶炼废渣
     */
    @TableField("废物类别")
    private String wasteCategory;

    /**
     * 废物类别名称
     */
    @TableField("废物类别名称")
    private String wasteCategoryName;

    /**
     * 限额
     */
    @TableField("限额")
    private BigDecimal limitAmount;

    /**
     * 限额开始时间
     */
    @TableField("限额开始时间")
    private Date limitStartTime;

    /**
     * 限额结束时间
     */
    @TableField("限额结束时间")
    private Date limitEndTime;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Integer creatorId;

    /**
     * 更新人编码
     */
    @TableField("更新人编码")
    private Integer updaterId;

    /**
     * 更新时间
     */
    @TableField("更新时间")
    private Date categoryUpdateTime;
}










