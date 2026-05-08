package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PermissionTreeNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer permissionId;
    private String permissionName;
    private String permissionCode;
    private Integer permissionTypeId;
    /**
     * 页面模式，仅对页面节点生效
     * VIEW_ONLY / LIST / 其他 or null
     */
    private String pageMode;
    private List<PermissionTreeNode> children;
}



