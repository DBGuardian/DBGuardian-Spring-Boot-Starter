package com.erp.controller.report.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 发票行数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRow {
  private Long invoiceId;
  private String invoiceNumber;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate invoiceDate;
  private BigDecimal invoiceAmount;
  private BigDecimal taxAmount;
  private BigDecimal totalAmount;
  private String businessPerson;

  // 树形结构字段
  private String rowKey;
  private Long settlementId;
  private BigDecimal receivedAmount;
  private BigDecimal outstandingAmount;
  private Boolean isDetailRow = false;
  private Boolean hasChildren = false;
  private List<ARDetailRow> children = new ArrayList<>();

  public InvoiceRow(Long invoiceId, String invoiceNumber, LocalDate invoiceDate,
                    BigDecimal invoiceAmount, BigDecimal taxAmount, BigDecimal totalAmount, String businessPerson) {
    this.invoiceId = invoiceId;
    this.invoiceNumber = invoiceNumber;
    this.invoiceDate = invoiceDate;
    this.invoiceAmount = invoiceAmount;
    this.taxAmount = taxAmount;
    this.totalAmount = totalAmount;
    this.businessPerson = businessPerson;
    this.rowKey = "invoice-" + invoiceId;
    this.receivedAmount = BigDecimal.ZERO;
    this.outstandingAmount = totalAmount;
    this.isDetailRow = false;
    this.hasChildren = false;
  }

  public void addChild(ARDetailRow child) {
    this.children.add(child);
    this.hasChildren = true;
    // 累加已收金额
    if (child.getReceivedAmount() != null) {
      this.receivedAmount = this.receivedAmount.add(child.getReceivedAmount());
    }
  }

  public void calculateOutstandingAmount() {
    this.outstandingAmount = this.totalAmount.subtract(this.receivedAmount);
  }
}
