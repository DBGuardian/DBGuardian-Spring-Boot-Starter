package com.erp.entity.production;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 转移联单子表实体
 * 对应表：TRANSFER_MANIFEST_ITEM
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("TRANSFER_MANIFEST_ITEM")
public class TransferManifestItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 联单子项主键ID
     */
    @TableId(value = "子项编号", type = IdType.AUTO)
    private Integer itemId;

    /**
     * 所属联单编号（关联 TRANSFER_MANIFEST.联单编号）
     */
    @TableField("联单编号")
    private Integer manifestId;

    // ---- 废物信息 ----

    /**
     * 废物类别
     */
    @TableField("废物类别")
    private String wasteCategory;

    /**
     * 废物代码
     */
    @TableField("废物代码")
    private String wasteCode;

    /**
     * 废物名称
     */
    @TableField("废物名称")
    private String wasteName;

    /**
     * 废物形态（固态/液态/气态/半固态）
     */
    @TableField("废物形态")
    private String wasteForm;

    /**
     * 包装方式（桶装/袋装/散装/箱装）
     */
    @TableField("包装方式")
    private String packagingMethod;

    // ---- 数量信息 ----

    /**
     * 计划转移数量
     */
    @TableField("计划数量")
    private BigDecimal plannedQuantity;

    /**
     * 实际确认数量
     */
    @TableField("确认数量")
    private BigDecimal confirmedQuantity;

    /**
     * 计量单位（吨/千克/升）
     */
    @TableField("计量单位")
    private String unit;

    /**
     * 逻辑删除（0正常 1已删除）
     */
    @TableField("是否删除")
    private Integer isDeleted;
}
