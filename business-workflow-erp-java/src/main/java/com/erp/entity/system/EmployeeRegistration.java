package com.erp.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 员工注册实体类
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("EMPLOYEE_REGISTRATION")
public class EmployeeRegistration extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 注册编号
     */
    @TableId(type = IdType.AUTO, value = "注册编号")
    private Integer registrationId;

    /**
     * 员工姓名
     */
    @TableField("员工姓名")
    private String employeeName;

    /**
     * 部门
     */
    @TableField("部门")
    private String department;

    /**
     * 岗位
     */
    @TableField("岗位")
    private String jobTitle;

    /**
     * 手机号码
     */
    @TableField("手机号码")
    private String phone;

    /**
     * 邮箱
     */
    @TableField("邮箱")
    private String email;

    /**
     * 身份证号码
     */
    @TableField("身份证号码")
    private String idCard;

    /**
     * 身份证正面文件编号
     */
    @TableField("身份证正面文件编号")
    private Integer idCardFrontFileId;

    /**
     * 身份证反面文件编号
     */
    @TableField("身份证反面文件编号")
    private Integer idCardBackFileId;

    /**
     * 登录账号
     */
    @TableField("登录账号")
    private String loginAccount;

    /**
     * 密码
     */
    @TableField("密码")
    private String password;

    /**
     * 审核状态：待审核/已通过/已拒绝
     */
    @TableField("审核状态")
    private String auditStatus;

    /**
     * 权限分配状态：待分配/已分配
     */
    @TableField("权限分配状态")
    private String permissionStatus;

    /**
     * 提交时间
     */
    @TableField("提交时间")
    private LocalDateTime submitTime;

    /**
     * 审核时间
     */
    @TableField("审核时间")
    private LocalDateTime auditTime;

    /**
     * 审核人编码
     */
    @TableField("审核人编码")
    private Integer auditorId;

    /**
     * 审核意见
     */
    @TableField("审核意见")
    private String auditOpinion;

    /**
     * 分配人编码
     */
    @TableField("分配人编码")
    private Integer assignerId;

    /**
     * 分配完成时间
     */
    @TableField("分配完成时间")
    private LocalDateTime assignCompleteTime;

    /**
     * 转员工编码（审核完成后同步生成的员工编码）
     */
    @TableField("转员工编码")
    private Integer employeeId;

    /**
     * 创建时间（EMPLOYEE_REGISTRATION 表中不存在此字段，排除）
     * 注意：此字段在父类 BaseEntity 中已定义，此处重新定义以排除数据库映射
     */
    @TableField(exist = false)
    private LocalDateTime createTime;

    /**
     * 更新时间（EMPLOYEE_REGISTRATION 表中不存在此字段，排除）
     * 注意：此字段在父类 BaseEntity 中已定义，此处重新定义以排除数据库映射
     */
    @TableField(exist = false)
    private LocalDateTime updateTime;
}







































