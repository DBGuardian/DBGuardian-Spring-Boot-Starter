package com.erp.controller.customer.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 供应商分页返回
 *
 * 字段级权限控制已由字段注解迁移为外部配置，此处仅保留字段本身定义。
 */
@Data
public class SupplierPageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 供应商编码
     */
    private Integer supplierId;

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
     * 供应商状态，例如：正常/停用
     */
    private String supplierStatus;

    /**
     * 业务员ID
     */
    private Integer ownerEmployeeId;

    /**
     * 业务员姓名
     */
    private String ownerEmployeeName;

    /**
     * 创建人编码
     * 说明：用于前端行级权限控制（仅操作自己/操作全部），后端行级权限校验
     */
    private Integer creatorId;

    /**
     * 操作人名称（由创建人编码关联员工表获得）
     * 说明：与客户档案保持一致，后端通过 SUPPLIER.创建人编码 左连接 EMPLOYEE 表获取员工姓名
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

    // 供应商特有字段：收款账户信息

    /**
     * 账户名称
     */
    private String accountName;

    /**
     * 账户号码
     */
    private String accountNumber;

    /**
     * 开户银行
     */
    private String bankName;

    // 乐观锁版本号（非页面字段权限控制范围）
    private Integer version;
}
