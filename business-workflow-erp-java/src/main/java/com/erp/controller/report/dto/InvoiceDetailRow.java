package com.erp.controller.report.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发票+应收明细行数据（一对一关系，合并显示）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDetailRow {
  // 发票字段
  private Long invoiceId;
  private String invoiceNumber;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate invoiceDate;
  private BigDecimal invoiceAmount;
  private BigDecimal taxAmount;
  private BigDecimal totalAmount;

  // 应收明细字段
  private Long arDetailId;
  private BigDecimal receivableAmount;
  private BigDecimal receivedAmount;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime receivedDate;
  private BigDecimal outstandingAmount;
  private Integer daysToPayment;
  private Integer accountAge;
  private String businessPerson;

  // 树形结构字段
  private String rowKey;
  private Long settlementId;
  private Boolean isDetailRow = false;
  private Boolean hasChildren = false;

  public InvoiceDetailRow(Long invoiceId, String invoiceNumber, LocalDate invoiceDate,
                          BigDecimal invoiceAmount, BigDecimal taxAmount, BigDecimal totalAmount,
                          Long arDetailId, BigDecimal receivableAmount, BigDecimal receivedAmount,
                          LocalDateTime receivedDate, String businessPerson, LocalDate invoiceDateForAge) {
    this.invoiceId = invoiceId;
    this.invoiceNumber = invoiceNumber;
    this.invoiceDate = invoiceDate;
    this.invoiceAmount = invoiceAmount;
    this.taxAmount = taxAmount;
    this.totalAmount = totalAmount;
    this.arDetailId = arDetailId;
    this.receivableAmount = receivableAmount;
    this.receivedAmount = receivedAmount != null ? receivedAmount : BigDecimal.ZERO;
    this.receivedDate = receivedDate;
    this.outstandingAmount = receivableAmount.subtract(this.receivedAmount);
    this.businessPerson = businessPerson;
    this.rowKey = "invoice-detail-" + invoiceId + "-" + arDetailId;
    this.isDetailRow = false;
    this.hasChildren = false;

    // 计算回款天数和应收账龄
    if (receivedDate != null && invoiceDateForAge != null) {
      LocalDate receivedLocalDate = receivedDate.toLocalDate();
      this.daysToPayment = (int) java.time.temporal.ChronoUnit.DAYS.between(invoiceDateForAge, receivedLocalDate);
      this.accountAge = (int) java.time.temporal.ChronoUnit.DAYS.between(invoiceDateForAge, LocalDate.now());
    } else if (invoiceDateForAge != null) {
      this.accountAge = (int) java.time.temporal.ChronoUnit.DAYS.between(invoiceDateForAge, LocalDate.now());
    }
  }
}
