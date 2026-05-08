package com.erp.controller.auth.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 忘记密码 - 发送邮箱验证码响应
 */
@Data
public class ResetPasswordSendCodeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邮件接收地址
     */
    private String targetEmail;

    /**
     * 员工ID
     */
    private Integer employeeId;

    /**
     * 验证码有效期（秒）
     */
    private Integer expireSeconds;

    /**
     * 发送时间
     */
    private LocalDateTime sentAt;
}









