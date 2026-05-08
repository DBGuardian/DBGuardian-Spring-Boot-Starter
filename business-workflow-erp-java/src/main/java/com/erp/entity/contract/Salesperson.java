package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务员基本信息实体
 *
 * 对应表：SALESPERSON
 * 存储业务员个人信息、甲乙方公司信息及收款卡，
 * 与 BUSINESS_CONTRACT 一对多关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("SALESPERSON")
public class Salesperson extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 业务员主键ID */
    @TableId(value = "业务员编号", type = IdType.AUTO)
    private Integer salespersonId;

    /** 关联系统员工编码（外部业务员为空） */
    @TableField("员工编码")
    private Integer employeeId;

    // ── 基本信息 ──────────────────────────────────────────

    /** 业务员姓名 */
    @TableField("业务员姓名")
    private String salespersonName;

    /** 业务员联系电话 */
    @TableField("业务员电话")
    private String salespersonPhone;

    /** 业务员身份证号 */
    @TableField("业务员身份证号")
    private String salespersonIdCard;

    // ── 甲方（合作公司）信息 ──────────────────────────────

    /** 关联客户编码（可为空，表示手动输入） */
    @TableField("客户编码")
    private Integer customerId;

    /** 甲方名称（合作公司全称） */
    @TableField("甲方名称")
    private String partyAName;

    /** 甲方统一社会信用代码 */
    @TableField("甲方统一社会信用代码")
    private String partyACreditCode;

    // ── 乙方（我方）信息 ──────────────────────────────────

    /** 乙方名称 */
    @TableField("乙方名称")
    private String partyBName;

    /** 乙方统一社会信用代码 */
    @TableField("乙方统一社会信用代码")
    private String partyBCreditCode;

    /** 乙方联系人 */
    @TableField("乙方联系人")
    private String partyBContactPerson;

    /** 乙方联系电话 */
    @TableField("乙方联系电话")
    private String partyBContactPhone;

    // ── 收款卡信息（单条） ────────────────────────────────

    /** 开户银行 */
    @TableField("开户银行")
    private String bankName;

    /** 银行卡号 */
    @TableField("银行卡号")
    private String cardNumber;

    /** 账户名称 */
    @TableField("账户名称")
    private String accountName;

    // ── 其他 ──────────────────────────────────────────────

    /** 备注 */
    @TableField("备注")
    private String remark;

    /** 创建人编码 */
    @TableField("创建人编码")
    private Integer creatorId;

    /** 创建人姓名 */
    @TableField("创建人姓名")
    private String creatorName;

    /** 逻辑删除：0正常 1已删除 */
    @TableField("是否删除")
    private Integer deleted;
}
