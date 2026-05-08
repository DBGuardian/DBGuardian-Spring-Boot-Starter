package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 员工创建响应 DTO
 *
 * @author ERP
 */
@Data
public class EmployeeCreateResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 员工编码
     */
    private Integer employeeId;

    /**
     * 登录账号
     */
    private String loginAccount;

    /**
     * 员工姓名
     */
    private String employeeName;
}
































