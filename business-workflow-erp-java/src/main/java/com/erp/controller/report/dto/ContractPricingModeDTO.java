package com.erp.controller.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 合同计价方式DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractPricingModeDTO {
  private Long contractId;
  private String pricingMode;
}
