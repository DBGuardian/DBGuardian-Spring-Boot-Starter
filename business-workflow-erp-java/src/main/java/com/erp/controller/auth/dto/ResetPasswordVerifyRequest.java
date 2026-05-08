package com.erp.controller.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 忘记密码第一步验证请求
 */
@Data
public class ResetPasswordVerifyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 登录账号
     */
    @NotBlank(message = "登录账号不能为空")
    private String account;

    /**
     * 绑定手机号
     */
    @NotBlank(message = "绑定手机号不能为空")
    private String phone;
}











