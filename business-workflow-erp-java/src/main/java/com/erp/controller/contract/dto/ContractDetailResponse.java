package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同详情响应
 */
@Data
@ApiModel("合同详情响应")
public class ContractDetailResponse {

    @ApiModelProperty("合同编号")
    private Integer contractId;

    @ApiModelProperty("合同号（业务可见的合同编号：HQ-YYYYMMDD-XXXXX）")
    private String contractNo;

    @ApiModelProperty("客户编码")
    private Integer customerId;

    @ApiModelProperty("客户名称")
    private String enterpriseName;

    @ApiModelProperty("客户快照")
    private ContractCustomerSnapshot customerSnapshot;

    @ApiModelProperty("甲方名称")
    private String partyAName;

    @ApiModelProperty("甲方联系人")
    private String partyAContact;

    @ApiModelProperty("甲方联系电话")
    private String partyAContactPhone;

    @ApiModelProperty("甲方统一社会信用代码")
    private String partyACreditCode;

    @ApiModelProperty("乙方名称")
    private String partyBName;

    @ApiModelProperty("乙方联系人")
    private String partyBContact;

    @ApiModelProperty("乙方联系电话")
    private String partyBContactPhone;

    @ApiModelProperty("乙方统一社会信用代码")
    private String partyBCreditCode;

    @ApiModelProperty("合同金额")
    private BigDecimal contractAmount;

    @ApiModelProperty("是否启用业务费用结算")
    private Boolean feeSettlementEnabled;

    @ApiModelProperty("签订时间")
    private LocalDateTime signTime;

    @ApiModelProperty("合同状态")
    private String contractStatus;

    @ApiModelProperty("合同有效期开始")
    private LocalDateTime validFrom;

    @ApiModelProperty("合同有效期结束")
    private LocalDateTime validTo;

    @ApiModelProperty("编号生成方式（BEFORE_APPROVAL/AFTER_APPROVAL）")
    private String numberGenerationMode;

    @ApiModelProperty("审核人编码")
    private Integer auditorId;

    @ApiModelProperty("审核人姓名")
    private String auditorName;

    @ApiModelProperty("审核时间")
    private LocalDateTime auditTime;

    @ApiModelProperty("寄件日期（合同寄出日期）")
    private LocalDateTime sendDate;

    @ApiModelProperty("收件日期（合同收件日期）")
    private LocalDateTime receiveDate;

    @ApiModelProperty("合同扫描件文件编号")
    private Integer contractFileId;

    @ApiModelProperty("合同扫描件URL")
    private String contractFileUrl;

    @ApiModelProperty("合同扫描件文件名")
    private String contractFileName;

    @ApiModelProperty("合同PDF文件编号（审批后生成的正式合同PDF）")
    private Integer contractPdfFileId;

    @ApiModelProperty("合同PDF文件URL")
    private String contractPdfFileUrl;

    @ApiModelProperty("合同PDF文件名")
    private String contractPdfFileName;

    @ApiModelProperty("扫描件路径（冗余字段，便于快速访问）")
    private String scanFilePath;

    @ApiModelProperty("创建人编码")
    private Integer creatorId;

    @ApiModelProperty("创建人姓名")
    private String creatorName;

    @ApiModelProperty("业务员编码")
    private Integer ownerEmployeeId;

    @ApiModelProperty("业务员姓名")
    private String ownerEmployeeName;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    // 报价单相关字段

    @ApiModelProperty("废物条目列表（层级结构：合同条目 -> 危废条目明细）")
    private List<ContractItemResponse> quotationItems;
    
    @ApiModelProperty(value = "价外服务列表（如有）")
    private List<com.erp.controller.contract.dto.QuotationDetailResponse.OutOfScopeServiceResponse> outOfScopeServices;

    /**
     * 合同条目响应（层级结构）
     */
    @Data
    @ApiModel("合同条目响应")
    public static class ContractItemResponse {
        @ApiModelProperty("合同条目编号")
        private Integer contractItemId;

        @ApiModelProperty("报价条目编号（来源报价，仅追溯，现已可为空）")
        private Integer quotationItemId;

        @ApiModelProperty("合同编号")
        private Integer contractNumber;

        @ApiModelProperty("计价方式（总价包干/按量结算）")
        private String quotationMode;

        @ApiModelProperty("付款方（甲方/乙方）")
        private String payer;

