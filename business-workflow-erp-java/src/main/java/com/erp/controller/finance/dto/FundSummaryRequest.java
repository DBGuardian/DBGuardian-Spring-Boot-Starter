package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 汇总表查询请求DTO
 */
@Data
@ApiModel("汇总表查询请求")
public class FundSummaryRequest {

    @ApiModelProperty(value = "组织ID（用于按组织汇总）", example = "1")
    private Long organizationId;

    @ApiModelProperty(value = "账期ID", example = "3")
    private Long periodId;

    @ApiModelProperty(value = "年份（与periodId二选一）", example = "2023")
    private Integer year;

    @ApiModelProperty(value = "季度（1-4）", example = "1")
    private Integer quarter;
    
}

