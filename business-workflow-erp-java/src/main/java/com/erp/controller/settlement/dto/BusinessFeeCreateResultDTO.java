package com.erp.controller.settlement.dto;

import lombok.Data;

/**
 * 业务费新增结果DTO
 */
@Data
public class BusinessFeeCreateResultDTO {

    /** 主返回业务序号 */
    private Integer businessSeq;

    /** 是否发生拆分 */
    private Boolean split;

    /** 收款单业务序号 */
    private Integer receivableBusinessSeq;

    /** 付款单业务序号 */
    private Integer payableBusinessSeq;
}
