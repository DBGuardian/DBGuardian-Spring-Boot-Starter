package com.erp.controller.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 业务费统计信息DTO
 */
@Data
public class BusinessFeeStatisticsDTO {

    /**
     * 总记录数
     */
    private Long totalCount = 0L;

    /**
     * 待审核状态数量
     */
    private Long pendingAuditCount = 0L;

    /**
     * 审核中状态数量
     */
    private Long auditingCount = 0L;

    /**
     * 已审核状态数量
     */
    private Long auditedCount = 0L;

    /**
     * 已驳回状态数量
     */
    private Long rejectedCount = 0L;

    /**
     * 已收款状态数量
     */
    private Long receivedCount = 0L;

    /**
     * 收款类型总金额
     */
    private BigDecimal receivableTotalAmount = BigDecimal.ZERO;

    /**
     * 付款类型总金额
     */
    private BigDecimal payableTotalAmount = BigDecimal.ZERO;

    /**
     * 总金额
     */
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * 已收款总金额
     */
    private BigDecimal totalReceivedAmount = BigDecimal.ZERO;

    /**
     * 未收款总金额
     */
    private BigDecimal totalUnreceivedAmount = BigDecimal.ZERO;
}
