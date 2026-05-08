package com.erp.controller.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 员工权限分配请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "员工权限分配请求")
public class EmployeePermissionAssignRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "页面权限列表", required = true)
    @NotNull(message = "权限列表不能为空")
    private List<PagePermissionItem> permissions;

    /**
     * 页面权限项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel(description = "页面权限项")
    public static class PagePermissionItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "页面权限编号", required = true)
        @NotNull(message = "页面权限编号不能为空")
        private Integer pagePermissionId;

        @ApiModelProperty(value = "是否可查看：0=否，1=是", required = true)
        @NotNull(message = "可查看状态不能为空")
        private Integer canView;

        @ApiModelProperty(value = "是否可编辑：0=否，1=是", required = true)
        @NotNull(message = "可编辑状态不能为空")
        private Integer canEdit;

        @ApiModelProperty(value = "数据范围：SELF=仅查看自己，ALL=查看全部", required = true)
        @NotNull(message = "数据范围不能为空")
        private String viewScope;

        @ApiModelProperty(value = "操作范围：SELF=仅操作自己，ALL=操作全部", required = true)
        @NotNull(message = "操作范围不能为空")
        private String operateScope;
    }
}
