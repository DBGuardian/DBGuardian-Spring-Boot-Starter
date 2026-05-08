package com.erp.controller.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 业务费创建专用 - 危废结算单分页查询响应
 */
@Data
public class SettlementForBusinessFeePageResponse {

    /**
     * 结算单编号（主键）
     */
    private Long settlementId;

    /**
     * 结算单单号（如 JSD-20251228-0001）
     */
    private String settlementCode;

    /**
     * 合同号
     */
    private String contractCode;

    /**
     * 合同编号
     */
    private Integer contractId;

    /**
     * 甲方名称（客户名称）
     */
    private String customerName;

    /**
     * 结算类型：RECEIVABLE=收款 / PAYABLE=付款
     */
    private String settlementType;

    /**
     * 关联来源类型：CONTRACT/WAREHOUSING
     */
    private String sourceType;

    /**
     * 结算金额
     */
    private BigDecimal settlementAmount;

    /**
     * 状态
     */
    private String status;

    /**
     * 制单人编码
     */
    private Integer creatorId;

    /**
     * 制单人名称
     */
    private String creatorName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 审核人编码
     */
    private Integer auditorId;

    /**
     * 审核人姓名
     */
    private String auditorName;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

    /**
     * 审核意见
     */
    private String auditOpinion;

    /**
     * 是否已生成业务结算单
     * true = 已存在关联的 BUSINESS_FEE_HEADER 记录
     */
    private Boolean hasBusinessFee;

    /**
     * 已关联的业务结算单数量（0表示未生成）
     */
    private Integer businessFeeCount;
}
