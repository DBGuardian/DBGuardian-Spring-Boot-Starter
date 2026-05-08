package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 总磅单统计信息
 */
@Data
@ApiModel("总磅单统计信息")
public class WeighingSlipStat {

    @ApiModelProperty("标签")
    private String label;

    @ApiModelProperty("值")
    private String value;

    @ApiModelProperty("颜色类型")
    private String color;
}

