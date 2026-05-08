package com.erp.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 开票通知单实体类
 *
 * 对应表：INVOICE_NOTICE
 *
 * @author ERP System
 * @date 2026-01-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("INVOICE_NOTICE")
public class InvoiceNotice extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 开票通知单编号
     */
    @TableId(value = "开票通知单编号", type = IdType.AUTO)
    private Integer noticeId;

    /**
     * 开票通知单号
     * 规则：KPTZ-YYYYMMDD-XXXX（4位序号）
     */
    @TableField("开票通知单号")
    private String noticeNo;

    /**
     * 合同编号
     */
    @TableField("合同编号")
    private Integer contractId;

    /**
     * 合同号
     */
    @TableField("合同号")
    private String contractNo;

    /**
     * 合同名称
     */
    @TableField("合同名称")
    private String contractName;

    /**
     * 客户编码
     */
    @TableField("客户编码")
    private Integer customerId;

    /**
     * 客户名称
     */
    @TableField("客户名称")
    private String customerName;

    /**
     * 主结算单编号
     */
    @TableField("结算单编号")
    private Integer mainSettlementId;

    /**
     * 已绑定结算摘要
     */
    @TableField("已绑定结算摘要")
    private String boundSettlementSummary;

    /**
     * 开票类型
     */
    @TableField("开票类型")
    private String invoiceType;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 状态
     * 草稿/待审批/已驳回/待开票/已开票/已归档/已取消
     */
    @TableField("状态")
    private String status;

    /**
     * 申请人编码
     */
    @TableField("申请人编码")
    private Integer applicantId;

    /**
     * 申请人姓名
     */
    @TableField("申请人姓名")
    private String applicantName;

    /**
     * 审批人编码
     */
    @TableField("审批人编码")
    private Integer approverId;

    /**
     * 审批人姓名
     */
    @TableField("审批人姓名")
    private String approverName;

    /**
     * 审批意见
     */
    @TableField("审批意见")
    private String approvalOpinion;

    /**
     * 办理人编码
     */
    @TableField("办理人编码")
    private Integer handlerId;

    /**
     * 办理人姓名
     */
    @TableField("办理人姓名")
    private String handlerName;

    /**
     * 已开票张数
     */
    @TableField("已开票张数")
    private Integer invoiceCount;

    /**
     * 已开票价税合计
     */
    @TableField("已开票价税合计")
    private BigDecimal totalAmount;

    /**
     * 开票完成时间
     */
    @TableField("开票完成时间")
    private LocalDateTime issuedAt;

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

