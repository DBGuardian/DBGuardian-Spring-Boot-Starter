package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 反结账响应DTO
 */
@Data
@ApiModel("反结账响应")
public class SettlementReverseResponse {

    @ApiModelProperty(value = "组织ID", example = "1")
    private Long organizationId;

    @ApiModelProperty(value = "反结账是否成功", example = "true")
    private Boolean reverseSuccess;
}

