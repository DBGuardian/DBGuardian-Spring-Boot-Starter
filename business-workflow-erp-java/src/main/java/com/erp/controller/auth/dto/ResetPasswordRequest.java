package com.erp.controller.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 忘记密码 - 重置密码请求
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class ResetPasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 员工ID（从第一步验证获取）
     */
    @NotNull(message = "员工信息异常，请重新验证账号")
    private Integer employeeId;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须在8-32位之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须包含字母和数字")
    private String newPassword;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}









