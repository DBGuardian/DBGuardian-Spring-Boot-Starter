package com.erp.controller.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 累积已结算量和合同计划总量DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccumulatedQuantityDTO {

    /**
     * 累积已结算量
     */
    private BigDecimal accumulatedQuantity;

    /**
     * 合同计划总量
     */
    private BigDecimal contractPlanTotal;
}
