package com.erp.controller.settlement.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务费详情保存基类DTO
 * 用于承载业务费详情新增/修改共用字段。
 *
 * 变更说明（2026-04-02）：
 *   - 从原“业务费通用保存请求 DTO”收敛为保存基类
 *   - 新增链路使用 BusinessFeeCreateDTO
 *   - 修改链路使用 BusinessFeeDetailUpdateDTO
 */
@Data
public class BusinessFeeDetailSaveBaseDTO {

    private Integer businessSeq;
    private Integer businessContractId;
    private String businessContractNo;
    private Integer salespersonId;
    private String salespersonName;

    @Size(max = 100, message = "服务公司名称长度不能超过100个字符")
    private String serviceCompanyName;

    private String settlementType;
    private BigDecimal settlementAmount;
    private BigDecimal receivedAmount;
    private LocalDateTime paymentDate;
    private String status;

    @Size(max = 255, message = "审核意见长度不能超过255个字符")
    private String auditOpinion;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    private Boolean isLocked;
    private Integer version;

    @Valid
    private List<SettlementRelSaveDTO> settlementRels;

    @Valid
    private List<BusinessFeeItemSaveDTO> businessFeeItems;

    @Valid
    private List<OutOfScopeServiceDTO> outOfScopeServices;

    @Data
    public static class SettlementRelSaveDTO {
        private Integer relId;
        private Integer settlementId;
        private String settlementCode;
        private String remark;
    }

    @Data
    public static class BusinessFeeItemSaveDTO {
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
        private Integer version;
        private List<WasteInfoSaveDTO> wasteInfoList;
    }

    @Data
    public static class WasteInfoSaveDTO {
        private Integer wasteInfoId;
        private Integer rowOrder;
        private List<Integer> sourceWasteDetailIds;
        private String wasteCategory;
        private String wasteCode;
        private String wasteName;
    }

    @Data
    public static class OutOfScopeServiceDTO {
        private Integer outOfScopeServiceId;
        @Size(max = 100, message = "服务项目长度不能超过100个字符")
        private String project;
        @Size(max = 100, message = "规格长度不能超过100个字符")
        private String spec;
        @Size(max = 20, message = "基本单位长度不能超过20个字符")
        private String basicUnit;
        private BigDecimal plannedQuantity;
        private BigDecimal contractUnitPrice;
        private BigDecimal basicSettlementQuantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        @Size(max = 255, message = "备注长度不能超过255个字符")
        private String remark;
        private Integer version;
    }
}
