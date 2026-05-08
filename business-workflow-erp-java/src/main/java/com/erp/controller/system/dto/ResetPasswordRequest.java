package com.erp.controller.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 重置密码请求DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class ResetPasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在6-32位之间")
    private String newPassword;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}

