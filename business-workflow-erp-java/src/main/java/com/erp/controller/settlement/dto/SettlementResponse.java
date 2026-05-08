package com.erp.controller.settlement.dto;

import lombok.Data;

/**
 * 结账响应
 */
@Data
public class SettlementResponse {
    /**
     * 组织ID
     */
    private Long organizationId;

    /**
     * 结账是否成功
     */
    private Boolean settlementSuccess;
}

