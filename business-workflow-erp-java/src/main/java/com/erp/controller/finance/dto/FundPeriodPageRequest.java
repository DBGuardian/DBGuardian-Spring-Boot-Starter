package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 账期分页查询请求DTO
 */
@Data
@ApiModel("账期分页查询请求")
public class FundPeriodPageRequest {

    @ApiModelProperty(value = "组织ID", required = true, example = "1")
    @NotNull(message = "组织ID不能为空")
    private Long organizationId;

    @ApiModelProperty(value = "年份", example = "2026")
    private Integer year;

    @ApiModelProperty(value = "结账状态（true-已结账，false-未结账）", example = "false")
    private Boolean isSettled;

    @ApiModelProperty(value = "账期编码", example = "202601")
    private String periodCode;

    @ApiModelProperty(value = "当前页码", example = "1")
    private Long current = 1L;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Long size = 10L;

    @ApiModelProperty(value = "排序字段", example = "createTime")
    private String sortField = "createTime";

    @ApiModelProperty(value = "排序顺序（asc-升序，desc-降序）", example = "desc")
    private String sortOrder = "desc";
}