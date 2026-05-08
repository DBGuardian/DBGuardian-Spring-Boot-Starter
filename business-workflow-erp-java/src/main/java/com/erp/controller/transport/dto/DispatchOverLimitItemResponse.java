package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 超限提醒条目
 */
@Data
@ApiModel("运输单超限条目")
public class DispatchOverLimitItemResponse {

    @ApiModelProperty("危废名称")
    private String wasteName;

    @ApiModelProperty("危废代码")
    private String wasteCode;

    @ApiModelProperty("计划数量(吨)")
    private Double plannedQtyTon;

    @ApiModelProperty("限额数量(吨)")
    private Double limitQtyTon;

    @ApiModelProperty("超出数量(吨)")
    private Double exceedQtyTon;
}


