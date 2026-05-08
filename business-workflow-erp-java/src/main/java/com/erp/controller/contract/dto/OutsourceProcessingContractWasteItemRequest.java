package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 委外处理合同废物明细请求
 */
@Data
@ApiModel("委外处理合同废物明细请求")
public class OutsourceProcessingContractWasteItemRequest {

    @ApiModelProperty(value = "废物明细编号（更新时传入）")
    private Integer wasteItemId;

    @ApiModelProperty(value = "行内顺序")
    private Integer rowOrder = 1;

    @ApiModelProperty(value = "危废条目编号")
    private Integer hazardousWasteItemId;

    @ApiModelProperty(value = "废物类别（如：HW01）")
    private String wasteCategory;

    @ApiModelProperty(value = "废物代码")
    private String wasteCode;

    @ApiModelProperty(value = "废物名称")
    private String wasteName;

    @ApiModelProperty(value = "废物形态（固态/液态/气态/半固态）")
    private String wasteState;

    @ApiModelProperty(value = "计划数量")
    private BigDecimal plannedQuantity;

    @ApiModelProperty(value = "是否不限量")
    private Boolean unlimitedQuantity = false;

    @ApiModelProperty(value = "计量单位")
    private String quantityUnit;

    // ========== 辅助计量单位 ==========

    @ApiModelProperty(value = "是否启用辅助核算")
    private Boolean enableAuxiliaryAccounting = false;

    @ApiModelProperty(value = "辅助计量单位")
    private String auxUnit;

    @ApiModelProperty(value = "辅助单位每基础单位数量")
    private BigDecimal auxPerBase;

    @ApiModelProperty(value = "辅助数量")
    private BigDecimal auxQuantity;

    @ApiModelProperty(value = "辅助单价")
    private BigDecimal auxUnitPrice;

    // ========== 价格信息 ==========

    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "超量单价")
    private BigDecimal overLimitPrice;

    @ApiModelProperty(value = "超量单位")
    private String overLimitUnit;

    @ApiModelProperty(value = "底价")
    private String floorPrice;

    @ApiModelProperty(value = "底价单位")
    private String floorPriceUnit;

    @ApiModelProperty(value = "计价方案描述")
    private String pricingStatement;

    @ApiModelProperty(value = "付款方（甲方/乙方）")
    private String payer;

    @ApiModelProperty(value = "备注")
    private String remark;
}
