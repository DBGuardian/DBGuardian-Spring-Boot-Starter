package com.erp.entity.production;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 收运通知单危废明细实体
 *
 * 对应表：PICKUP_NOTICE_ITEM
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("PICKUP_NOTICE_ITEM")
public class PickupNoticeItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 转移废物明细编号
     */
    @TableId(value = "转移废物明细编号", type = IdType.AUTO)
    private Integer itemId;

    /**
     * 收运通知单号
     */
    @TableField("收运通知单号")
    private String noticeCode;

    /**
     * 合同危废明细编号
     */
    @TableField(value = "合同危废明细编号", updateStrategy = FieldStrategy.IGNORED)
    private Integer contractWasteItemId;

    /**
     * 危废条目编号
     */
    @TableField("危废条目编号")
    private Integer hazardousWasteItemId;

    /**
     * 废物名称
     */
    @TableField("废物名称")
    private String wasteName;

    /**
     * 废物代码
     */
    @TableField("废物代码")
    private String wasteCode;

    /**
     * 危险特性
     */
    @TableField("危险特性")
    private String hazardFeature;

    /**
     * 废物形态
     */
    @TableField("废物形态")
    private String form;

    /**
     * 有害成分成分名称
     */
    @TableField("有害成分名称")
    private String hazardousComponentName;

    /**
     * 包装方式
     */
    @TableField(exist = false)
    private String packageType;

    /**
     * 包装数量
     */
    @TableField(exist = false)
    private BigDecimal packageQty;

    /**
     * 计划转移数量（吨，-1代表不限量）
     */
    @TableField("计划转移数量")
    private BigDecimal plannedQtyTon;

    /**
     * 是否启用辅助核算
     */
    @TableField("是否启用辅助核算")
    private Boolean enableAuxiliaryAccounting;

    /**
     * 辅助计量单位（业务友好展示单位，如桶/袋/车等）
     */
    @TableField("辅助计量单位")
    private String auxUnit;

    /**
     * 辅助单位每基础单位数量（1计量单位≈多少辅助单位，例如1吨≈10桶）
     */
    @TableField("辅助单位每基础单位数量")
    private BigDecimal auxPerBase;

    /**
     * 辅助数量（按辅助计量单位表达的数量，通常对应合同中的桶/袋等数量）
     */
    @TableField("辅助数量")
    private BigDecimal auxQuantity;

    /**
     * 基本计量单位（吨/桶/个等），需与合同口径一致
     */
    @TableField("基本计量单位")
    private String measureUnit;

    /**
     * 辅助单位数量（兼容旧字段，映射到 auxQuantity）
     * 注意：数据库表中没有此字段，但前端可能使用
     */
    @TableField(exist = false)
    private BigDecimal auxUnitQty;

    /**
     * 危废类别编码（通过关联危废名录和类别表查询获得）
     */
    @TableField(exist = false)
    private String wasteCategory;
}

