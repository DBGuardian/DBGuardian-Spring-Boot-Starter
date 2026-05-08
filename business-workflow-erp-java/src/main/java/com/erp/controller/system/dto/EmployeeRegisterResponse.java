package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 员工注册响应DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class EmployeeRegisterResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 员工ID
     */
    private Integer employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 登录账号
     */
    private String loginAccount;

    /**
     * 员工状态
     */
    private String employeeStatus;

    /**
     * 提示信息
     */
    private String message;
}







































