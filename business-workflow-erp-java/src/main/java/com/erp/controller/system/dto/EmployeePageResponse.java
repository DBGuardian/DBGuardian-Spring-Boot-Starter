package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工分页查询响应DTO
 *
 * 用于员工档案 / 员工管理主列表分页展示
 */
@Data
public class EmployeePageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 员工编码
     */
    private Integer employeeId;

    /**
     * 员工姓名
     */
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
     * 联系方式（手机号码）
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 登录账号
     */
    private String loginAccount;

    /**
     * 角色：超级管理员/普通用户
     */
    private String role;

    /**
     * 员工状态：在职/离职/待审核/已拒绝
     */
    private String employeeStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 身份证号码
     */
    private String idCard;

    /**
     * 身份证正面图片URL
     */
    private String idCardFrontUrl;

    /**
     * 身份证反面图片URL
     */
    private String idCardBackUrl;

    /**
     * 身份证正面文件ID
     */
    private Integer idCardFrontFileId;

    /**
     * 身份证反面文件ID
     */
    private Integer idCardBackFileId;
}

