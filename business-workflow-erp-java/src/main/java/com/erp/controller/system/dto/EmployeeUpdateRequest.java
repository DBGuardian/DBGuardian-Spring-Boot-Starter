package com.erp.controller.system.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 员工更新请求 DTO
 *
 * @author ERP
 */
@Data
public class EmployeeUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "员工姓名不能为空")
    private String employeeName;

    /**
     * 部门
     */
    private String department;

    /**
     * 岗位
     */
    private String jobTitle;

    @NotBlank(message = "手机号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位手机号")
    private String phone;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "请输入正确的邮箱格式")
    private String email;

    /**
     * 身份证号码
     */
    @NotBlank(message = "身份证号码不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$", message = "请输入正确的18位身份证号码")
    private String idCard;

    /**
     * 员工状态：在职/离职/停用
     */
    @NotBlank(message = "员工状态不能为空")
    private String employeeStatus;

    /**
     * 角色：超级管理员/普通用户
     */
    private String role;

    /**
     * 备注信息
     */
    private String remark;
}
