package com.erp.controller.settlement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 结算单审核请求DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class SettlementAuditDTO {

    /**
     * 审核结果：通过/驳回
     */
    @NotBlank(message = "审核结果不能为空")
    private String auditResult;

    /**
     * 审核意见
     */
    private String auditOpinion;
}
