package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 反结账请求DTO
 */
@Data
@ApiModel("反结账请求")
public class SettlementReverseRequest {

    @ApiModelProperty(value = "组织ID", required = true, example = "1")
    @NotNull(message = "组织ID不能为空")
    private Long organizationId;

    @ApiModelProperty(value = "账期ID（用于单个账期反结账）", required = false, example = "61")
    private Long periodId;
}

