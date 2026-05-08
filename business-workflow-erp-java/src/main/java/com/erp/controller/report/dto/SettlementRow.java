package com.erp.controller.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 结算单行数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementRow {
  private Long settlementId;
  private String settlementCode;
  private String settlementPeriod;
  private BigDecimal settlementAmount;
  private BigDecimal receivableAmount;
  private BigDecimal receivedAmount;
  private String businessPerson;

  // 树形结构字段
  private String rowKey;
  private Long contractId;
  private BigDecimal outstandingAmount;
  private Boolean isDetailRow = false;
  private Boolean hasChildren = false;
  private List<InvoiceDetailRow> children = new ArrayList<>();

  public SettlementRow(Long settlementId, String settlementCode, String settlementPeriod,
                       BigDecimal settlementAmount, BigDecimal receivedAmount, String businessPerson) {
    this.settlementId = settlementId;
    this.settlementCode = settlementCode;
    this.settlementPeriod = settlementPeriod;
    this.settlementAmount = settlementAmount;
    this.receivableAmount = settlementAmount;
    this.receivedAmount = receivedAmount != null ? receivedAmount : BigDecimal.ZERO;
    this.businessPerson = businessPerson;
    this.rowKey = "settlement-" + settlementId;
    this.outstandingAmount = settlementAmount.subtract(this.receivedAmount);
    this.isDetailRow = false;
    this.hasChildren = false;
  }

  public void addChild(InvoiceDetailRow child) {
    this.children.add(child);
    this.hasChildren = true;
  }
}
