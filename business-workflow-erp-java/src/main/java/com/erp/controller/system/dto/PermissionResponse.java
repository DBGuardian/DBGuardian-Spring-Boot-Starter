package com.erp.controller.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 权限响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer permissionId;
    private String permissionName;
    private String permissionDescription;
    private Integer permissionTypeId;
    private String permissionCode;
    /**
     * 页面模式：仅对页面权限（type=2）生效
     * VIEW_ONLY = 仅查看页面
     * LIST      = 列表页（可编辑）
     */
    private String pageMode;
    private Integer parentPermissionId;
    private String parentPermissionName;
    
    /**
     * 以下字段仅对页面级权限（permissionTypeId=2）有效
     * 用于前端进行细粒度的权限控制
     */
    
    /**
     * 数据查看范围
     * SELF = 仅查看自己创建的数据
     * ALL = 查看全部数据
     */
    private String viewScope;
    
    /**
     * 数据操作范围
     * SELF = 仅操作自己创建的数据
     * ALL = 操作全部数据
     */
    private String operateScope;
    
    /**
     * 是否可查看
     * 0 = 不可查看
     * 1 = 可查看
     */
    private Integer canView;
    
    /**
     * 是否可编辑
     * 0 = 不可编辑（不可操作）
     * 1 = 可编辑
     */
    private Integer canEdit;
}



