package com.erp.controller.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 创建/更新权限请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String permissionName;
    private String permissionDescription;
    private Integer permissionTypeId;
    private Integer parentPermissionId;
}



