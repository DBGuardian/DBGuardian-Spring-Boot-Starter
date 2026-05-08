package com.erp.controller.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 角色创建请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "RoleCreateRequest", description = "角色创建请求")
public class RoleCreateRequest {

    @NotBlank(message = "角色名称不能为空")
    @ApiModelProperty(value = "角色名称", required = true, example = "管理员")
    private String roleName;

    @ApiModelProperty(value = "角色描述", example = "系统管理员角色，拥有所有权限")
    private String roleDesc;
}
