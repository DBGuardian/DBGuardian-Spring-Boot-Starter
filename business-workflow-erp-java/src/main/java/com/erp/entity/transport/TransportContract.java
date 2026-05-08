package com.erp.entity.transport;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 运输合同实体
 *
 * 对应表：TRANSPORT_CONTRACT
 * 设计原则：数据完整独立，所有业务关键字段冗余存储在本表。
 * 关联编号仅用于审查溯源，即使关联记录被删除，本合同数据仍完整可用。
 *
 * 新增合同时：写入本表所有冗余字段。
 * 后续使用时：直接读本表字段，不依赖关联表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("TRANSPORT_CONTRACT")
public class TransportContract extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 合同主键ID */
    @TableId(value = "合同编号", type = IdType.AUTO)
    private Integer contractId;

    /** 合同单号（系统生成，格式：TC-YYYYMMDD-XXXXX） */
    @TableField("合同单号")
    private String contractNo;

    // ── 签约方信息（甲方/承运方）──────────────────────────────────────────────

    /** 签约类型：个人司机/运输公司 */
    @TableField("签约类型")
    private String signingType;

    /** 承运方名称（个人司机姓名或运输公司名称） */
    @TableField("承运方名称")
    private String carrierName;

    /** 联系人姓名 */
    @TableField("联系人")
    private String contactPerson;

    /** 联系电话 */
    @TableField("联系电话")
    private String contactPhone;

    /** 身份证号（个人司机必填） */
    @TableField("身份证号码")
    private String idCardNo;

    /** 统一社会信用代码（运输公司必填） */
    @TableField("统一社会信用代码")
    private String creditCode;

    /** 开户银行 */
    @TableField("银行名称")
    private String bankName;

    /** 银行卡号 */
    @TableField("银行卡号")
    private String cardNumber;

    /** 银行账户名称 */
    @TableField("账户名称")
    private String accountName;

    // ── 乙方（我方）信息（冗余存储）──────────────────────────────────────────

    /** 乙方公司全称（我方公司名称） */
    @TableField("乙方名称")
    private String partyBName;

    /** 乙方统一社会信用代码 */
    @TableField("乙方统一社会信用代码")
    private String partyBCreditCode;

    /** 乙方联系人姓名 */
    @TableField("乙方联系人")
    private String partyBContactPerson;

    /** 乙方联系电话 */
    @TableField("乙方联系电话")
    private String partyBContactPhone;

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

    // ── 结算方式（按趟次结算 / 按重量结算 / 按距离结算）──────────────────────

    /** 结算方式：按趟次结算/按重量结算/按距离结算 */
    @TableField("结算方式")
    private String settlementMethod;

    /** 单价（元/趟 或 元/吨 或 元/公里） */
    @TableField("单价")
    private BigDecimal unitPrice;

    /** 计量单位 */
    @TableField("计量单位")
    private String unit;

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
