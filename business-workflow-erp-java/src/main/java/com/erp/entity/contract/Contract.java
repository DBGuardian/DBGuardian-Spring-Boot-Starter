package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合同实体
 *
 * 对应表：CONTRACT
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("CONTRACT")
public class Contract extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 合同编号
     */
    @TableId(value = "合同编号", type = IdType.AUTO)
    private Integer contractId;

    /**
     * 合同号（业务可见的合同编号：HQ-YYYYMMDD-XXXXX）
     */
    @TableField("合同号")
    private String contractNo;

    /**
     * 客户编码
     */
    @TableField("客户编码")
    private Integer customerId;

    /**
     * 客户快照（JSON）
     */
    @TableField("customer_snapshot")
    private String customerSnapshot;

    /**
     * 甲方名称
     */
    @TableField("甲方名称")
    private String partyAName;

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

    /**
     * 甲方统一社会信用代码
     */
    @TableField("甲方统一社会信用代码")
    private String partyACreditCode;

    /**
     * 乙方名称
     */
    @TableField("乙方名称")
    private String partyBName;

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

    /**
     * 乙方统一社会信用代码
     */
    @TableField("乙方统一社会信用代码")
    private String partyBCreditCode;

    /**
     * 合同金额
     */
    @TableField("合同金额")
    private BigDecimal contractAmount;

    /**
     * 是否启用业务费用结算
     */
    @TableField("业务费用结算")
    private Boolean feeSettlementEnabled;

    /**
     * 签订时间
     */
    @TableField("签订时间")
    private LocalDateTime signTime;

    /**
     * 合同状态
     */
    @TableField("合同状态")
    private String contractStatus;

    /**
     * 合同有效期开始
     */
    @TableField("合同有效期开始")
    private LocalDateTime validFrom;

    /**
     * 合同有效期结束
     */
    @TableField("合同有效期结束")
    private LocalDateTime validTo;

    /**
     * 编号生成方式（BEFORE_APPROVAL/AFTER_APPROVAL）
     */
    @TableField("编号生成方式")
    private String numberGenerationMode;

    /**
     * 审核人编码
     */
    @TableField("审核人编码")
    private Integer auditorId;

    /**
     * 审核时间
     */
    @TableField("审核时间")
    private LocalDateTime auditTime;

    /**
     * 寄件日期（合同寄出日期）
     */
    @TableField("寄件日期")
    private LocalDateTime sendDate;

    /**
     * 收件日期（合同收件日期）
     */
    @TableField("收件日期")
    private LocalDateTime receiveDate;

    /**
     * 合同扫描件文件编号，关联 FILE 表
     */
    @TableField("合同扫描件文件编号")
    private Integer contractFileId;

    /**
     * 合同PDF文件编号（审批后生成的正式合同PDF），关联 FILE 表
     */
    @TableField("合同PDF文件编号")
    private Integer contractPdfFileId;

    /**
     * 扫描件路径（冗余字段，便于快速访问）
     */
    @TableField("扫描件路径")
    private String scanFilePath;

    /**
     * 审核意见
     */
    @TableField("审核意见")
    private String auditOpinion;

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
}



