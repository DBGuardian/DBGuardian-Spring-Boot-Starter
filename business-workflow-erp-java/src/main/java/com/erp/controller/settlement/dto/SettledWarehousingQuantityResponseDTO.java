package com.erp.controller.settlement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 已结算入库量查询响应DTO
 *
 * @author ERP System
 * @date 2025-01-02
 */
@Data
public class SettledWarehousingQuantityResponseDTO {

    /**
     * 响应数据列表
     */
    private List<SettledQuantityItem> data;

    /**
     * 已结算数量明细项
     */
    @Data
    public static class SettledQuantityItem {
        /**
         * 废物类别（如 HW08）
         */
        private String wasteCategory;

        /**
         * 废物名称
         */
        private String wasteName;

        /**
         * 废物代码
         */
        private String wasteCode;

        /**
         * 当前表格行数（与请求对应）
         */
        private Integer currentRowIndex;

        /**
         * 已结算基本入库量
         */
        private BigDecimal settledBasicQuantity;

        /**
         * 基本计量单位
         */
        private String basicUnit;

        /**
         * 已结算辅助入库量
         */
        private BigDecimal settledAuxiliaryQuantity;

        /**
         * 辅助计量单位
         */
        private String auxiliaryUnit;
    }
}
