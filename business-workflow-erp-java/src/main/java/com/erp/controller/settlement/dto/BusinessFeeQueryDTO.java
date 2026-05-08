package com.erp.controller.settlement.dto;

import lombok.Data;

/**
 * 业务费查询DTO
 * 变更说明（2025-03-27）：
 *   - 删除 contractId、settlementId（原单值危废结算单筛选，改由 dangerousSettlementId 替代语义更清晰）
 *   - 新增 businessContractId（按业务合同筛选）
 *   - 新增 dangerousSettlementId（按关联的危废结算单筛选，通过关联表查询）
 */
@Data
public class BusinessFeeQueryDTO {

    /** 当前页 */
    private Integer current = 1;

    /** 每页大小 */
    private Integer size = 10;

    /** 结算类型：RECEIVABLE=收款 / PAYABLE=付款 */
    private String settlementType;

    /** 状态 */
    private String status;

    /** 业务单号 */
    private String businessCode;

    /** 业务合同ID（BUSINESS_CONTRACT.合同编号） */
    private Integer businessContractId;

    /**
     * 危废结算单编号（通过 BUSINESS_FEE_SETTLEMENT_REL 关联表过滤）
     */
    private Integer dangerousSettlementId;

    /** 业务员编码 */
    private Integer salespersonId;

    /** 制单人编码（用于权限过滤） */
    private Integer creatorId;

    /** 是否仅查询独立业务费结算单（未关联业务合同且无危废结算单关联） */
    private Boolean independentOnly;

    /**
     * 数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断
     */
    private String viewScope;
}
