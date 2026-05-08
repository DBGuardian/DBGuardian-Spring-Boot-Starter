package com.erp.controller.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 员工权限分配响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "员工权限分配响应")
public class EmployeePermissionAssignResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "新增的权限数量")
    private Integer added;

    @ApiModelProperty(value = "更新的权限数量")
    private Integer updated;

    @ApiModelProperty(value = "删除的权限数量")
    private Integer deleted;

    @ApiModelProperty(value = "保持不变的权限数量")
    private Integer unchanged;
}
