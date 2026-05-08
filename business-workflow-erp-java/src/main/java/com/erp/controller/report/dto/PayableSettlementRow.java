package com.erp.controller.report.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 应付账款明细表 - 结算单行数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayableSettlementRow {
  private Long settlementId;
  private String settlementCode;
  private Long contractId;
  private String settlementPeriod;
  private BigDecimal settlementAmount;
  private BigDecimal payableAmount;
  private BigDecimal paidAmount;
  private BigDecimal outstandingAmount;
  private String businessPerson;
  private String rowKey;
  private Boolean isDetailRow = false;
  private Boolean hasChildren = false;
  private java.util.List<PayableInvoiceDetailRow> children;
}
