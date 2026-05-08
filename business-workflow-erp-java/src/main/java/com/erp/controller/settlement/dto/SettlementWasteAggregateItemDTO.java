package com.erp.controller.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 按废物聚合的入库重量行 DTO
 * <p>
 * 按 废物类别 + 废物代码 + 废物名称 分组，合并有价类/无价类重量合计。
 * 同时保留来源危废明细编号集合，供前端回填 BUSINESS_FEE_ITEM_WASTE_INFO.sourceWasteDetailIds。
 */
@Data
public class SettlementWasteAggregateItemDTO {

    /**
     * 废物类别（如 HW08，来自 SETTLEMENT_WASTE_INFO.废物类别）
     */
    private String wasteCategory;

    /**
     * 废物代码（如 251-001-08，来自 SETTLEMENT_WASTE_INFO.废物代码）
     */
    private String wasteCode;

    /**
     * 废物名称（来自 SETTLEMENT_WASTE_INFO.废物名称）
     */
    private String wasteName;

    /**
     * 来源危废明细编号列表（SETTLEMENT_WASTE_DETAIL.明细编号）
     */
    private List<Integer> sourceWasteDetailIds;

    /**
     * 有价类重量合计（吨，SUM WAREHOUSING_WASTE_ITEM.有价类重量）
     */
    private BigDecimal valuableWeight;

    /**
     * 无价类重量合计（吨，SUM WAREHOUSING_WASTE_ITEM.无价类重量）
     */
    private BigDecimal worthlessWeight;

    /**
     * 总重量（有价 + 无价）
     */
    private BigDecimal totalWeight;

    /**
     * 是否启用辅助核算
     */
    private Boolean enableAuxiliaryAccounting;

    /**
     * 基本结算数量（吨）
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
     * 辅助计量单位（桶/袋/车等）
     */
    private String auxiliaryUnit;

    /**
     * 结算单价（元/吨）
     */
    private BigDecimal settlementUnitPrice;

    /**
     * 本次明细金额（元）
     */
    private BigDecimal settlementAmount;
}
