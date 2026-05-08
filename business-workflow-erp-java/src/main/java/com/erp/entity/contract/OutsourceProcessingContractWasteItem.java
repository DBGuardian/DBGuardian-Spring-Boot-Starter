package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 委外处理合同废物明细实体
 *
 * 对应表：OUTSOURCE_PROCESSING_CONTRACT_WASTE_ITEM
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("OUTSOURCE_PROCESSING_CONTRACT_WASTE_ITEM")
public class OutsourceProcessingContractWasteItem extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 废物明细编号
     */
    @TableId(value = "废物明细编号", type = IdType.AUTO)
    private Integer wasteItemId;

    /**
     * 条目编号
     */
    @TableField("条目编号")
    private Integer itemId;

    /**
     * 行内顺序
     */
    @TableField("行内顺序")
    private Integer rowOrder;

    /**
     * 危废条目编号（关联 HAZARDOUS_WASTE_ITEM.条目编号）
     */
    @TableField("危废条目编号")
    private Integer hazardousWasteItemId;

    /**
     * 废物类别（如：HW01）
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
    private String wasteState;

    /**
     * 计划数量
     */
    @TableField("计划数量")
    private BigDecimal plannedQuantity;

    /**
     * 是否不限量
     */
    @TableField("是否不限量")
    private Boolean unlimitedQuantity;

    /**
     * 计量单位
     */
    @TableField("计量单位")
    private String quantityUnit;

    // ========== 辅助计量单位 ==========

    /**
     * 是否启用辅助核算
     */
    @TableField("是否启用辅助核算")
    private Boolean enableAuxiliaryAccounting;

    /**
     * 辅助计量单位
     */
    @TableField("辅助计量单位")
    private String auxUnit;

    /**
     * 辅助单位每基础单位数量
     */
    @TableField("辅助单位每基础单位数量")
    private BigDecimal auxPerBase;

    /**
     * 辅助数量
     */
    @TableField("辅助数量")
    private BigDecimal auxQuantity;

    /**
     * 辅助单价
     */
    @TableField("辅助单价")
    private BigDecimal auxUnitPrice;

    // ========== 价格信息 ==========

    /**
     * 单价
     */
    @TableField("单价")
    private BigDecimal unitPrice;

    /**
     * 超量单价
     */
    @TableField("超量单价")
    private BigDecimal overLimitPrice;

    /**
     * 超量单位
     */
    @TableField("超量单位")
    private String overLimitUnit;

    /**
     * 底价
     */
    @TableField("底价")
    private String floorPrice;

    /**
     * 底价单位
     */
    @TableField("底价单位")
    private String floorPriceUnit;

    // ========== 计价信息 ==========

    /**
     * 计价方案描述
     */
    @TableField("计价方案")
    private String pricingStatement;

    /**
     * 付款方（甲方/乙方）
     */
    @TableField("付款方")
    private String payer;

    // ========== 小计信息 ==========

    /**
     * 小计单位
     */
    @TableField("小计单位")
    private String subtotalUnit;

    /**
     * 小计数量
     */
    @TableField("小计数量")
    private BigDecimal subtotalQuantity;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 是否删除
     */
    @TableField("是否删除")
    private Boolean isDeleted;
}
