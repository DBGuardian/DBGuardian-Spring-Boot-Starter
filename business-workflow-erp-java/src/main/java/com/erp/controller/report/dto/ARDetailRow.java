package com.erp.controller.report.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应收明细行数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ARDetailRow {
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
  private Long invoiceId;
  private Boolean isDetailRow = true;
  private Boolean hasChildren = false;

  public ARDetailRow(Long arDetailId, BigDecimal receivedAmount, LocalDateTime receivedDate,
                     String businessPerson, LocalDate invoiceDate) {
    this.arDetailId = arDetailId;
    this.receivedAmount = receivedAmount != null ? receivedAmount : BigDecimal.ZERO;
    this.receivedDate = receivedDate;
    this.businessPerson = businessPerson;
    this.rowKey = "ar-detail-" + arDetailId;
    this.isDetailRow = true;
    this.hasChildren = false;

    // 计算回款天数和应收账龄
    if (receivedDate != null && invoiceDate != null) {
      LocalDate receivedLocalDate = receivedDate.toLocalDate();
      this.daysToPayment = (int) java.time.temporal.ChronoUnit.DAYS.between(invoiceDate, receivedLocalDate);
      this.accountAge = (int) java.time.temporal.ChronoUnit.DAYS.between(invoiceDate, LocalDate.now());
    } else if (invoiceDate != null) {
      this.accountAge = (int) java.time.temporal.ChronoUnit.DAYS.between(invoiceDate, LocalDate.now());
    }
  }
}