        @ApiModelProperty("计价方案")
        private String pricingPlan;

        @ApiModelProperty("低价备注")
        private String floorPriceRemark;

        @ApiModelProperty("备注")
        private String remark;

        @ApiModelProperty("小计摘要（记录各计量单位小计，JSON数组形式 [{unit,total}]）")
        private String subtotalSummary;

        @ApiModelProperty("创建时间")
        private LocalDateTime createdTime;

        @ApiModelProperty("更新时间")
        private LocalDateTime updatedTime;

        @ApiModelProperty("危废条目明细列表")
        private List<ContractWasteItemResponse> wasteItems;
    }

    /**
     * 合同危废条目明细响应
     */
    @Data
    @ApiModel("合同危废条目明细响应")
    public static class ContractWasteItemResponse {
        @ApiModelProperty("合同危废明细编号")
        private Integer contractWasteItemId;

        @ApiModelProperty("合同条目编号")
        private Integer contractItemId;

        @ApiModelProperty("报价危废明细编号（来源报价，仅追溯，现已可为空）")
        private Integer quotationWasteItemId;

        @ApiModelProperty("危废条目编号")
        private Integer hazardousWasteItemId;

        @ApiModelProperty("废物类别")
        private String wasteCategory;

        @ApiModelProperty("行业来源")
        private String industrySource;

        @ApiModelProperty("废物代码")
        private String wasteCode;

        @ApiModelProperty("危险特性")
        private String hazardFeature;

        @ApiModelProperty("危险废物（危废或固废名称）")
        private String hazardousWaste;

        @ApiModelProperty("形态")
        private String form;

        @ApiModelProperty("计量单位")
        private String unit;

        @ApiModelProperty("计划转移数量")
        private BigDecimal plannedQuantity;

        @ApiModelProperty("单价（合同约定单价，元/计量单位）")
        private BigDecimal unitPrice;

        @ApiModelProperty("金额（合同约定金额）")
        private BigDecimal amount;

        @ApiModelProperty("计价方案（按量结算时使用）")
        private String pricingPlan;

        @ApiModelProperty("付款方（按量结算时使用）")
        private String payer;

        @ApiModelProperty("低价备注")
        private String floorPriceRemark;

        @ApiModelProperty("备注")
        private String remark;

        // ====== 基础/辅助计量单位与换算关系（用于统一按吨结算与表达） ======

        @ApiModelProperty("基础计量单位（如吨，用于统一按基础单位统计与展示）")
        private String baseUnit;

        @ApiModelProperty("基础计量数量（按基础计量单位换算后的数量，如吨）")
        private BigDecimal baseQuantity;

        @ApiModelProperty("基础计价单价（元/基础计量单位，如元/吨）")
        private BigDecimal baseUnitPrice;

        @ApiModelProperty("辅助计量单位（业务友好展示单位，如桶/袋/车等）")
        private String auxUnit;

        @ApiModelProperty("辅助单位每基础单位数量（1计划转移数量≈多少辅助单位，例如1吨≈10桶）")
        private BigDecimal auxPerBase;

        @ApiModelProperty("辅助数量（按辅助计量单位表达的数量，通常对应页面上的桶/袋等数量）")
        private BigDecimal auxQuantity;

        @ApiModelProperty("辅助计价单价（元/辅助计量单位，如元/桶；在合同阶段用于更贴近业务场景的价格表达）")
        private BigDecimal auxUnitPrice;

        @ApiModelProperty("是否启用辅助核算")
        private Boolean enableAuxiliaryAccounting;

        @ApiModelProperty("创建时间")
        private LocalDateTime createdTime;

        @ApiModelProperty("更新时间")
        private LocalDateTime updatedTime;
    }

    /**
     * 报价项内部类（保留用于兼容旧接口）
     */
    @Data
    @ApiModel("报价项")
    @Deprecated
    public static class QuotationItem {
        @ApiModelProperty("危废类别")
        private String hazardousWasteCategory;

        @ApiModelProperty("危废条目编号")
        private Integer hazardousWasteItemId;

        @ApiModelProperty("危废条目名称")
        private String hazardousWasteItemName;

        @ApiModelProperty("单价")
        private BigDecimal unitPrice;

        @ApiModelProperty("计价方式")
        private String pricingMethod;

        @ApiModelProperty("报价明细")
        private String quotationDetail;
    }
}



