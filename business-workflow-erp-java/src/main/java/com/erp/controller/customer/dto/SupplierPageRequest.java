package com.erp.controller.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 供应商分页查询请求
 */
@Data
@Schema(description = "供应商分页查询请求")
public class SupplierPageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页
     */
    @Schema(description = "当前页", example = "1")
    private Long current = 1L;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "10")
    private Long size = 10L;

    /**
     * 企业名称（模糊）
     */
    @Schema(description = "企业名称（模糊搜索）")
    private String enterpriseName;

    /**
     * 统一社会信用代码（模糊）
     */
    private String creditCode;

    /**
     * 联系电话（模糊）
     */
    private String contactPhone;

    /**
     * 电话（模糊）
     */
    private String phone;

    /**
     * 联系人（模糊）
     */
    private String contactPerson;

    /**
     * 地址（模糊）
     */
    private String address;

    /**
     * 法定代表人（模糊）
     */
    private String legalRepresentative;

    /**
     * 曾用名（模糊）
     */
    private String formerNames;

    /**
     * 供应商编码（模糊）
     */
    private String supplierCode;

    /**
     * 供应商状态（精确匹配，例如：正常/停用）
     */
    private String supplierStatus;

    /**
     * 业务员编码（仅管理员可生效）
     */
    private Integer ownerEmployeeId;

    /**
     * 业务员（名称或编码，模糊搜索）
     */
    private String ownerEmployee;

    /**
     * 账户名称（模糊）
     */
    private String accountName;

    /**
     * 账户号码（模糊）
     */
    private String accountNumber;

    /**
     * 开户银行（模糊）
     */
    private String bankName;

    /**
     * 排序字段（例如：supplierId, enterpriseName, creditCode, phone, address, legalRepresentative, contactPerson, contactPhone, formerNames, supplierStatus, ownerEmployeeId, accountName, accountNumber, bankName, remark, createTime, updateTime）
     */
    private String orderBy;

    /**
     * 排序方向（asc: 升序, desc: 降序）
     */
    private String orderDirection;

    /**
     * 数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断
     */
    @Schema(description = "数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断")
    private String viewScope;
}
