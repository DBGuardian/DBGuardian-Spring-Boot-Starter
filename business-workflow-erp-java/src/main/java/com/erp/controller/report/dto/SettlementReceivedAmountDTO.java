package com.erp.controller.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 结算单已收金额DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementReceivedAmountDTO {
  private Long settlementId;
  private BigDecimal receivedAmount;
}
