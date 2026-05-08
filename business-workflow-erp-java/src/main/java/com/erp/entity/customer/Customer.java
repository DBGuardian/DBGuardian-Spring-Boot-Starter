package com.erp.entity.customer;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("CUSTOMER")
public class Customer extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 客户编码
     */
    @TableId(value = "客户编码", type = IdType.AUTO)
    private Integer customerId;

    /**
     * 企业名称
     */
    @TableField("企业名称")
    private String enterpriseName;

    /**
     * 统一社会信用代码
     */
    @TableField("统一社会信用代码")
    private String creditCode;

    /**
     * 地址
     */
    @TableField("地址")
    private String address;

    /**
     * 电话
     */
    @TableField("电话")
    private String phone;

    /**
     * 法定代表人
     */
    @TableField("法定代表人")
    private String legalRepresentative;

    /**
     * 联系人
     */
    @TableField("联系人")
    private String contactPerson;

    /**
     * 联系电话
     */
    @TableField("联系电话")
    private String contactPhone;

    /**
     * 曾用名
     */
    @TableField("曾用名")
    private String formerNames;

    /**
     * 客户状态：normal-正常，disabled-停用
     */
    @TableField("客户状态")
    private String customerStatus;

    /**
     * 业务员编码
     */
    @TableField("业务员编码")
    private Integer ownerEmployeeId;

    /**
     * 创建人
     */
    @TableField("创建人编码")
    private Integer creatorId;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    @TableField(exist = false)
    private String ownerEmployeeName;

    @TableField(exist = false)
    private String creatorName;
}


