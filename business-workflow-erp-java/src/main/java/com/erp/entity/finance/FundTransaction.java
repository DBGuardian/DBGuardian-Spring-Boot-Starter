package com.erp.entity.finance;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资金流水实体
 *
 * 对应表：FUND_TRANSACTION
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("FUND_TRANSACTION")
public class FundTransaction extends BaseEntity implements Serializable {

    /**
     * 流水编号
     */
    @TableId("流水编号")
    private Long transactionId;

    /**
     * 流水编码
     */
    @TableField("流水编码")
    private String transactionCode;

    /**
     * 账户编号
     */
    @TableField("账户编号")
    private Long accountId;

    /**
     * 交易日期
     */
    @TableField("交易日期")
    private LocalDate transactionDate;

    /**
     * 交易类型：INCOME、EXPENDITURE
     */
    @TableField("交易类型")
    private String transactionType;

    /**
     * 交易金额
     */
    @TableField("交易金额")
    private BigDecimal amount;

    /**
     * 往来单位账号
     */
    @TableField("往来单位账号")
    private String counterpartyAccount;

    /**
     * 往来单位账户名称
     */
    @TableField("往来单位账户名称")
    private String counterpartyName;

    /**
     * 往来单位开户银行名称
     */
    @TableField("往来单位开户银行名称")
    private String counterpartyBank;

    /**
     * 用途
     */
    @TableField("用途")
    private String purpose;

    /**
     * 摘要
     */
    @TableField("摘要")
    private String summary;

    /**
     * 科目编号
     */
    @TableField("科目编号")
    private Long subjectId;

    /**
     * 是否内部往来
     */
    @TableField("是否内部往来")
    private Boolean internalTransfer;

    /**
     * 关联账户编号
     */
    @TableField("关联账户编号")
    private Long relatedAccountId;

    /**
     * 关联流水编号
     */
    @TableField("关联流水编号")
    private Long relatedTransactionId;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 时间戳
     */
    @TableField("时间戳")
    private String timestamp;

    /**
     * 回单文件编号
     */
    @TableField("回单文件编号")
    private Integer receiptFile;

    /**
     * 回单编号
     */
    @TableField("回单编号")
    private String receiptNo;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Long createBy;

    /**
     * 更新人编码
     */
    @TableField("更新人编码")
    private Long updateBy;
}


