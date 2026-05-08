package com.erp.controller.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 业务费危废详情统一DTO
 * 变更说明（2025-03-27）：
 *   - settlementWasteDetailIds 改为单值 settlementWasteDetailId（明细行精确关联一条）
 *   - 删除 contractItemId、contractWasteItemId（不再从危废合同维度关联）
 *   - 新增 paymentDirection（本行收付款方向）
 *   - 新增 wasteInfoList（总价包干时的多条危废信息子表列表）
 */
@Data
public class BusinessFeeWasteDetailDTO {

    // ===== 基础标识字段 =====

    /** 业务序号 */
    private Integer businessSeq;

    /** 业务费明细序号 */
    private Integer businessFeeItemSeq;

    /** 关联危废结算单编号 */
    private Integer settlementId;

    /**
     * 关联危废结算明细编号（SETTLEMENT_WASTE_DETAIL.明细编号）
     * 同一明细编号可出现多行（收/付款方向不同）
     */
    private Integer settlementWasteDetailId;

    /** 入库危废明细编号 */
    private Integer warehousingItemId;

    // ===== 业务费自身字段（计费信息） =====

    /** 结算类型：RECEIVABLE=收款 / PAYABLE=付款（主表整体方向） */
    private String settlementType;

    /** 本行收付款方向：RECEIVABLE=收款 / PAYABLE=付款（行级，可与主表不同） */
    private String paymentDirection;

    /** 结算模式：FIXED_PRICE=总价包干 / QUANTITY_BASED=按量结算 / 按量子项 */
    private String settlementMode;

    /** 是否分行显示重量 */
    private Boolean splitValuableWorthlessRows;

    /** 底价单价（元/单位） */
    private BigDecimal baseUnitPrice;

    /** 有价类单价（元/单位） */
    private BigDecimal valuableUnitPrice;

    /** 无价类单价（元/单位） */
    private BigDecimal worthlessUnitPrice;

    /** 合同底价/包干金额（元） */
    private BigDecimal contractBasePrice;

    /** 有价类合同底价 */
    private BigDecimal valuableContractBasePrice;

    /** 无价类合同底价 */
    private BigDecimal worthlessContractBasePrice;

    /** 中间费（元） */
    private BigDecimal intermediaryFee;

    /** 返点比例 */
    private BigDecimal rebateRatio;

    /** 应付金额（元） */
    private BigDecimal payableAmount;

    /** 有价类应付金额（元） */
    private BigDecimal valuablePayableAmount;

    /** 无价类应付金额（元） */
    private BigDecimal worthlessPayableAmount;

    /** 有价重量（吨） */
    private BigDecimal valuableWeight;

    /** 无价重量（吨） */
    private BigDecimal worthlessWeight;

    /** 货款结算金额（元） */
    private BigDecimal cargoSettlementAmount;

    /** 是否启用辅助核算 */
    private Boolean enableAuxAccounting;

    /** 基本核算数量 */
    private BigDecimal basicQuantity;

    /** 辅助核算数量 */
    private BigDecimal auxiliaryQuantity;

    /** 辅助计量单位 */
    private String auxiliaryUnit;

    // ===== 危废信息（按量结算时为单条快照，总价包干时见 wasteInfoList） =====

    /**
     * 危废代码快照（按量结算时填写；总价包干时为空，见 wasteInfoList）
     */
    private String wasteCode;

    /**
     * 危废名称快照（按量结算时填写；总价包干时为空）
     */
    private String wasteName;

    /**
     * 危废类别快照（按量结算时填写；总价包干时为空）
     */
    private String wasteCategory;

    /**
     * 总价包干模式下的多条危废信息列表（来自 BUSINESS_FEE_ITEM_WASTE_INFO 子表）
     * 按量结算时此字段为空
     */
    private List<BusinessFeeItemDTO.BusinessFeeItemWasteInfoDTO> wasteInfoList;

    // ===== 入库明细相关字段 =====

    /** 入库单号 */
    private String warehousingNo;

    /** 入库侧废物名称 */
    private String warehousingWasteName;

    /** 入库侧废物代码 */
    private String warehousingWasteCode;

    /** 入库侧废物类别/形态 */
    private String warehousingWasteCategory;

    /** 入库侧是否启用辅助核算 */
    private Boolean warehousingEnableAuxAccounting;

    /** 入库侧基本核算数量 */
    private BigDecimal warehousingBasicQuantity;

    /** 入库侧辅助核算数量 */
    private BigDecimal warehousingAuxiliaryQuantity;

    /** 入库侧辅助计量单位 */
    private String warehousingAuxiliaryUnit;

    // ===== 结算危废明细相关字段 =====

    /** 结算侧废物类别 */
    private String settlementWasteCategory;

    /** 结算侧废物代码 */
    private String settlementWasteCode;

    /** 结算侧废物名称 */
    private String settlementWasteName;

    /** 是否启用辅助核算（结算单危废明细） */
    private Boolean settlementEnableAuxiliaryAccounting;

    /** 基本结算数量 */
    private BigDecimal basicSettlementQuantity;

    /** 基本计量单位 */
    private String basicSettlementUnit;

    /** 辅助结算数量 */
    private BigDecimal auxiliarySettlementQuantity;

    /** 辅助计量单位（结算侧） */
    private String settlementAuxiliaryUnit;

    /** 结算单价（元） */
    private BigDecimal settlementUnitPrice;

    /** 结算金额（元） */
    private BigDecimal settlementAmount;
}
