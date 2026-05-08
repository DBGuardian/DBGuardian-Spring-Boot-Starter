package com.erp.controller.system.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 员工注册请求DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class EmployeeRegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 员工姓名
     */
    @NotBlank(message = "员工姓名不能为空")
    @Size(max = 50, message = "员工姓名长度不能超过50个字符")
    private String employeeName;

    /**
     * 部门
     */
    private String department;

    /**
     * 岗位
     */
    private String jobTitle;

    /**
     * 手机号码（用作登录账号）
     */
    @NotBlank(message = "手机号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 身份证号码
     */
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$", message = "身份证号码格式不正确")
    private String idCard;

    /**
     * 身份证正面照片（选填）
     */
    private MultipartFile idCardFront;

    /**
     * 身份证反面照片（选填）
     */
    private MultipartFile idCardBack;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;

    /**
     * 确认密码（必填）
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}







































