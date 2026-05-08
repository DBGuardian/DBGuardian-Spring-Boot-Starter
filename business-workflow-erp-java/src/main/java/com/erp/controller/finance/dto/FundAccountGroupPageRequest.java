package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 账户组合分页查询请求
 *
 * 支持组合名称、启用状态筛选以及字段排序。
 */
@Data
@ApiModel("账户组合分页查询请求")
public class FundAccountGroupPageRequest {

    @ApiModelProperty(value = "组合名称（模糊查询）", example = "主账户组合")
    private String groupName;

    @ApiModelProperty(value = "是否启用：true-启用，false-停用", example = "true")
    private Boolean isEnabled;

    @ApiModelProperty(value = "当前页码（从1开始）", example = "1")
    @Min(value = 1, message = "当前页码不能小于1")
    private Integer current = 1;

    @ApiModelProperty(value = "每页大小", example = "10")
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 200, message = "每页大小不能超过200")
    private Integer size = 10;

    @ApiModelProperty(value = "排序字段（支持：groupCode, groupName, enabled, createTime）", example = "createTime")
    private String sortField;

    @ApiModelProperty(value = "排序方向：asc / desc", example = "desc")
    private String sortOrder;
}

