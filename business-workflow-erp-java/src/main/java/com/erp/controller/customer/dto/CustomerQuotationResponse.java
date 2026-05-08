package com.erp.controller.customer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户报价记录响应
 */
@Data
@ApiModel("客户报价记录响应")
public class CustomerQuotationResponse {

    @ApiModelProperty("报价单编号")
    private Integer quotationId;

    @ApiModelProperty("危废类别")
    private String hazardousWasteCategory;

    @ApiModelProperty("危废条目编号")
    private Integer hazardousWasteItemId;

    @ApiModelProperty("危废条目名称")
    private String hazardousWasteItemName;

    @ApiModelProperty("单价")
    private BigDecimal unitPrice;

    @ApiModelProperty("计价方式")
    private String pricingMethod;

    @ApiModelProperty("报价明细")
    private String quotationDetail;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}


