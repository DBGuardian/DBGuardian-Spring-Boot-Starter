package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 入库单统计信息
 */
@Data
@ApiModel("入库单统计信息")
public class WarehousingStat {

    @ApiModelProperty("标签")
    private String label;

    @ApiModelProperty("值")
    private String value;

    @ApiModelProperty("颜色：primary/success/warning/danger/info")
    private String color;
}

