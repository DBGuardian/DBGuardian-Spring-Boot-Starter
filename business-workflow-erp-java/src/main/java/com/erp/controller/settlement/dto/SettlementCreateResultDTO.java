package com.erp.controller.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建结算单响应DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class SettlementCreateResultDTO {

    /**
     * 结算单列表（支持多个结算单）
     */
    private List<SettlementResult> settlements;

    /**
     * 总结算单数量
     */
    private Integer totalSettlementCount;

    /**
     * 单个结算单结果
     */
    @Data
    public static class SettlementResult {
        /**
         * 结算单ID
         */
        private Long settlementId;

        /**
         * 结算单号
         */
        private String settlementCode;

        /**
         * 总金额
         */
        private BigDecimal totalAmount;

        /**
         * 结算类型：RECEIVABLE=收款 / PAYABLE=付款
         */
        private String settlementType;
    }

    /**
     * 获取第一个结算单的结算单号（兼容旧接口）
     */
    public String getSettlementCode() {
        if (settlements != null && !settlements.isEmpty()) {
            return settlements.get(0).getSettlementCode();
        }
        return null;
    }

    /**
     * 获取第一个结算单的ID（兼容旧接口）
     */
    public Long getSettlementId() {
        if (settlements != null && !settlements.isEmpty()) {
            return settlements.get(0).getSettlementId();
        }
        return null;
    }
}
