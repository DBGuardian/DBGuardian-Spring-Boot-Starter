package com.erp.controller.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 结算危废明细DTO
 *
 * 对应表：SETTLEMENT_WASTE_DETAIL
 * 注意：废物类别、废物代码、废物名称已移至 SettlementWasteInfoDTO
 *
 * 验证规则：
 * - 所有字段都可以为空（新增/修改时）
 * - 只有填写了数据才验证数据格式
 * - 审核时需要验证业务完整性
 */
@Data
public class SettlementWasteDetailDTO {

    /**
     * 明细ID
     */
    private Long detailId;

    /**
     * 序号
     */
    private Integer sequence;

    /**
     * 接收日期
     */
    private LocalDate receiveDate;

    /**
     * 关联来源类型：CONTRACT/WAREHOUSING/TRANSPORT
     */
    private String sourceType;

    /**
     * 关联来源单号（入库单号/运输单号）
     */
    private String sourceCode;

    /**
     * 广东省联单号
     */
    private String gdManifestCode;

    /**
     * 是否启用辅助核算
     */
    private Boolean enableAuxiliaryAccounting;

    /**
     * 基本结算数量
     */
    private BigDecimal basicSettlementQuantity;

    /**
     * 基本计量单位
     */
    private String basicUnit;

    /**
     * 辅助结算数量
     */
    private BigDecimal auxiliarySettlementQuantity;

    /**
     * 辅助计量单位
     */
    private String auxiliaryUnit;

    /**
     * 合同条目ID（仅溯源）
     */
    private Integer contractItemId;

    /**
     * 合同计划总量（包干使用）
     */
    private BigDecimal contractPlanTotal;

    /**
     * 辅助合同计划总量（包干使用）
     */
    private BigDecimal auxiliaryContractPlanTotal;

    /**
     * 累积已结算量
     */
    private BigDecimal settledBasicQuantity;

    /**
     * 辅助累积已结算量
     */
    private BigDecimal settledAuxiliaryQuantity;

    /**
     * 本次累积量
     */
    private BigDecimal currentAccumulatedQuantity;

    /**
     * 超出量
     */
    private BigDecimal excessQuantity;

    /**
     * 结算单价
     */
    private BigDecimal unitPrice;

    /**
     * 超出单价
     */
    private BigDecimal excessUnitPrice;

    /**
     * 保存原始单价
     */
    private BigDecimal saveUnitPrice;

    /**
     * 保存辅助单价
     */
    private BigDecimal auxiliaryUnitPrice;

    /**
     * 超出金额
     */
    private BigDecimal excessAmount;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否锁定
     */
    private Boolean isLocked;

    /**
     * 操作类型：CREATE（新增）/UPDATE（修改）/DELETE（删除）
     */
    private String operation;

    /**
     * 危废信息（废物类别、废物代码、废物名称）
     */
    private SettlementWasteInfoDTO wasteInfo;

    /**
     * 危废信息列表（支持多条废物信息）
     */
    private List<SettlementWasteInfoDTO> wasteInfoList;

    /**
     * 付款方：甲方/乙方
     */
    private String payer;
}
