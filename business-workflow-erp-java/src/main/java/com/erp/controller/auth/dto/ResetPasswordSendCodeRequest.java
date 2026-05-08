package com.erp.controller.auth.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 忘记密码 - 发送邮箱验证码请求
 */
@Data
public class ResetPasswordSendCodeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邮箱地址
     */
    @NotBlank(message = "邮箱地址不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;
}









