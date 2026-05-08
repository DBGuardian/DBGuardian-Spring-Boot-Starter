package com.erp.controller.finance.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.erp.controller.settlement.dto.SettlementWasteInfoDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 废物结算明细详情DTO
 * 根据前端WasteSettlementDetail接口结构设计
 *
 * 表结构变更说明：
 * - 废物类别、废物代码、废物名称统一使用 wasteInfoList 数组管理
 * - 父子层级结构（parentDetailId、isContractLevel、settlementMode）已移除
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class WasteSettlementDetailDTO {

    // ===== 基础信息 =====
    /** 接收日期：格式为 YYYY-MM-DD */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate receiveDate;

    /** 备注信息 */
    private String remark;

    // ===== 关联来源信息 =====
    /** 关联来源类型：CONTRACT/WAREHOUSING/TRANSPORT */
    private String sourceType;

    /** 关联来源单号（入库单号/运输单号） */
    private String sourceCode;

    /** 广东省联单号 */
    private String gdManifestCode;

    // ===== 合同关联信息 =====
    /** 合同条目ID：关联合同项 */
    private Long contractItemId;

    // ===== 数量和单位信息 =====
    /** 基本结算数量：按量结算时的实际数量 */
    private BigDecimal basicSettlementQuantity;

    /** 基本计量单位：如 "吨" */
    private String basicUnit;

    /** 辅助结算数量：启用辅助核算时的数量 */
    private BigDecimal auxiliarySettlementQuantity;

    /** 辅助计量单位：如 "桶"、"立方" */
    private String auxiliaryUnit;

    // ===== 计划和累积信息 =====
    /** 合同计划总量：-1表示不限量 */
    private BigDecimal contractPlanTotal;

    /** 辅助合同计划总量：-1表示不限量 */
    private BigDecimal auxiliaryContractPlanTotal;

    /** 累积已结算量 */
    private BigDecimal settledBasicQuantity;

    /** 辅助累积已结算量 */
    private BigDecimal settledAuxiliaryQuantity;

    /** 本次累积量 */
    private BigDecimal currentAccumulatedQuantity;

    // ===== 价格信息 =====
    /** 单价：按量结算时的单价 */
    private BigDecimal unitPrice;

    /** 保存原始单价 */
    private BigDecimal saveUnitPrice;

    /** 辅助单价：按量结算时的单价 */
    private BigDecimal auxiliaryUnitPrice;

    /** 超量单价：超出计划量的单价 */
    private BigDecimal excessUnitPrice;

    /** 超量金额：超出量 × 超量单价 */
    private BigDecimal excessAmount;

    /** 结算金额：最终计算出的金额 */
    private BigDecimal amount;

    // ===== 辅助核算信息 =====
    /** 是否启用辅助核算：控制是否使用辅助单位结算 */
    private Boolean enableAuxiliaryAccounting;

    // ===== 业务信息 =====
    /** 付款方向：RECEIVABLE=收款，PAYABLE=付款 */
    private String payer;

    /** 省份联单号：入库时的省份信息 */
    private String provinceNo;

    // ===== 危废信息列表（从 SETTLEMENT_WASTE_INFO 表获取）=====
    /** 危废信息列表：废物类别、废物代码、废物名称 */
    private List<SettlementWasteInfoDTO> wasteInfoList;

    // ===== 数据库字段（用于数据传输）=====
    /** 明细ID */
    private Long detailId;

    /** 序号 */
    private Integer sequence;

    /** 超出量 */
    private BigDecimal excessQuantity;

    /** 是否锁定 */
    private Boolean isLocked;

    /** 操作类型：CREATE/UPDATE/DELETE（用于增量更新） */
    private String operation;
}
