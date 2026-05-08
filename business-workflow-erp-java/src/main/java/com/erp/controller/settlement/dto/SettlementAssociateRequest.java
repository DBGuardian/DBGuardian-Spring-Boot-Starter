package com.erp.controller.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SettlementAssociateRequest {
    private List<SettlementRelation> settlementRelations;

    @Data
    public static class SettlementRelation {
        private Long settlementId;
        private BigDecimal relAmount;
    }
}

