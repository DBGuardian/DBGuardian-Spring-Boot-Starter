package com.erp.controller.report.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应付账款明细表 - 发票+应付明细行数据（一对一关系，合并显示）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayableInvoiceDetailRow {
  // 发票字段
  private Long invoiceId;
  private String invoiceNumber;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate invoiceDate;
  private BigDecimal invoiceAmount;
  private BigDecimal taxAmount;
  private BigDecimal totalAmount;

  // 应付明细字段
  private Long apDetailId;
  private BigDecimal payableAmount;
  private BigDecimal paidAmount;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime paidDate;
  private BigDecimal outstandingAmount;
  private Integer daysToPayment;
  private Integer accountAge;
  private String businessPerson;

  // 树形结构字段
  private String rowKey;
  private Long settlementId;
  private Boolean isDetailRow = false;
  private Boolean hasChildren = false;

  public PayableInvoiceDetailRow(Long invoiceId, String invoiceNumber, LocalDate invoiceDate,
                                 BigDecimal invoiceAmount, BigDecimal taxAmount, BigDecimal totalAmount,
                                 Long apDetailId, BigDecimal payableAmount, BigDecimal paidAmount,
                                 LocalDateTime paidDate, String businessPerson, LocalDate invoiceDateForAge) {
    this.invoiceId = invoiceId;
    this.invoiceNumber = invoiceNumber;
    this.invoiceDate = invoiceDate;
    this.invoiceAmount = invoiceAmount;
    this.taxAmount = taxAmount;
    this.totalAmount = totalAmount;
    this.apDetailId = apDetailId;
    this.payableAmount = payableAmount;
    this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
    this.paidDate = paidDate;
    this.outstandingAmount = payableAmount.subtract(this.paidAmount);
    this.businessPerson = businessPerson;
    this.rowKey = "invoice-detail-" + invoiceId + "-" + apDetailId;
    this.isDetailRow = false;
    this.hasChildren = false;

    // 计算付款天数和应付账龄
    if (paidDate != null && invoiceDateForAge != null) {
      LocalDate paidLocalDate = paidDate.toLocalDate();
      this.daysToPayment = (int) java.time.temporal.ChronoUnit.DAYS.between(invoiceDateForAge, paidLocalDate);
      this.accountAge = (int) java.time.temporal.ChronoUnit.DAYS.between(invoiceDateForAge, LocalDate.now());
    } else if (invoiceDateForAge != null) {
      this.accountAge = (int) java.time.temporal.ChronoUnit.DAYS.between(invoiceDateForAge, LocalDate.now());
    }
  }
}
