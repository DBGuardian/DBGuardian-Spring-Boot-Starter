package com.erp.controller.settlement.dto;

import lombok.Data;

/**
 * 业务费创建专用 - 危废结算单分页查询请求
 */
@Data
public class SettlementForBusinessFeePageRequest {

    /**
     * 当前页，默认1
     */
    private Integer current = 1;

    /**
     * 每页大小，默认10
     */
    private Integer size = 10;

    /**
     * 结算单编号（精确匹配）
     */
    private Long settlementId;

    /**
     * 结算单单号（模糊匹配）
     */
    private String settlementCode;

    /**
     * 结算类型：RECEIVABLE=收款 / PAYABLE=付款
     */
    private String settlementType;

    /**
     * 状态（精确匹配）
     */
    private String status;

    /**
     * 制单人名称（模糊匹配）
     */
    private String creatorName;

    /**
     * 合同编号（精确匹配，用于按危废合同过滤）
     */
    private Integer contractId;

    /**
     * 业务合同编号（精确匹配，用于收窄 hasBusinessFee 聚合范围）
     * 传入后 bfCount 子查询只统计该业务合同下已关联的危废结算单，避免全表 GROUP BY
     */
    private Integer businessContractId;

    /**
     * 是否仅查询未关联危废合同的危废结算单
     */
    private Boolean unlinkedHazardousContractOnly;
}
