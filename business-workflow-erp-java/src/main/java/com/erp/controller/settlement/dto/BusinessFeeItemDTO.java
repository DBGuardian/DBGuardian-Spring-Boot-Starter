package com.erp.controller.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务结算明细DTO
 * 变更说明（2026-04-01）：
 *   - 删除 settlementId、wasteCode、wasteName、wasteCategory
 *   - 危废信息统一通过 wasteInfoList 提供
 *   - 子表删除数量/单位字段
 */
@Data
public class BusinessFeeItemDTO {

    private Integer itemSeq;
    private Integer businessSeq;
    private String paymentDirection;
    private String settlementMode;
    private BigDecimal baseUnitPrice;
    private BigDecimal valuableUnitPrice;
    private BigDecimal worthlessUnitPrice;
    private BigDecimal contractBasePrice;
    private BigDecimal valuableContractBasePrice;
    private BigDecimal worthlessContractBasePrice;
    private BigDecimal intermediaryFee;
    private BigDecimal rebateRatio;
    private BigDecimal payableAmount;
    private BigDecimal valuablePayableAmount;
    private BigDecimal worthlessPayableAmount;
    private BigDecimal valuableWeight;
    private BigDecimal worthlessWeight;
    private BigDecimal cargoSettlementAmount;
    private Boolean enableAuxAccounting;
    private BigDecimal basicQuantity;
    private BigDecimal auxiliaryQuantity;
    private String auxiliaryUnit;
    private Integer creatorId;
    private LocalDateTime createTime;
    private Integer updaterId;
    private LocalDateTime updateTime;
    private List<BusinessFeeItemWasteInfoDTO> wasteInfoList;

    @Data
    public static class BusinessFeeItemWasteInfoDTO {
        private Integer wasteInfoId;
        private Integer rowOrder;
        /**
         * 当前危废子项对应的来源危废明细编号数组快照（SETTLEMENT_WASTE_DETAIL.明细编号），
         * 仅用于保存溯源数据与页面回显。
         */
        private List<Integer> sourceWasteDetailIds;
        private String wasteCategory;
        private String wasteCode;
        private String wasteName;
    }
}
