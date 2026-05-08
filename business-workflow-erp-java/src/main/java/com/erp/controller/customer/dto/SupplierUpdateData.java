package com.erp.controller.customer.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 供应商更新数据结构
 * 包含供应商ID和要更新的字段数据
 */
@Data
public class SupplierUpdateData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 供应商ID（必填）
     */
    @NotNull(message = "供应商ID不能为空")
    private Integer supplierId;

    /**
     * 企业名称
     */
    @Size(max = 100, message = "企业名称长度不能超过100个字符")
    private String enterpriseName;

    /**
     * 统一社会信用代码
     */
    @Size(max = 20, message = "统一社会信用代码长度不能超过20个字符")
    private String creditCode;

    /**
     * 地址
     */
    @Size(max = 200, message = "地址长度不能超过200个字符")
    private String address;

    /**
     * 电话
     */
    @Size(max = 20, message = "电话长度不能超过20个字符")
    private String phone;

    /**
     * 法定代表人
     */
    @Size(max = 50, message = "法定代表人长度不能超过50个字符")
    private String legalRepresentative;

    /**
     * 联系人
     */
    @Size(max = 50, message = "联系人长度不能超过50个字符")
    private String contactPerson;

    /**
     * 联系电话
     */
    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    private String contactPhone;

    /**
     * 曾用名
     */
    @Size(max = 255, message = "曾用名长度不能超过255个字符")
    private String formerNames;

    /**
     * 供应商状态，例如：正常/停用
     */
    @Size(max = 20, message = "供应商状态长度不能超过20个字符")
    private String supplierStatus;

    /**
     * 业务员编码
     */
    private Integer ownerEmployeeId;

    /**
     * 备注
     */
    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String remark;

    // 供应商特有字段：收款账户信息
    /**
     * 账户名称
     */
    @Size(max = 100, message = "账户名称长度不能超过100个字符")
    private String accountName;

    /**
     * 账户号码
     */
    @Size(max = 50, message = "账户号码长度不能超过50个字符")
    private String accountNumber;

    /**
     * 开户银行
     */
    @Size(max = 100, message = "开户银行长度不能超过100个字符")
    private String bankName;

    /**
     * 乐观锁版本号
     */
    private Integer version;
}
