package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同危废条目DTO
 */
@Data
@ApiModel("合同危废条目")
public class ContractWasteItemDTO {

    @ApiModelProperty("合同危废明细编号")
    private Integer contractWasteItemId;

    @ApiModelProperty("合同条目编号")
    private Integer contractItemId;

    @ApiModelProperty("危废条目编号")
    private Integer hazardousWasteItemId;

    @ApiModelProperty("报价危废明细编号")
    private Integer quotationWasteItemId;

    @ApiModelProperty("废物代码")
    private String wasteCode;

    @ApiModelProperty("废物名称（危废或固废名称）")
    private String hazardousWaste;

    @ApiModelProperty("废物形态")
    private String wasteForm;

    @ApiModelProperty("废物类别")
    private String wasteCategory;

    @ApiModelProperty("计划转移数量")
    private BigDecimal plannedQuantity;

    @ApiModelProperty("计量单位")
    private String unit;

    @ApiModelProperty("是否启用辅助核算")
    private Boolean enableAuxiliaryAccounting;

    @ApiModelProperty("辅助计量单位（业务友好展示单位，如桶/袋/车等）")
    private String auxUnit;

    @ApiModelProperty("辅助单位每基础单位数量（1基础计量单位≈多少辅助单位，例如1吨≈10桶）")
    private BigDecimal auxPerBase;

    @ApiModelProperty("按辅助计量单位表达的数量，通常对应页面上的桶/袋等数量")
    private BigDecimal auxQuantity;

    @ApiModelProperty("辅助计价单价（元/辅助计量单位，如元/桶）")
    private BigDecimal auxUnitPrice;

    @ApiModelProperty("单价")
    private BigDecimal unitPrice;

    @ApiModelProperty("金额")
    private BigDecimal amount;

    @ApiModelProperty("付款方：甲方/乙方")
    private String payer;

    @ApiModelProperty("计价方案")
    private String pricingPlan;

    @ApiModelProperty("低价备注")
    private String floorPriceRemark;

    // 结算模式相关字段
    @ApiModelProperty("结算模式：按量结算/总价包干")
    private String settlementMode;

    // 总价包干相关字段
    @ApiModelProperty("包干总价")
    private BigDecimal lumpSumAmount;

    @ApiModelProperty("包干单位：次/年/月等")
    private String lumpSumUnit;

    @ApiModelProperty("超量单价")
    private BigDecimal excessUnitPrice;

    @ApiModelProperty("备注")
    private String remark;
}
