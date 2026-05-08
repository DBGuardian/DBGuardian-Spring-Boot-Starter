package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 日记账查询请求DTO
 */
@Data
@ApiModel("日记账查询请求")
public class FundDiaryRequest {

    @ApiModelProperty(value = "账户ID", example = "1", required = true)
    @NotNull(message = "账户ID不能为空")
    private Long accountId;

    @ApiModelProperty(value = "账期ID", example = "3")
    private Long periodId;

    @ApiModelProperty(value = "组织ID", example = "1", required = true)
    @NotNull(message = "组织ID不能为空")
    private Long organizationId;

    @ApiModelProperty(value = "年份", example = "2023")
    private Integer year;

    @ApiModelProperty(value = "月份（1-12）", example = "3")
    private Integer month;

    @ApiModelProperty(value = "日期范围开始", example = "2023-03-01")
    private String dateRangeStart;

    @ApiModelProperty(value = "日期范围结束", example = "2023-03-31")
    private String dateRangeEnd;

    @ApiModelProperty(value = "摘要（模糊查询）", example = "支付运费")
    private String summary;

    @ApiModelProperty(value = "往来单位账户名称（模糊查询）", example = "供应商A")
    private String counterpartyName;
}


