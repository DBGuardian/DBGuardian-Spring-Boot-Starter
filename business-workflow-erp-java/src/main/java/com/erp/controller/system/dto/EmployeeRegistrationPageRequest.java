package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 员工注册分页查询请求DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class EmployeeRegistrationPageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码，默认第1页
     */
    private Long current = 1L;

    /**
     * 每页数量，默认10条
     */
    private Long size = 10L;

    /**
     * 员工姓名（模糊搜索）
     */
    private String employeeName;

    /**
     * 部门（模糊搜索）
     */
    private String department;

    /**
     * 岗位（模糊搜索）
     */
    private String jobTitle;

    /**
     * 手机号码（模糊搜索）
     */
    private String phone;

    /**
     * 登录账号（模糊搜索）
     */
    private String loginAccount;

    /**
     * 审核状态（精确匹配）：待审核/已通过/已拒绝
     */
    private String auditStatus;

    /**
     * 权限分配状态（精确匹配）：待分配/已分配
     */
    private String permissionStatus;
}

