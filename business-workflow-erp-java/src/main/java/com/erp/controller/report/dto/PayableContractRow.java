package com.erp.controller.report.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应付账款明细表 - 合同行数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayableContractRow {
  private Long contractId;
  private String contractNo;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate contractSignDate;
  private String partyBName;
  private BigDecimal contractAmount;
  private BigDecimal paidAmount;
  private BigDecimal outstandingAmount;
  private String settlementType;
  private String pricingMode;
  private String businessPerson;
  private Integer sequenceNo;
  private String rowKey;
  private Boolean isDetailRow = false;
  private Boolean hasChildren = false;
  private java.util.List<PayableSettlementRow> children;
}
