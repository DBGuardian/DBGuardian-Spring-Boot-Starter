package com.erp.controller.report.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JOIN查询返回的DTO（包含所有层级的数据）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceivableDetailDTO {
  // 合同字段
  private Long contractId;
  private String contractNo;
  private String partyAName;
  private BigDecimal contractAmount;
  private String contractCreatedBy;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate contractSignDate;
  private String settlementType;

  // 结算单字段
  private Long settlementId;
  private String settlementCode;
  private String settlementPeriod;
  private BigDecimal settlementAmount;
  private BigDecimal settlementReceivedAmount;
  private String settlementCreatedBy;

  // 发票字段
  private Long invoiceId;
  private String invoiceNumber;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate invoiceDate;
  private BigDecimal invoiceAmount;
  private BigDecimal taxAmount;
  private BigDecimal totalAmount;
  private String invoiceCreatedBy;

  // 应收明细字段
  private Long arDetailId;
  private BigDecimal relatedAmount;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime relatedTime;
  private String arCreatedBy;
}
