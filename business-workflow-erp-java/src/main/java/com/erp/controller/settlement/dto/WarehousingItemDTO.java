package com.erp.controller.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 入库明细DTO
 * 用于业务费详情页展示关联的入库数据
 */
@Data
public class WarehousingItemDTO {

    /**
     * 入库危废明细编号
     */
    private Integer itemId;

    /**
     * 入库单号
     */
    private String warehousingNo;

    /**
     * 废物名称
     */
    private String wasteName;

    /**
     * 废物代码
     */
    private String wasteCode;

    /**
     * 废物形态/类别
     */
    private String wasteCategory;

    /**
     * 有价类重量（吨）
     */
    private BigDecimal valuableWeight;

    /**
     * 无价类重量（吨）
     */
    private BigDecimal worthlessWeight;

    /**
     * 是否启用辅助核算
     */
    private Boolean enableAuxAccounting;

    /**
     * 基本核算数量
     */
    private BigDecimal basicQuantity;

    /**
     * 辅助核算数量
     */
    private BigDecimal auxiliaryQuantity;

    /**
     * 辅助计量单位
     */
    private String auxiliaryUnit;

    /**
     * 结算单危废明细ID（用于精确匹配结算单危废明细）
     */
    private Long settlementWasteDetailId;
}
