package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 委外处理合同报价组响应
 */
@Data
@ApiModel("委外处理合同报价组响应")
public class OutsourceProcessingContractItemResponse {

    @ApiModelProperty(value = "条目编号")
    private Integer itemId;

    @ApiModelProperty(value = "合同编号")
    private Integer contractId;

    @ApiModelProperty(value = "行号")
    private Integer rowNumber;

    @ApiModelProperty(value = "计价方式")
    private String pricingMode;

    @ApiModelProperty(value = "付款方")
    private String payer;

    @ApiModelProperty(value = "计价方案")
    private String pricingStatement;

    @ApiModelProperty(value = "小计摘要")
    private String subtotalSummary;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "废物明细列表")
    private List<OutsourceProcessingContractWasteItemResponse> wastes;
}
