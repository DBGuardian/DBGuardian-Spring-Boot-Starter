package com.erp.controller.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 员工页面权限详情响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "员工页面权限详情")
public class EmployeePagePermissionDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "页面权限编号")
    private Integer pagePermissionId;

    @ApiModelProperty(value = "权限名称")
    private String permissionName;

    @ApiModelProperty(value = "权限编码")
    private String permissionCode;

    @ApiModelProperty(value = "页面模式：VIEW_ONLY=仅查看页面，LIST=可编辑列表页（支持数据范围），SIMPLE=简单操作页（有操作按钮但不区分数据范围）")
    private String pageMode;

    @ApiModelProperty(value = "是否可查看：0=否，1=是")
    private Integer canView;

    @ApiModelProperty(value = "是否可编辑：0=否，1=是")
    private Integer canEdit;

    @ApiModelProperty(value = "数据范围：SELF=仅查看自己，ALL=查看全部")
    private String viewScope;

    @ApiModelProperty(value = "操作范围：SELF=仅操作自己，ALL=操作全部")
    private String operateScope;
}
