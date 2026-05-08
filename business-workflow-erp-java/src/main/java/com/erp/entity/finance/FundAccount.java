package com.erp.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 资金账户实体
 *
 * 对应表：FUND_ACCOUNT
 *
 * 字段说明参考《资金管理实现说明》文档：
 * - 账户编号：主键，自增
 * - 账户编码：业务编号（如：ACC-20260101-0001）
 * - 账户名称：如“工商银行”、“备用金1号”、“现金1号”
 * - 账户类型：BANK（银行）、PETTY_CASH（备用金）、CASH（现金）
 * - 是否启用：1-启用，0-停用
 * - 备注：补充说明
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("FUND_ACCOUNT")
public class FundAccount extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 账户编号（主键，自增）
     */
    @TableId(value = "账户编号", type = IdType.AUTO)
    private Long accountId;

    /**
     * 账户编码（业务编号，如：ACC-20260101-0001）
     */
    @TableField("账户编码")
    private String accountCode;

    /**
     * 账户名称（如：工商银行、备用金1号、现金1号）
     */
    @TableField("账户名称")
    private String accountName;

    /**
     * 账户类型：BANK（银行）、PETTY_CASH（备用金）、CASH（现金）
     */
    @TableField("账户类型")
    private String accountType;

    /**
     * 所属组织ID
     */
    @TableField("组织编号")
    private Long organizationId;

    /**
     * 账户银行账号
     */
    @TableField("账户银行账号")
    private String accountBankAccount;

    /**
     * 账户银行/机构
     */
    @TableField("账户银行机构")
    private String accountBankInstitution;

    /**
     * 是否启用（1-启用，0-停用）
     */
    @TableField("是否启用")
    private Boolean enabled;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Integer createUserId;

    /**
     * 更新人编码
     */
    @TableField("更新人编码")
    private Integer updateUserId;
}


