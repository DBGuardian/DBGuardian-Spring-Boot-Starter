package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工注册分页查询响应DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class EmployeeRegistrationPageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 注册编号
     */
    private Integer registrationId;

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
     * 手机号码
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 身份证号码
     */
    private String idCard;

    /**
     * 登录账号
     */
    private String loginAccount;

    /**
     * 审核状态：待审核/已通过/已拒绝
     */
    private String auditStatus;

    /**
     * 权限分配状态：待分配/已分配
     */
    private String permissionStatus;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

    /**
     * 审核人编码
     */
    private Integer auditorId;

    /**
     * 审核意见
     */
    private String auditOpinion;

    /**
     * 分配人编码
     */
    private Integer assignerId;

    /**
     * 分配完成时间
     */
    private LocalDateTime assignCompleteTime;

    /**
     * 转员工编码（审核完成后同步生成的员工编码）
     */
    private Integer employeeId;

    /**
     * 身份证正面照片URL
     */
    private String idCardFrontFileUrl;

    /**
     * 身份证反面照片URL
     */
    private String idCardBackFileUrl;

    /**
     * 身份证正面文件ID
     */
    private Integer idCardFrontFileId;

    /**
     * 身份证反面文件ID
     */
    private Integer idCardBackFileId;
}

