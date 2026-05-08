package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 已结算量DTO
 * 用于存储根据合同号和废物代码查询的已结算量
 */
@Data
@ApiModel("已结算量")
public class SettledQuantityDTO {

    @ApiModelProperty("已结算基本入库量")
    private BigDecimal settledBasicQuantity;

    @ApiModelProperty("已结算辅助入库量")
    private BigDecimal settledAuxiliaryQuantity;
}
