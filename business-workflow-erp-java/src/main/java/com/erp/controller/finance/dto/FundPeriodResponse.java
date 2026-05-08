package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账期响应DTO
 */
@Data
@ApiModel("账期信息")
public class FundPeriodResponse {

    @ApiModelProperty(value = "账期编号", example = "1")
    private Long periodId;

    @ApiModelProperty(value = "账期编码", example = "202303")
    private String periodCode;

    @ApiModelProperty(value = "年份", example = "2023")
    private Integer year;

    @ApiModelProperty(value = "月份（1-12）", example = "3")
    private Integer month;

    @ApiModelProperty(value = "组织ID", example = "1")
    private Long organizationId;

    @ApiModelProperty(value = "是否已结账", example = "true")
    private Boolean isSettled;

    @ApiModelProperty(value = "结账时间", example = "2023-04-01 10:00:00")
    private LocalDateTime settlementTime;

    @ApiModelProperty(value = "结账人ID", example = "1")
    private Long settlementUserId;
}




