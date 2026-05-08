package com.erp.entity.settlement;

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
 * 业务结算明细表实体
 *
 * 对应表：BUSINESS_FEE_ITEM
 * 变更说明（2026-04-01）：
 *   - 删除 settlementId 冗余字段
 *   - 删除 wasteCode/wasteName/wasteCategory，危废信息统一存于 BUSINESS_FEE_ITEM_WASTE_INFO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("BUSINESS_FEE_ITEM")
public class BusinessFeeItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "明细序号", type = IdType.AUTO)
    private Integer itemSeq;

    @TableField("业务序号")
    private Integer businessSeq;

    @TableField("收付款方向")
    private String paymentDirection;

    @TableField("结算模式")
    private String settlementMode;

    @TableField("底价单价")
    private BigDecimal baseUnitPrice;

    @TableField("有价类单价")
    private BigDecimal valuableUnitPrice;

    @TableField("无价类单价")
    private BigDecimal worthlessUnitPrice;

    @TableField("合同底价")
    private BigDecimal contractBasePrice;

    @TableField("有价类合同底价")
    private BigDecimal valuableContractBasePrice;

    @TableField("无价类合同底价")
    private BigDecimal worthlessContractBasePrice;

    @TableField("中间费")
    private BigDecimal intermediaryFee;

    @TableField("返点比例")
    private BigDecimal rebateRatio;

    @TableField("应付金额")
    private BigDecimal payableAmount;

    @TableField("有价类应付金额")
    private BigDecimal valuablePayableAmount;

    @TableField("无价类应付金额")
    private BigDecimal worthlessPayableAmount;

    @TableField("有价重量")
    private BigDecimal valuableWeight;

    @TableField("无价重量")
    private BigDecimal worthlessWeight;

    @TableField("货款结算金额")
    private BigDecimal cargoSettlementAmount;

    @TableField("是否启用辅助核算")
    private Boolean enableAuxAccounting;

    @TableField("基本核算数量")
    private BigDecimal basicQuantity;

    @TableField("辅助核算数量")
    private BigDecimal auxiliaryQuantity;

    @TableField("辅助计量单位")
    private String auxiliaryUnit;

    @TableField("创建人编码")
    private Integer creatorId;

    @TableField("创建时间")
    private LocalDateTime createTime;

    @TableField("更新人编码")
    private Integer updaterId;

    @TableField("更新时间")
    private LocalDateTime updateTime;

    @TableField("version")
    private Integer version;
}
