package com.erp.controller.auth.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 忘记密码 - 校验邮箱验证码请求
 */
@Data
public class ResetPasswordVerifyCodeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "员工ID不能为空")
    private Integer employeeId;

    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱长度不能超过64个字符")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码长度应为6位")
    private String emailCode;
}









