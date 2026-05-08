package com.erp.controller.report.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应付账款明细表 - 数据传输对象（用于数据库查询结果映射）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayableDetailDTO {
  // 合同字段
  private Long contractId;
  private String contractNo;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate contractSignDate;
  private String partyBName;
  private BigDecimal contractAmount;
  private String contractCreatedBy;
  private String settlementType;
  private String pricingMode;

  // 结算单字段
  private Long settlementId;
  private String settlementCode;
  private String settlementPeriod;
  private BigDecimal settlementAmount;
  private BigDecimal settlementPaidAmount;
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

  // 应付明细字段
  private Long apDetailId;
  private BigDecimal relatedAmount;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime relatedTime;
  private String apCreatedBy;
}
