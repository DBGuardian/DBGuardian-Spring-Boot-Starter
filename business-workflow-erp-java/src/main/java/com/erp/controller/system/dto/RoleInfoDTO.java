package com.erp.controller.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色信息DTO（包含权限数量）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "RoleInfoDTO", description = "角色信息（包含权限数量）")
public class RoleInfoDTO {

    @ApiModelProperty(value = "角色ID", example = "1")
    private Integer roleId;

    @ApiModelProperty(value = "角色名称", example = "管理员")
    private String roleName;

    @ApiModelProperty(value = "角色编码", example = "JS20250101001")
    private String roleCode;

    @ApiModelProperty(value = "角色描述", example = "系统管理员角色")
    private String roleDesc;

    @ApiModelProperty(value = "保护标志", example = "0")
    private Integer protectedFlag;

    @ApiModelProperty(value = "权限数量", example = "15")
    private Integer permissionCount;

    @ApiModelProperty(value = "用户数量", example = "5")
    private Integer userCount;
}
