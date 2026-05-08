package com.erp.controller.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import java.util.List;

/**
 * 权限分页查询请求
 */
@Data
@ApiModel("权限分页查询请求")
public class PermissionPageRequest {

    /**
     * 当前页码
     */
    @ApiModelProperty(value = "当前页码，从1开始", example = "1")
    @Min(value = 1, message = "当前页码必须大于等于1")
    private long current = 1;

    /**
     * 每页数量
     */
    @ApiModelProperty(value = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量必须大于等于1")
    private long size = 10;

    /**
     * 权限类型ID（单个值，用于精确匹配）
     */
    @ApiModelProperty(value = "权限类型ID：1=模块权限，2=页面权限，3=字段权限")
    private Integer permissionTypeId;

    /**
     * 权限类型ID列表（多个值，用于OR查询）
     */
    @ApiModelProperty(value = "权限类型ID列表，用于OR查询：1=模块权限，2=页面权限，3=字段权限")
    private List<Integer> permissionTypeIds;

    /**
     * 权限名称（模糊匹配）
     */
    @ApiModelProperty(value = "权限名称（模糊匹配）")
    private String permissionName;

    /**
     * 权限描述（模糊匹配）
     */
    @ApiModelProperty(value = "权限描述（模糊匹配）")
    private String permissionDescription;

    /**
     * 页面模式（精确匹配，仅对页面权限有效）
     * 示例：VIEW_ONLY / LIST
     */
    @ApiModelProperty(value = "页面模式（仅对页面权限生效）：VIEW_ONLY=仅查看页面，LIST=可编辑列表页")
    private String pageMode;

    /**
     * 父权限名称（模糊匹配）
     */
    @ApiModelProperty(value = "父权限名称（模糊匹配）")
    private String parentPermissionName;
}
