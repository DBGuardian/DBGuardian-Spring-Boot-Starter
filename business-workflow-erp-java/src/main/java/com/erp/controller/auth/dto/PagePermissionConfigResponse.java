package com.erp.controller.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 页面权限配置响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagePermissionConfigResponse {
    
    /**
     * 是否可查看
     */
    private Integer canView;
    
    /**
     * 是否可编辑/操作
     */
    private Integer canEdit;
    
    /**
     * 数据范围（SELF/ALL）
     */
    private String viewScope;
    
    /**
     * 操作范围（SELF/ALL）
     */
    private String operateScope;
}
