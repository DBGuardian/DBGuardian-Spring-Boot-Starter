package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 初始化账期响应DTO
 */
@Data
@ApiModel("初始化账期响应")
public class FundPeriodInitResponse {

    @ApiModelProperty(value = "年份", example = "2026")
    private Integer year;

    @ApiModelProperty(value = "组织ID", example = "1")
    private Long organizationId;

    @ApiModelProperty(value = "创建的账期数量", example = "12")
    private Integer createdCount;

    @ApiModelProperty(value = "创建的账期列表")
    private List<FundPeriodResponse> periods;
}



