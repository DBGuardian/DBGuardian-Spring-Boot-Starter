package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 初始化账期请求DTO
 */
@Data
@ApiModel("初始化账期请求")
public class FundPeriodInitRequest {

    @ApiModelProperty(value = "组织ID", example = "1", required = true)
    @NotNull(message = "组织ID不能为空")
    private Long organizationId;

    @ApiModelProperty(value = "年份", example = "2026", required = true)
    @NotNull(message = "年份不能为空")
    private Integer year;

    @ApiModelProperty(value = "是否覆盖已存在的账期", example = "false")
    private Boolean overwrite = false;
}



