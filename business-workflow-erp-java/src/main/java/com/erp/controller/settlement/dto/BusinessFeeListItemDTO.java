package com.erp.controller.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务费列表项DTO
 * 变更说明（2025-03-27）：
 *   - 删除 contractId/contractCode、settlementTableName、settlementId/settlementCode（单值）
 *   - 新增 businessContractNo、salespersonName（快照冗余字段）
 *   - 新增 settlementRels（关联危废结算单列表，替代原单值）
 */
@Data
public class BusinessFeeListItemDTO {

    /** 业务序号 */
    private Integer businessSeq;

    /** 业务单号 */
    private String businessCode;

    /** 关联业务合同ID */
    private Integer businessContractId;

    /** 业务合同单号 */
    private String businessContractNo;

    /** 服务公司名称 */
    private String serviceCompanyName;

    /** 业务员编码 */
    private Integer salespersonId;

    /** 业务员姓名 */
    private String salespersonName;

    /** 结算类型：RECEIVABLE=收款 / PAYABLE=付款 */
    private String settlementType;

    /** 结算模式：FIXED_PRICE=总价包干 / QUANTITY_BASED=按量结算 */
    private String settlementMode;

    /** 业务结算金额（元） */
    private BigDecimal settlementAmount;

    /** 已收/已付金额 */
    private BigDecimal receivedAmount;

    /** 状态 */
    private String status;

    /** 制单人编码（用于"仅操作自己"行级权限判断） */
    private Integer creatorId;

    /** 制单人名称 */
    private String creatorName;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 审核人名称 */
    private String auditorName;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核意见 */
    private String auditOpinion;

    /**
     * 关联危废结算单摘要列表（替代原单值 settlementId/settlementCode）
     * 一个业务结算单可关联多个危废结算单
     */
    private List<SettlementRelSummaryDTO> settlementRels;

    /** 危废结算单摘要（列表展示用） */
    @Data
    public static class SettlementRelSummaryDTO {
        /** 危废结算单编号 */
        private Integer settlementId;
        /** 危废结算单单号 */
        private String settlementCode;
    }
}
