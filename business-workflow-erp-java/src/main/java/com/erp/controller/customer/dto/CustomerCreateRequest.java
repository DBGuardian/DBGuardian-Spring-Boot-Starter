package com.erp.controller.customer.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 客户新增请求
 */
@Data
public class CustomerCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 100, message = "企业名称长度不能超过100个字符")
    private String enterpriseName;

    @NotBlank(message = "统一社会信用代码不能为空")
    @Size(max = 20, message = "统一社会信用代码长度不能超过20个字符")
    private String creditCode;

    @Size(max = 200, message = "地址长度不能超过200个字符")
    private String address;

    @Size(max = 20, message = "电话长度不能超过20个字符")
    private String phone;

    @Size(max = 50, message = "法定代表人长度不能超过50个字符")
    private String legalRepresentative;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 50, message = "联系人长度不能超过50个字符")
    private String contactPerson;

    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    private String contactPhone;  // 已改为选填

    @Size(max = 255, message = "曾用名长度不能超过255个字符")
    private String formerNames;

    /**
     * 客户状态，例如：正常/停用
     */
    @Size(max = 20, message = "客户状态长度不能超过20个字符")
    private String customerStatus;

    /**
     * 业务员编码（系统自动使用当前登录用户）
     */
    private Integer ownerEmployeeId;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String remark;
}




