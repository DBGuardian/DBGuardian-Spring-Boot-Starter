package com.erp.controller.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 忘记密码 - 校验邮箱验证码响应
 */
@Data
public class ResetPasswordVerifyCodeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否校验通过
     */
    private boolean verified;

    /**
     * 员工ID（用于重置密码）
     */
    private Integer employeeId;
}









