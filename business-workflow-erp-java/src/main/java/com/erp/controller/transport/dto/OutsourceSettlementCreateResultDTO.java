package com.erp.controller.transport.dto;

import lombok.Data;

/**
 * 委外运输结算单创建结果DTO
 */
@Data
public class OutsourceSettlementCreateResultDTO {

    /**
     * 主结算单编号（返回给前端用于跳转）
     */
    private Integer settlementId;

    /**
     * 结算单编号（主单，与 settlementId 相同）
     */
    private Integer businessSeq;

    /**
     * 是否拆分（是否有收付款两个方向）
     */
    private Boolean split;

    /**
     * 收款结算单编号
     */
    private Integer receivableSettlementId;

    /**
     * 付款结算单编号
     */
    private Integer payableSettlementId;
}
