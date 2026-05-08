package com.erp.controller.customer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户报价记录层级响应
 */
@Data
@ApiModel("客户报价记录层级响应")
public class CustomerQuotationHierarchicalResponse {

    @ApiModelProperty("报价单编号")
    private Integer quotationId;

    @ApiModelProperty("报价单号")
    private String quotationNo;

    @ApiModelProperty("报价状态")
    private String quotationStatus;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("报价条目列表")
    private List<QuotationItemResponse> items;

    /**
     * 报价条目响应
     */
    @Data
    @ApiModel("报价条目响应")
    public static class QuotationItemResponse {

        @ApiModelProperty("报价条目编号")
        private Integer quotationItemId;

        @ApiModelProperty("报价模式：总价包干/按量结算")
        private String quotationMode;

        @ApiModelProperty("付款方：甲方/乙方/共同")
        private String payer;

        @ApiModelProperty("计价方案（总价包干时使用）")
        private String pricingPlan;

        @ApiModelProperty("备注（总价包干时使用）")
        private String remark;

        @ApiModelProperty("危废条目明细列表")
        private List<QuotationWasteItemResponse> wasteItems;
    }

    /**
     * 报价危废条目明细响应
     */
    @Data
    @ApiModel("报价危废条目明细响应")
    public static class QuotationWasteItemResponse {

        @ApiModelProperty("报价危废明细编号")
        private Integer quotationWasteItemId;

        @ApiModelProperty("危废条目编号")
        private Integer hazardousWasteItemId;

        @ApiModelProperty("废物类别")
        private String wasteCategory;

        @ApiModelProperty("行业来源")
        private String industrySource;

        @ApiModelProperty("废物代码")
        private String wasteCode;

        @ApiModelProperty("危险废物名称")
        private String hazardousWaste;

        @ApiModelProperty("形态")
        private String form;

        @ApiModelProperty("计量单位")
        private String unit;

        @ApiModelProperty("计划数量")
        private BigDecimal plannedQuantity;

        @ApiModelProperty("付款方（按量结算时使用）")
        private String payer;

        @ApiModelProperty("计价方案（按量结算时使用）")
        private String pricingPlan;

        @ApiModelProperty("备注（按量结算时使用）")
        private String remark;

        @ApiModelProperty("是否启用辅助核算")
        private Boolean enableAuxiliaryAccounting;

        @ApiModelProperty("辅助计量单位")
        private String auxUnit;

        @ApiModelProperty("辅助单位每基础单位数量")
        private BigDecimal auxPerBase;

        @ApiModelProperty("辅助数量")
        private BigDecimal auxQuantity;

        @ApiModelProperty("辅助单价")
        private BigDecimal auxUnitPrice;

        @ApiModelProperty("低价备注")
        private String floorPriceRemark;
    }
}





















