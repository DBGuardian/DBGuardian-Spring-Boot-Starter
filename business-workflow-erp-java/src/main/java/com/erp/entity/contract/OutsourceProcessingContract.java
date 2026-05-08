package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 委外处理合同实体
 *
 * 对应表：OUTSOURCE_PROCESSING_CONTRACT
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("OUTSOURCE_PROCESSING_CONTRACT")
public class OutsourceProcessingContract extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 合同编号
     */
    @TableId(value = "合同编号", type = IdType.AUTO)
    private Integer contractId;

    /**
     * 合同单号
     */
    @TableField("合同单号")
    private String contractNo;

    // ========== 甲方信息（危废处置供应商/承运方）==========

    /**
     * 甲方编码（关联 SUPPLIER.供应商编码）
     */
    @TableField("甲方编码")
    private Integer partyAId;

    /**
     * 甲方名称
     */
    @TableField("甲方名称")
    private String partyAName;

    /**
     * 甲方统一社会信用代码
     */
    @TableField("甲方统一社会信用代码")
    private String partyACreditCode;

    /**
     * 甲方联系人
     */
    @TableField("甲方联系人")
    private String partyAContact;

    /**
     * 甲方联系电话
     */
    @TableField("甲方联系电话")
    private String partyAContactPhone;

    // ========== 乙方信息（我方公司）==========

    /**
     * 乙方名称
     */
    @TableField("乙方名称")
    private String partyBName;

    /**
     * 乙方统一社会信用代码
     */
    @TableField("乙方统一社会信用代码")
    private String partyBCreditCode;

    /**
     * 乙方联系人
     */
    @TableField("乙方联系人")
    private String partyBContact;

    /**
     * 乙方联系电话
     */
    @TableField("乙方联系电话")
    private String partyBContactPhone;

    // ========== 业务员信息 ==========

    /**
     * 业务员编号
     */
    @TableField("业务员编号")
    private Integer ownerEmployeeId;

    /**
     * 业务员姓名
     */
    @TableField("业务员姓名")
    private String ownerEmployeeName;

    // ========== 业务费用结算 ==========

    /**
     * 业务费用结算（0-否，1-是）
     */
    @TableField("业务费用结算")
    private Boolean feeSettlementEnabled;

    // ========== 合同期限 ==========

    /**
     * 签订时间
     */
    @TableField("签订时间")
    private LocalDateTime signTime;

    /**
     * 有效期开始
     */
    @TableField("有效期开始")
    private LocalDateTime validFrom;

    /**
     * 有效期结束
     */
    @TableField("有效期结束")
    private LocalDateTime validTo;

    // ========== 审核与状态 ==========

    /**
     * 合同状态
     */
    @TableField("合同状态")
    private String contractStatus;

    /**
     * 审核人编码
     */
    @TableField("审核人编码")
    private Integer auditorId;

    /**
     * 审核人姓名
     */
    @TableField("审核人姓名")
    private String auditorName;

    /**
     * 审核时间
     */
    @TableField("审核时间")
    private LocalDateTime auditTime;

    /**
     * 审核意见
     */
    @TableField("审核意见")
    private String auditOpinion;

    // ========== 合同文件 ==========

    /**
     * 合同文件编号（关联 FILE 表，通过 fileService 获取文件信息）
     */
    @TableField("合同文件编号")
    private Integer contractFileId;

    // ========== 锁定信息 ==========

    /**
     * 是否锁定
     */
    @TableField("是否锁定")
    private Boolean isLocked;

    /**
     * 锁定时间
     */
    @TableField("锁定时间")
    private LocalDateTime lockTime;

    /**
     * 锁定人编码
     */
    @TableField("锁定人编码")
    private Integer lockerId;

    /**
     * 锁定原因
     */
    @TableField("锁定原因")
    private String lockReason;

    // ========== 其他 ==========

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Integer creatorId;

    /**
     * 创建人姓名
     */
    @TableField("创建人姓名")
    private String creatorName;
}
