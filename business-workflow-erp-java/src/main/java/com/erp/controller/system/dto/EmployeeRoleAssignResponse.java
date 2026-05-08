package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 员工角色分配响应DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class EmployeeRoleAssignResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 新增的角色数量
     */
    private Integer added;

    /**
     * 移除的角色数量
     */
    private Integer removed;

    /**
     * 保持不变的角色数量
     */
    private Integer unchanged;
}
