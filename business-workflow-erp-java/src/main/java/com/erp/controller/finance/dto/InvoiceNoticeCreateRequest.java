package com.erp.controller.finance.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 创建开票通知单请求DTO
 *
 * @author ERP System
 * @date 2026-01-06
 */
@Data
public class InvoiceNoticeCreateRequest {

    /**
     * 合同ID
     */
    @NotNull(message = "合同ID不能为空")
    private Integer contractId;

    /**
     * 合同号
     */
    private String contractNo;

    /**
     * 合同名称
     */
    private String contractName;

    /**
     * 客户ID
     */
    private Integer customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 主结算单编号
     */
    private Integer mainSettlementId;

    /**
     * 已绑定结算摘要（JSON格式）
     */
    private String boundSettlementSummary;

    /**
     * 开票类型
     */
    private String invoiceType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态（可选，创建时统一落为"待审核"）
     */
    private String status;
}

