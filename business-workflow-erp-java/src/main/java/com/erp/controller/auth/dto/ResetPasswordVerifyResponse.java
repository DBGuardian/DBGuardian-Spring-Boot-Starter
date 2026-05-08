package com.erp.controller.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 忘记密码第一步验证响应
 */
@Data
public class ResetPasswordVerifyResponse implements Serializable {

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
     * 绑定手机号
     */
    private String phone;
}











