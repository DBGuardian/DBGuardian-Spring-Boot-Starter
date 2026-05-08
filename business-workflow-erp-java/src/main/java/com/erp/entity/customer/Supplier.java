package com.erp.entity.customer;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 供应商实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("SUPPLIER")
public class Supplier extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 供应商编码
     */
    @TableId(value = "供应商编码", type = IdType.AUTO)
    private Integer supplierId;

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
     * 供应商状态：正常/停用
     */
    @TableField("供应商状态")
    private String supplierStatus;

    /**
     * 业务员编码
     */
    @TableField("业务员编码")
    private Integer ownerEmployeeId;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Integer creatorId;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    // 供应商特有字段：收款账户信息
    /**
     * 账户名称
     */
    @TableField("账户名称")
    private String accountName;

    /**
     * 账户号码
     */
    @TableField("账户号码")
    private String accountNumber;

    /**
     * 开户银行
     */
    @TableField("开户银行")
    private String bankName;

    // 非数据库字段，用于关联查询
    @TableField(exist = false)
    private String ownerEmployeeName;

    @TableField(exist = false)
    private String creatorName;
}
