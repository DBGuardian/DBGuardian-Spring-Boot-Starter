package com.erp.entity.production;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime; 

/**
 * 入库单危废明细实体
 * 对应表：WAREHOUSING_WASTE_ITEM
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("WAREHOUSING_WASTE_ITEM")
public class WarehousingWasteItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 入库危废明细编号
     */
    @TableId(value = "入库危废明细编号", type = IdType.AUTO)
    private Integer itemId;

    /**
     * 入库单编号（关联的入库单WAREHOUSING.入库单编号）
     */
    @TableField("入库单编号")
    private Integer warehousingId;

    /**
     * 入库单号（关联的入库单WAREHOUSING.入库单号）
     */
    @TableField(exist = false)
    private String warehousingNo;

    /**
     * 收运通知单明细编号（关联收运通知单明细，便于追溯来源）
     */
    @TableField("收运通知单明细编号")
    private Integer pickupNoticeItemId;

    /**
     * 危废条目编号（引用危废条目，便于统一管理）
     */
    @TableField("危废条目编号")
    private Integer hazardousWasteItemId;

    /**
     * 废物名称（默认沿用收运通知单明细）
     */
    @TableField("废物名称")
    private String wasteName;

    /**
     * 废物代码（默认沿用收运通知单明细）
     */
    @TableField("废物代码")
    private String wasteCode;

    /**
     * 废物形态（固态/液态/气态/半固态等）
     */
    @TableField("废物形态")
    private String form;

    /**
     * 危险特性（易燃、腐蚀、有毒等），继承自收运通知单明细
     */
    @TableField("危险特性")
    private String hazardFeature;

    
    /**
     * 计划收运数量（基本核算数量，吨，-1代表不限量）
     */
    @TableField("计划收运数量")
    private BigDecimal plannedQty;

    /**
     * 基本计量单位（吨/桶/个等），需与合同口径一致
     */
    @TableField("基本计量单位")
    private String measureUnit;

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
     * 辅助单位每基础单位数量（1基本计量单位≈多少辅助单位，例如1吨≈10桶）
     */
    @TableField("辅助单位每基础单位数量")
    private BigDecimal auxPerBase;

    /**
     * 辅助数量（按辅助计量单位表达的数量，通常对应合同中的桶/袋等数量）
     */
    @TableField("辅助数量")
    private BigDecimal auxQuantity;

    /**
     * 实际收运辅助数量（桶/袋等）
     */
    @TableField("实际收运辅助数量")
    private BigDecimal actualAuxQuantity;

    /**
 * 实际入库日期（重写BaseEntity的createTime映射）
 */
@TableField("实际入库日期")
private LocalDateTime createTime;
 
    /**
     * 实际收运数量（吨）
     */
    @TableField("实际收运数量")
    private BigDecimal actualQty;

    /**
     * 差异原因（计划收运数量 vs 实际收运数量）
     */
    @TableField("差异原因")
    private String differenceReason;

    /**
     * 有价类重量（吨，可回收利用部分，保留6位小数）
     */
    @TableField("有价类重量")
    private BigDecimal valuableWeight;

    /**
     * 无价类重量（吨，不可回收利用部分，保留6位小数）
     */
    @TableField("无价类重量")
    private BigDecimal valuelessWeight;
}

