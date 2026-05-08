package com.erp.controller.transport.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 结算单关联总磅单VO
 */
@Data
public class OutsourceTransportSettlementSlipVO {

    /**
     * 关联编号
     */
    private Integer relationId;

    /**
     * 结算单编号
     */
    private Integer settlementId;

    /**
     * 结算单单号
     */
    private String settlementNo;

    /**
     * 总磅单编号
     */
    private Integer slipId;

    /**
     * 总磅单号
     */
    private String slipCode;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
