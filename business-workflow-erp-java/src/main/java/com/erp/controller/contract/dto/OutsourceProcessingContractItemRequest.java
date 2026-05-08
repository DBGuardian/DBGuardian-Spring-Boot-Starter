package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

/**
 * 委外处理合同报价组请求
 */
@Data
@ApiModel("委外处理合同报价组请求")
public class OutsourceProcessingContractItemRequest {

    @ApiModelProperty(value = "条目编号（更新时传入）")
    private Integer itemId;

    @ApiModelProperty(value = "行号")
    private Integer rowNumber = 1;

    @ApiModelProperty(value = "计价方式（UNIT-按量结算，PACKAGE-总价包干）")
    private String pricingMode;

    @ApiModelProperty(value = "付款方（甲方/乙方）")
    private String payer;

    @ApiModelProperty(value = "计价方案描述")
    private String pricingStatement;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "废物明细列表")
    @Valid
    private List<OutsourceProcessingContractWasteItemRequest> wastes;
}
