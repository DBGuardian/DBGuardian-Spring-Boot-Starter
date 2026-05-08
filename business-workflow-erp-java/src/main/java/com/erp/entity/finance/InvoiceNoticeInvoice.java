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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 开票通知单-发票登记明细实体类
 *
 * 对应表：INVOICE_NOTICE_INVOICE
 *
 * @author ERP System
 * @date 2026-01-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("INVOICE_NOTICE_INVOICE")
public class InvoiceNoticeInvoice extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 明细编号
     */
    @TableId(value = "明细编号", type = IdType.AUTO)
    private Integer detailId;

    /**
     * 开票通知单编号
     */
    @TableField("开票通知单编号")
    private Integer noticeId;

    /**
     * 发票编号
     */
    @TableField("发票编号")
    private Integer invoiceId;

    /**
     * 发票号码
     */
    @TableField("发票号码")
    private String invoiceNumber;

    /**
     * 发票代码
     */
    @TableField("发票代码")
    private String invoiceCode;

    /**
     * 开票日期
     */
    @TableField("开票日期")
    private LocalDate invoiceDate;

    /**
     * 发票类型
     */
    @TableField("发票类型")
    private String invoiceType;

    /**
     * 发票形式
     */
    @TableField("发票形式")
    private String invoiceForm;

    /**
     * 不含税金额
     */
    @TableField("不含税金额")
    private BigDecimal amount;

    /**
     * 税额
     */
    @TableField("税额")
    private BigDecimal taxAmount;

    /**
     * 价税合计
     */
    @TableField("价税合计")
    private BigDecimal totalAmount;

    /**
     * 购买方名称
     */
    @TableField("购买方名称")
    private String buyerName;

    /**
     * 购买方统一社会信用代码
     */
    @TableField("购买方统一社会信用代码")
    private String buyerCreditCode;

    /**
     * 销售方名称
     */
    @TableField("销售方名称")
    private String sellerName;

    /**
     * 销售方统一社会信用代码
     */
    @TableField("销售方统一社会信用代码")
    private String sellerCreditCode;

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
     * 排除更新时间字段（表中不存在此字段）
     */
    @TableField(value = "更新时间", exist = false)
    private LocalDateTime updateTime;
}

