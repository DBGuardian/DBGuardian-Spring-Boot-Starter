package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算统计信息
 */
@Data
@ApiModel("结算统计信息")
public class SettlementStatisticsDTO {

    @ApiModelProperty("结算单总数")
    private Long totalCount;

    @ApiModelProperty("应收总金额")
    private BigDecimal receivableTotal;

    @ApiModelProperty("应收已收金额")
    private BigDecimal receivablePaid;

    @ApiModelProperty("应收未收金额")
    private BigDecimal receivableUnpaid;

    @ApiModelProperty("应付总金额")
    private BigDecimal payableTotal;

    @ApiModelProperty("应付已付金额")
    private BigDecimal payablePaid;

    @ApiModelProperty("应付未付金额")
    private BigDecimal payableUnpaid;
}
