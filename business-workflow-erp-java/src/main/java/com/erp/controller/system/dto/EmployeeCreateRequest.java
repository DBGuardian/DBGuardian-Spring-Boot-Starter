package com.erp.controller.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.List;

/**
 * 员工创建请求 DTO
 *
 * @author ERP
 */
@Data
public class EmployeeCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "员工姓名不能为空")
    private String employeeName;

    /**
     * 部门（选填）
     */
    private String department;

    /**
     * 岗位（选填）
     */
    private String jobTitle;

    @NotBlank(message = "手机号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位手机号")
    private String phone;

    /**
     * 邮箱（选填）
     */
    private String email;

    /**
     * 身份证号码（选填）
     */
    private String idCard;

    @NotBlank(message = "登录密码不能为空")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 角色：超级管理员/普通用户
     */
    @NotBlank(message = "角色不能为空")
    private String role;
}

