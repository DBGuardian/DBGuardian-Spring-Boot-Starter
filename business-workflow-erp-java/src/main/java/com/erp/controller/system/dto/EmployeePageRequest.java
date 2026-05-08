package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 员工分页查询请求DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class EmployeePageRequest implements Serializable {

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
     * 联系方式（模糊搜索）
     */
    private String phone;

    /**
     * 登录账号（模糊搜索）
     */
    private String loginAccount;

    /**
     * 员工状态（精确匹配）：在职/离职/待审核/已拒绝
     */
    private String employeeStatus;

    /**
     * 排序字段：employeeId/employeeName/loginAccount/department/jobTitle/phone/employeeStatus/createTime/updateTime
     */
    private String sortField;

    /**
     * 排序方向：asc/desc
     */
    private String sortOrder;
}


