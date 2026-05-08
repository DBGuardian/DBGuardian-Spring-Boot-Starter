package com.erp.controller.finance.dto;

import lombok.Data;

/**
 * 合同条目payer信息DTO
 * 用于批量查询合同条目的payer信息，避免N+1查询
 */
@Data
public class ContractItemPayerDTO {
    /**
     * 合同条目编号
     */
    private Integer contractItemId;

    /**
     * 付款方
     */
    private String payer;
}
