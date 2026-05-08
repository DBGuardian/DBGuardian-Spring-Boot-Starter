package com.erp.controller.settlement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算单查询结果DTO
 * 用于开票通知单选择结算单时的数据传输
 *
 * @author ERP System
 * @date 2026-01-23
 */
@Data
public class SettlementQueryResultDTO {

    /**
     * 结算单编号
     */
    private Long settlementId;

    /**
     * 结算单单号
     */
    private String settlementCode;

    /**
     * 合同号
     */
    private String contractCode;

    /**
     * 结算金额
     */
    private BigDecimal settlementAmount;

    /**
     * 已收金额
     */
    private BigDecimal receivedAmount;

    /**
     * 状态
     */
    private String status;

    /**
     * 结算周期起
     */
    private LocalDateTime settlementPeriodStart;

    /**
     * 结算周期止
     */
    private LocalDateTime settlementPeriodEnd;

    /**
     * 可开蓝字金额
     */
    private BigDecimal availableBlueAmount;

    /**
     * 可红冲金额
     */
    private BigDecimal availableRedAmount;

    /**
     * 结算类型
     */
    private String settlementType;

    /**
     * 关联来源类型
     */
    private String sourceType;
}