package com.erp.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 员工实体类
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("EMPLOYEE")
public class Employee extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 员工编码
     */
    @TableId(value = "员工编码", type = IdType.AUTO)
    private Integer employeeId;

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
     * 联系方式
     */
    @TableField("联系方式")
    private String phone;

    /**
     * 邮箱
     */
    @TableField("邮箱")
    private String email;

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
     * 角色：超级管理员/普通用户
     */
    @TableField("角色")
    private String role;

    /**
     * 员工状态：在职/离职/待审核/已拒绝
     */
    @TableField("员工状态")
    private String employeeStatus;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 身份证号码
     */
    @TableField("身份证号码")
    private String idCard;

    /**
     * 身份证正面文件ID
     */
    @TableField("身份证正面文件ID")
    private Integer idCardFrontFileId;

    /**
     * 身份证反面文件ID
     */
    @TableField("身份证反面文件ID")
    private Integer idCardBackFileId;
}





