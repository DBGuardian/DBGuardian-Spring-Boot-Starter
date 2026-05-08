package com.erp.controller.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户分页查询请求
 */
@Data
@Schema(description = "客户分页查询请求")
public class CustomerPageRequest implements Serializable {

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
     * 客户编码（模糊）
     */
    private String customerCode;

    /**
     * 客户状态（精确匹配，例如：正常/停用）
     */
    private String customerStatus;

    /**
     * 业务员编码（仅管理员可生效）
     */
    private Integer ownerEmployeeId;

    /**
     * 业务员（名称或编码，模糊搜索）
     */
    private String ownerEmployee;

    /**
     * 排序字段（例如：customerId, enterpriseName, creditCode, phone, address, legalRepresentative, contactPerson, contactPhone, formerNames, customerStatus, ownerEmployeeId, remark, createTime, updateTime）
     */
    private String orderBy;

    /**
     * 排序方向（asc: 升序, desc: 降序）
     */
    private String orderDirection;

    /**
     * 数据范围过滤：前端在 viewScope=SELF 时传入当前员工ID，只导出/查询自己创建的数据。
     * 后端在 listCustomersForExport 中会对此字段进行安全校验并强制覆盖，防止越权。
     * 列表分页查询（getCustomerPage）忽略此字段，由后端自动根据权限配置过滤。
     */
    private Integer creatorFilter;

    /**
     * 数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断
     */
    @Schema(description = "数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断")
    private String viewScope;
}




