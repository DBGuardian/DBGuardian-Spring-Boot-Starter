package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 创建单个账期请求DTO
 */
@Data
@ApiModel("创建单个账期请求")
public class FundPeriodCreateRequest {

    @ApiModelProperty(value = "年份", example = "2026", required = true)
    @NotNull(message = "年份不能为空")
    private Integer year;

    @ApiModelProperty(value = "月份（1-12）", example = "1", required = true)
    @NotNull(message = "月份不能为空")
    @Min(value = 1, message = "月份必须在1-12之间")
    @Max(value = 12, message = "月份必须在1-12之间")
    private Integer month;
}

