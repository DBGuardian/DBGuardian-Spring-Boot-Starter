package com.erp.controller.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 角色权限批量查询请求
 * 请求体：{ "roleIds": number[] }
 */
@Data
@ApiModel("角色权限批量查询请求")
public class RolePermissionsBatchGetRequest {

    @ApiModelProperty(value = "角色ID列表", required = true)
    @NotEmpty(message = "roleIds 不能为空")
    private List<Integer> roleIds;
}

