package com.erp.controller.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * 已结算废物明细VO - 用于SQL合并查询结果
 *
 * @author ERP System
 * @date 2026-01-22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettledWasteDetailVO {

    /**
     * 废物类别（如 HW08）
     */
    private String wasteCategory;

    /**
     * 废物名称
     */
    private String wasteName;

    /**
     * 废物代码（如 900-214-08）
     */
    private String wasteCode;

    /**
     * 累积基本结算数量
     */
    private BigDecimal totalBasicQuantity;

    /**
     * 累积辅助结算数量
     */
    private BigDecimal totalAuxiliaryQuantity;

    /**
     * 基本计量单位
     */
    private String basicUnit;

    /**
     * 辅助计量单位
     */
    private String auxiliaryUnit;
}
