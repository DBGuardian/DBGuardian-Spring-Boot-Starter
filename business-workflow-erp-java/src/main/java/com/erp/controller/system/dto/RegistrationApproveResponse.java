package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 审核通过响应DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class RegistrationApproveResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 新创建的员工编号
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
}


