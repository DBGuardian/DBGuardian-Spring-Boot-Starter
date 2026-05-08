package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入库危废明细（含合同匹配信息）VO
 * 用于后端统一处理入库数据与合同数据的匹配
 */
@Data
@ApiModel("入库危废明细（含合同匹配信息）")
public class WarehousingWasteDetailWithContractVO {

    // ==================== 入库明细字段 ====================

    @ApiModelProperty("入库危废明细编号")
    private Integer warehousingWasteItemId;

    @ApiModelProperty("入库单编号")
    private Integer warehousingId;

    @ApiModelProperty("入库单号")
    private String warehousingCode;

    @ApiModelProperty("入库时间")
    private LocalDateTime warehousingTime;

    @ApiModelProperty("废物名称")
    private String wasteName;

    @ApiModelProperty("废物代码")
    private String wasteCode;

    @ApiModelProperty("废物形态")
    private String form;

    @ApiModelProperty("基本计量单位")
    private String basicUnit;

    @ApiModelProperty("是否启用辅助核算")
    private Boolean enableAuxiliaryAccounting;

    @ApiModelProperty("实际收运数量")
    private BigDecimal quantity;

    @ApiModelProperty("实际收运辅助数量")
    private BigDecimal auxiliaryQuantity;

    @ApiModelProperty("辅助计量单位")
    private String auxiliaryUnit;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("入库记录付款方（按量结算备选）")
    private String warehousingPayer;

    // ==================== 合同匹配信息 ====================

    @ApiModelProperty("合同条目编号")
    private Integer contractItemId;

    @ApiModelProperty("合同危废明细编号")
    private Integer contractWasteItemId;

    @ApiModelProperty("报价模式: 总价包干 / 按量结算")
    private String quotationMode;

    @ApiModelProperty("付款方（总价包干来自contract_item，按量结算来自contract_waste_item或warehousing_waste_item）")
    private String payer;

    // ==================== 危废条目信息（通过危废条目编号关联）====================

    @ApiModelProperty("废物类别")
    private String wasteCategory;

    // ==================== 合同危废条目信息 ====================

    @ApiModelProperty("计划转移数量")
    private BigDecimal planQuantity;

    @ApiModelProperty("计量单位")
    private String measurementUnit;

    @ApiModelProperty("单价")
    private BigDecimal unitPrice;

    @ApiModelProperty("金额")
    private BigDecimal amount;

    @ApiModelProperty("计价方案")
    private String pricingPlan;

    @ApiModelProperty("低价备注")
    private String lowPriceRemark;

    @ApiModelProperty("是否启用辅助核算")
    private Boolean auxiliaryEnabled;

    @ApiModelProperty("辅助计量单位")
    private String auxiliaryMeasurementUnit;

    @ApiModelProperty("辅助单位每基础单位数量")
    private BigDecimal auxiliaryPerBasicUnit;

    @ApiModelProperty("辅助数量")
    private BigDecimal auxiliaryQuantity2;

    @ApiModelProperty("辅助单价")
    private BigDecimal auxiliaryUnitPrice;

    // ==================== 已结算信息（总价包干专用）====================

    @ApiModelProperty("已结算基本入库量")
    private BigDecimal settledBasicQuantity;

    @ApiModelProperty("已结算辅助入库量")
    private BigDecimal settledAuxiliaryQuantity;

    // ==================== 匹配状态 ====================

    @ApiModelProperty("是否匹配成功")
    private Boolean matched;

    @ApiModelProperty("匹配信息/失败原因")
    private String matchMessage;
}
