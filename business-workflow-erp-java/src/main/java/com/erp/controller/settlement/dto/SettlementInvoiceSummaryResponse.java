package com.erp.controller.settlement.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 结算单发票汇总信息响应DTO
 * 用于展示结算单已关联发票的金额汇总信息
 *
 * @author ERP System
 * @date 2026-01-24
 */
@Data
public class SettlementInvoiceSummaryResponse {
    /**
     * 结算单编号
     */
    private Long settlementId;

    /**
     * 结算单单号
     */
    private String settlementNo;

    /**
     * 结算金额
     */
    private BigDecimal settlementAmount;

    /**
     * 已关联蓝字总额
     */
    private BigDecimal blueSum;

    /**
     * 已关联红字总额
     */
    private BigDecimal redSum;

    /**
     * 发票净额（蓝字总额 - 红字总额）
     */
    private BigDecimal netAmount;

    /**
     * 可开蓝字金额（结算金额 - 发票净额）
     */
    private BigDecimal canInvoiceAmount;

    /**
     * 可开红字金额（已开蓝字金额 - 已红冲金额）
     */
    private BigDecimal canRedAmount;
}