package com.erp.controller.report.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应付账款明细表 Excel 导出行数据
 *
 * contractRowSpan / settlementRowSpan 语义：
 *   > 0  该格为起始格，值为跨行数（= 1 时不执行合并操作，直接写值）
 *   = 0  该格已被上方起始格合并，写入阶段跳过
 */
@Data
@NoArgsConstructor
public class PayableExcelRow {

    // ── 合同字段 ──
    /** 合同跨行数 */
    private int contractRowSpan;
    /** 序号 */
    private int contractSeq;
    /** 结算类型 */
    private String settlementType;
    /** 合同类型（计价方式） */
    private String pricingMode;
    /** 供应商名称 */
    private String partyBName;
    /** 合同编号 */
    private String contractNo;
    /** 合同签订日期 */
    private LocalDate contractSignDate;
    /** 合同金额 */
    private BigDecimal contractAmount;
    /** 业务员（合同） */
    private String contractBizPerson;

    // ── 结算单字段 ──
    /** 结算单跨行数 */
    private int settlementRowSpan;
    /** 结算单编号 */
    private String settlementCode;
    /** 结算期间 */
    private String settlementPeriod;
    /** 结算金额 */
    private BigDecimal settlementAmount;
    /** 业务员（结算单） */
    private String settlementBizPerson;

    // ── 发票字段 ──
    /** 发票号码 */
    private String invoiceNumber;
    /** 开票日期 */
    private LocalDate invoiceDate;
    /** 发票金额 */
    private BigDecimal invoiceAmount;
    /** 税额 */
    private BigDecimal taxAmount;
    /** 价税合计 */
    private BigDecimal totalAmount;
    /** 业务员（发票） */
    private String invoiceBizPerson;

    // ── 应付明细字段 ──
    /** 应付金额 */
    private BigDecimal payableAmount;
    /** 已付金额 */
    private BigDecimal paidAmount;
    /** 付款日期 */
    private LocalDate paidDate;
    /** 未付金额 */
    private BigDecimal outstandingAmount;
    /** 付款天数 */
    private Integer daysToPayment;
    /** 应付账龄（天） */
    private Integer accountAge;
}
