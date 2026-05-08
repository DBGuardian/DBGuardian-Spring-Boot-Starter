package com.erp.controller.settlement.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 结账请求
 */
@Data
public class SettlementRequest {
    /**
     * 组织ID（组织级别结账）或账期ID（单个账期结账）
     */
    @NotNull(message = "ID不能为空")
    private Long organizationId;

    /**
     * 单个账期结账时使用的账期ID（与 organizationId 二选一）
     */
    private Long periodId;
}

