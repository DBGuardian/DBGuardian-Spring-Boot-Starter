package com.erp.controller.customer.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户分页返回
 *
 * 字段级权限控制已由字段注解迁移为外部配置，此处仅保留字段本身定义。
 */
@Data
public class CustomerPageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户编码
     */
    private Integer customerId;

    /**
     * 企业名称
     */
    private String enterpriseName;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 地址
     */
    private String address;

    /**
     * 电话
     */
    private String phone;

    /**
     * 法定代表人
     */
    private String legalRepresentative;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 曾用名
     */
    private String formerNames;

    /**
     * 客户状态，例如：正常/停用
     */
    private String customerStatus;

    /**
     * 业务员ID
     */
    private Integer ownerEmployeeId;

    /**
     * 业务员姓名
     */
    private String ownerEmployeeName;

    /**
     * 创建人编码（操作人ID）
     */
    private Integer creatorId;

    /**
     * 操作人名称（由创建人编码关联员工表获得）
     */
    private String creatorName;

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
}




