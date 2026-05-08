package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 组织树形节点
 */
@Data
@ApiModel("组织树形节点")
public class OrganizationTreeNode {

    @ApiModelProperty(value = "节点ID（组织ID或账户ID）", required = true, example = "1")
    private String id;

    @ApiModelProperty(value = "节点类型", required = true, allowableValues = "organization,account", example = "organization")
    private String type;

    @ApiModelProperty(value = "节点标签（显示名称）", required = true, example = "主账户组织")
    private String label;

    @ApiModelProperty(value = "父节点ID", example = "0")
    private String parentId;

    @ApiModelProperty(value = "子节点列表")
    private List<OrganizationTreeNode> children;

    @ApiModelProperty(value = "节点数据（组织或账户的详细信息）")
    private Object data;

    @ApiModelProperty(value = "是否展开", example = "false")
    private Boolean expanded;

    @ApiModelProperty(value = "是否选中", example = "false")
    private Boolean selected;
}