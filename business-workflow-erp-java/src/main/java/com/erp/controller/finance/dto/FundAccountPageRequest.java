package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 资金账户分页查询请求
 *
 * 支持账户名称、账户类型、启用状态筛选以及字段排序。
 */
@Data
@ApiModel("资金账户分页查询请求")
public class FundAccountPageRequest {

    @ApiModelProperty(value = "账户名称（模糊查询）", example = "工商银行")
    private String accountName;

    @ApiModelProperty(value = "账户类型：BANK（银行）、PETTY_CASH（备用金）、CASH（现金）", example = "BANK")
    private String accountType;

    @ApiModelProperty(value = "是否启用：true-启用，false-停用", example = "true")
    private Boolean isEnabled;

    @ApiModelProperty(value = "当前页码（从1开始）", example = "1")
    @Min(value = 1, message = "当前页码不能小于1")
    private Integer current = 1;

    @ApiModelProperty(value = "每页大小", example = "10")
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 200, message = "每页大小不能超过200")
    private Integer size = 10;

    @ApiModelProperty(value = "排序字段（支持：accountCode, accountName, accountType, createTime, updateTime）", example = "createTime")
    private String sortField;

    @ApiModelProperty(value = "排序方向：asc / desc", example = "desc")
    private String sortOrder;

    /**
     * 数据范围过滤：当 viewScope=SELF 时由后端 Service 注入当前员工编码
     * 前端无需传此字段，后端自动注入
     */
    @ApiModelProperty(hidden = true)
    private Integer creatorFilter;
}










































































