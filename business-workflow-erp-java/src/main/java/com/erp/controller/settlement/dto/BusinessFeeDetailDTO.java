package com.erp.controller.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务费详情DTO
 * 变更说明（2026-04-01）：
 *   - 删除 enablePaymentSelection、splitValuableWorthlessRows
 *   - 明细危废信息统一由 businessFeeItems.wasteInfoList / wasteDetailItems.wasteInfoList 提供
 */
@Data
public class BusinessFeeDetailDTO {

    private Integer businessSeq;
    private String businessCode;
    private BusinessContractInfoDTO businessContractInfo;
    private Integer businessContractId;
    private String serviceCompanyName;
    private EmployeeInfoDTO salespersonInfo;
    private String settlementType;
    private BigDecimal settlementAmount;
    private BigDecimal receivedAmount;
    private LocalDateTime paymentDate;
    private String status;
    private EmployeeInfoDTO creatorInfo;
    private String creatorName;
    private LocalDateTime createTime;
    private EmployeeInfoDTO auditorInfo;
    private LocalDateTime auditTime;
    private String auditOpinion;
    private String remark;
    private Boolean isLocked;
    private LocalDateTime lockTime;
    private LockInfoDTO lockInfo;
    private List<SettlementRelInfoDTO> settlementRels;
    private List<BusinessFeeItemDTO> businessFeeItems;
    private List<WarehousingItemDTO> warehousingItems;
    private List<BusinessFeeWasteDetailDTO> wasteDetailItems;
    private List<OutOfScopeServiceItemDTO> outOfScopeServices;

    @Data
    public static class BusinessContractInfoDTO {
        private Integer contractId;
        private String contractNo;
        private String salespersonName;
        private String bankName;
        private String cardNumber;
        private String accountName;
        private String status;
    }

    @Data
    public static class SettlementRelInfoDTO {
        private Integer relId;
        private Integer settlementId;
        private String settlementCode;
        private BigDecimal settlementAmount;
        private String status;
        private String remark;
    }

    @Data
    public static class EmployeeInfoDTO {
        private Integer employeeId;
        private String employeeName;
    }

    @Data
    public static class LockInfoDTO {
        private Boolean isLocked;
        private LocalDateTime lockTime;
        private String lockUserName;
    }

    @Data
    public static class OutOfScopeServiceItemDTO {
        private Integer outOfScopeServiceId;
        private String project;
        private String spec;
        private String unit;
        private java.math.BigDecimal plannedQuantity;
        private java.math.BigDecimal contractUnitPrice;
        private java.math.BigDecimal settledQuantity;
        private java.math.BigDecimal settledUnitPrice;
        private java.math.BigDecimal settledAmount;
        private String status;
        private String remark;
        private Integer version;
    }
}
