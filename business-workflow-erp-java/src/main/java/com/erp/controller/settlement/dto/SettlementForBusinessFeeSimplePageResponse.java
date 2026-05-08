package com.erp.controller.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 业务费创建专用 - 危废结算单轻量分页查询响应
 */
@Data
public class SettlementForBusinessFeeSimplePageResponse {

    /**
     * 结算单编号（主键）
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
     * 合同编号
     */
    private Integer contractId;

    /**
     * 结算金额
     */
    private BigDecimal settlementAmount;

    /**
     * 状态
     */
    private String status;

    /**
     * 是否已生成业务结算单
     */
    private Boolean hasBusinessFee;
}
