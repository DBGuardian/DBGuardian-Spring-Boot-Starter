package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 嵌套的合同条目DTO
 */
@Data
@ApiModel("嵌套的合同条目")
public class ContractNestedItemDTO {

    @ApiModelProperty("合同条目编号")
    private Integer contractItemId;

    @ApiModelProperty("报价条目编号")
    private Integer quotationItemId;

    @ApiModelProperty("报价模式：PACKAGE(总价包干)/UNIT(按量结算)")
    private String quotationMode;

    @ApiModelProperty("付款方：甲方/乙方")
    private String payer;

    @ApiModelProperty("计价方案")
    private String pricingPlan;

    @ApiModelProperty("低价备注")
    private String floorPriceRemark;

    @ApiModelProperty("小计摘要")
    private String subtotalSummary;

    @ApiModelProperty("危废明细列表")
    private List<WasteItemDetailDTO> wasteItemDetails;

    public List<WasteItemDetailDTO> getWasteItemDetails() {
        return wasteItemDetails != null ? wasteItemDetails : Collections.emptyList();
    }

    @Data
    @ApiModel("危废明细")
    public static class WasteItemDetailDTO {
        @ApiModelProperty("合同危废明细编号")
        private Integer contractWasteItemId;

        @ApiModelProperty("报价危废明细编号")
        private Integer quotationWasteItemId;

        @ApiModelProperty("危废条目编号")
        private Integer hazardousWasteItemId;

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

        @ApiModelProperty("付款方")
        private String payer;

        @ApiModelProperty("计价方案")
        private String pricingPlan;

        @ApiModelProperty("低价备注")
        private String floorPriceRemark;
    }
}
