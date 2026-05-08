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
 * 合同行数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractRow {
  private Long contractId;
  private String contractNo;
  private String partyAName;
  private BigDecimal contractAmount;
  private String pricingMode;
  private String businessPerson;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate contractSignDate;
  private String settlementType;

  // 树形结构字段
  private String rowKey;
  private Integer sequenceNo;
  private BigDecimal receivedAmount;
  private BigDecimal outstandingAmount;
  private Boolean isDetailRow = false;
  private Boolean hasChildren = false;
  private List<SettlementRow> children = new ArrayList<>();

  public ContractRow(Long contractId, String contractNo, String partyAName, BigDecimal contractAmount,
                     String pricingMode, String businessPerson, LocalDate contractSignDate, String settlementType) {
    this.contractId = contractId;
    this.contractNo = contractNo;
    this.partyAName = partyAName;
    this.contractAmount = contractAmount;
    this.pricingMode = pricingMode;
    this.businessPerson = businessPerson;
    this.contractSignDate = contractSignDate;
    this.settlementType = settlementType;
    this.rowKey = "contract-" + contractId;
    this.sequenceNo = 0;
    this.receivedAmount = BigDecimal.ZERO;
    this.outstandingAmount = contractAmount;
    this.isDetailRow = false;
    this.hasChildren = false;
  }

  public void addChild(SettlementRow child) {
    this.children.add(child);
    this.hasChildren = true;
    // 累加已收金额和未收金额
    if (child.getReceivedAmount() != null) {
      this.receivedAmount = this.receivedAmount.add(child.getReceivedAmount());
    }
  }

  public void calculateOutstandingAmount() {
    this.outstandingAmount = this.contractAmount.subtract(this.receivedAmount);
  }
}
