package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 委外处理合同价外服务响应
 */
@Data
@ApiModel("委外处理合同价外服务响应")
public class OutsourceProcessingContractOutOfScopeServiceResponse {

    @ApiModelProperty(value = "价外费用编号")
    private Integer outOfScopeServiceId;

    @ApiModelProperty(value = "服务类型")
    private String serviceType;

    @ApiModelProperty(value = "规格型号")
    private String spec;

    @ApiModelProperty(value = "单位")
    private String unit;

    @ApiModelProperty(value = "计划数量")
    private BigDecimal plannedQuantity;

    @ApiModelProperty(value = "合同单价")
    private BigDecimal contractUnitPrice;
}
