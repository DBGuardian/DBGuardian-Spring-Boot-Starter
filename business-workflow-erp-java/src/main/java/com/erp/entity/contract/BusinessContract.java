package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 业务合同实体
 *
 * 对应表：BUSINESS_CONTRACT
 * 设计原则：数据完整独立，所有业务关键字段冗余存储在本表。
 * 关联编号（危废合同编号、业务员编号）仅用于审查溯源，
 * 即使关联记录被删除，本合同数据仍完整可用。
 *
 * 新增合同时：从 SALESPERSON 读取并写入本表所有冗余字段。
 * 后续使用时：直接读本表字段，不依赖关联表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("BUSINESS_CONTRACT")
public class BusinessContract extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 合同主键ID */
    @TableId(value = "合同编号", type = IdType.AUTO)
    private Integer contractId;

    /** 合同单号（系统生成，格式：BC-YYYYMMDD-XXXXX） */
    @TableField("合同单号")
    private String contractNo;

    /**
     * 关联危废合同编号（仅审查溯源用）
     * 即使危废合同被删除，本合同数据仍完整独立，不受影响
     */
    @TableField("危废合同编号")
    private Integer hazardousContractId;

    /**
     * 关联危废合同的合同号（非数据库字段，由 JOIN CONTRACT 查询获得）
     */
    @TableField(exist = false)
    private String hazardousContractNo;

    /**
     * 关联业务员编号（仅审查溯源用）
     * 即使 SALESPERSON 记录被删除，本合同所有业务数据仍完整
     */
    @TableField("业务员编号")
    private Integer salespersonId;

    // ── 甲方信息（业务员 + 合作公司，冗余存储）──────────────────────────────────

    /** 业务员姓名（冗余） */
    @TableField("业务员姓名")
    private String salespersonName;

    /** 业务员联系电话（冗余） */
    @TableField("业务员电话")
    private String salespersonPhone;

    /** 业务员身份证号（冗余） */
    @TableField("业务员身份证号")
    private String salespersonIdCard;

    /** 甲方公司全称（冗余） */
    @TableField("甲方名称")
    private String partyAName;

    /** 甲方统一社会信用代码（冗余） */
    @TableField("甲方统一社会信用代码")
    private String partyACreditCode;

    // ── 乙方（我方）信息（冗余存储）────────────────────────────────────────────

    /** 乙方公司全称（冗余） */
    @TableField("乙方名称")
    private String partyBName;

    /** 乙方统一社会信用代码（冗余） */
    @TableField("乙方统一社会信用代码")
    private String partyBCreditCode;

    /** 乙方联系人姓名（冗余） */
    @TableField("乙方联系人")
    private String partyBContactPerson;

    /** 乙方联系电话（冗余） */
    @TableField("乙方联系电话")
    private String partyBContactPhone;

    // ── 收款卡信息（冗余存储）──────────────────────────────────────────────────

    /** 开户银行名称（冗余） */
    @TableField("开户银行")
    private String bankName;

    /** 银行卡号（冗余） */
    @TableField("银行卡号")
    private String cardNumber;

    /** 银行账户名称（冗余） */
    @TableField("账户名称")
    private String accountName;

    // ── 审核与状态 ──────────────────────────────────────────────────────────────

    /** 合同状态：待审核/审核中/执行中/已驳回/已完结/已归档 */
    @TableField("合同状态")
    private String status;

    /** 审核意见 */
    @TableField("审核意见")
    private String auditOpinion;

    /** 审核人编码 */
    @TableField("审核人编码")
    private Integer auditorId;

    /** 审核人姓名 */
    @TableField("审核人姓名")
    private String auditorName;

    /** 审核时间 */
    @TableField("审核时间")
    private LocalDateTime auditTime;

    // ── 合同期限 ────────────────────────────────────────────────────────────────

    /** 合同签订时间 */
    @TableField("签订时间")
    private LocalDate signTime;

    /** 合同有效期开始日期 */
    @TableField("有效期开始")
    private LocalDate validFrom;

    /** 合同有效期结束日期 */
    @TableField("有效期结束")
    private LocalDate validTo;

    // ── 合同文件 ────────────────────────────────────────────────────────────────

    /** 合同文件编号（关联 FILE 表） */
    @TableField("合同文件编号")
    private Integer contractFileId;

    /** 合同文件访问路径（冗余，便于快速访问） */
    @TableField("合同文件路径")
    private String contractFilePath;

    // ── 其他 ────────────────────────────────────────────────────────────────────

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
