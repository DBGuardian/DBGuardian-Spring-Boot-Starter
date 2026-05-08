package com.erp.controller.system.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 审核通过请求 DTO
 * 用于接收审核时修改的员工注册信息
 *
 * @author ERP System
 * @date 2025-01-27
 */
@Data
public class RegistrationApproveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 员工姓名
     */
    @NotBlank(message = "员工姓名不能为空")
    private String employeeName;

    /**
     * 手机号码
     */
    @NotBlank(message = "手机号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位手机号")
    private String phone;

    /**
     * 邮箱地址
     */
    @Email(message = "请输入正确的邮箱格式")
    private String email;

    /**
     * 身份证号码
     */
    private String idNumber;

    /**
     * 部门
     */
    private String department;

    /**
     * 岗位
     */
    private String jobTitle;
}
