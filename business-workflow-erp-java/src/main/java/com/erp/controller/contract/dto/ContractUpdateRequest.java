package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 更新合同请求
 *
 * 宽松验证策略：
 * - 创建/编辑时：大部分字段为可选项，允许先保存草稿
 * - 审核时：由前端进行严格验证，后端仅在审核接口进行最终校验
 */
@Data
@ApiModel("更新合同请求")
public class ContractUpdateRequest {

    @ApiModelProperty(value = "合同编号", required = true)
    private Integer contractId;

    @ApiModelProperty(value = "客户编码（可为空，用于手填甲方信息时不强制关联客户表）")
    private Integer customerId;

    @ApiModelProperty(value = "客户快照（需要更新临时客户信息或抬头时传入）")
    private ContractCustomerSnapshot customerSnapshot;

    @ApiModelProperty(value = "甲方名称")
    private String partyAName;

    @ApiModelProperty(value = "甲方联系人")
    private String partyAContact;

    @ApiModelProperty(value = "甲方联系电话")
    private String partyAContactPhone;

    @ApiModelProperty(value = "甲方统一社会信用代码")
    private String partyACreditCode;

    @ApiModelProperty(value = "乙方名称")
    private String partyBName;

    @ApiModelProperty(value = "乙方联系人")
    private String partyBContact;

    @ApiModelProperty(value = "乙方联系电话")
    private String partyBContactPhone;

    @ApiModelProperty(value = "乙方统一社会信用代码")
    private String partyBCreditCode;

    @ApiModelProperty(value = "合同金额")
    private BigDecimal contractAmount;

    @ApiModelProperty(value = "是否启用业务费用结算")
    private Boolean feeSettlementEnabled;

    @ApiModelProperty(value = "签订时间")
    private LocalDateTime signTime;

    @ApiModelProperty(value = "合同状态：待审核/已通过/执行中/已完结/已归档/已驳回")
    private String contractStatus;

    @ApiModelProperty(value = "合同有效期开始")
    private LocalDateTime validFrom;

    @ApiModelProperty(value = "合同有效期结束")
    private LocalDateTime validTo;

    @ApiModelProperty(value = "编号生成方式（BEFORE_APPROVAL/AFTER_APPROVAL）")
    private String numberGenerationMode;

    @ApiModelProperty(value = "寄件日期（合同寄出日期）")
    private LocalDateTime sendDate;

    @ApiModelProperty(value = "收件日期（合同收件日期）")
    private LocalDateTime receiveDate;

    @ApiModelProperty(value = "扫描件路径（冗余字段，便于快速访问）")
    private String scanFilePath;

    @ApiModelProperty(value = "业务员编码")
    private Integer ownerEmployeeId;

    @ApiModelProperty(value = "备注")
    private String remark;

    // 合同条目及危废明细（无报价单关联）
    @ApiModelProperty(value = "废物条目列表（层级结构：合同条目 -> 危废条目明细）")
    @Valid
    private List<ContractItemRequest> quotationItems;

    @ApiModelProperty(value = "价外服务列表（可选），在更新合同时一并同步（差分同步）")
    private List<com.erp.controller.contract.dto.OutOfScopeServiceCreateDTO> outOfScopeServices;

    /**
     * 合同报价条目请求（层级结构中的父项）
     *
     * 宽松验证策略：
     * - 创建/编辑时：计价方式和付款方为可选项
     * - 审核时：由前端进行严格验证
     */
    @Data
    @ApiModel("合同报价条目请求")
    public static class ContractItemRequest {
        @ApiModelProperty(value = "合同条目编号（更新时使用）")
        private Integer contractItemId;

        @ApiModelProperty(value = "报价条目编号（来源的报价条目，用于追溯）")
        private Integer quotationItemId;

        @ApiModelProperty(value = "计价方式（总价包干/按量结算，可为空）")
        private String quotationMode;

        @ApiModelProperty(value = "付款方：甲方/乙方/共同（可为空）")
        private String payer;

        @ApiModelProperty(value = "计价方案（总价包干时使用）")
        private String pricingPlan;

        @ApiModelProperty(value = "低价备注（在新增合同时处理低价备注，PACKAGE模式必填，UNIT模式可空）")
        private String floorPriceRemark;

        @ApiModelProperty(value = "备注（总价包干时使用）")
        private String remark;

        @ApiModelProperty(value = "小计摘要（记录各计量单位小计，JSON数组形式 [{unit,total}]，由前端计算并传递）")
        private String subtotalSummary;

        @ApiModelProperty(value = "危废条目明细列表（按量结算时必填）")
        @Valid
        private List<ContractWasteItemRequest> wasteItems;
    }

    /**
     * 合同危废条目明细请求
     *
     * 宽松验证策略：
     * - 创建/编辑时：大部分字段为可选项
     * - 审核时：由前端进行严格验证
     */
    @Data
    @ApiModel("合同危废条目明细请求")
    public static class ContractWasteItemRequest {
        @ApiModelProperty(value = "合同危废明细编号（更新时使用）")
        private Integer contractWasteItemId;

        @ApiModelProperty(value = "报价危废明细编号（来源报价危废明细，用于追溯）")
        private Integer quotationWasteItemId;

        @ApiModelProperty(value = "危废条目编号（可为空）")
        private Integer hazardousWasteItemId;

        @ApiModelProperty(value = "废物类别（HW/SW 类别名称，如：HW01 医疗废物）")
        private String wasteCategory;

        @ApiModelProperty(value = "行业来源（卫生、炼铁等）")
        private String industrySource;

        @ApiModelProperty(value = "废物代码（官方公布的废物代码，如：841-001-01）")
        private String wasteCode;

        @ApiModelProperty(value = "危险废物（危废或固废名称）")
        private String hazardousWaste;

        @ApiModelProperty(value = "形态（固态/液态/气态/半固态等）")
        private String form;

        @ApiModelProperty(value = "计量单位（吨/桶/个等）")
        private String unit;

        @ApiModelProperty(value = "计划数量（计划转移数量，可为空）")
        private java.math.BigDecimal plannedQuantity;

        @ApiModelProperty(value = "单价（合同约定单价，元/计量单位）")
        private java.math.BigDecimal unitPrice;

        @ApiModelProperty(value = "金额（合同约定金额）")
        private java.math.BigDecimal amount;

        // ====== 基础/辅助计量单位与换算关系（以表结构为准） ======

        @ApiModelProperty(value = "是否启用辅助核算")
        @com.fasterxml.jackson.annotation.JsonProperty("enableAuxiliaryAccounting")
        private Boolean enableAuxiliaryAccounting;

        @ApiModelProperty(value = "基础计量单位（如吨），用于统一按基础单位统计与换算")
        @com.fasterxml.jackson.annotation.JsonProperty("baseUnit")
        private String baseUnit;

        @ApiModelProperty(value = "基础计量数量（按基础计量单位换算后的数量，如吨）")
        @com.fasterxml.jackson.annotation.JsonProperty("baseQuantity")
        private java.math.BigDecimal baseQuantity;

        @ApiModelProperty(value = "辅助计量单位（业务友好展示单位，如桶/袋/车等）")
        @com.fasterxml.jackson.annotation.JsonProperty("auxUnit")
        private String auxUnit;

        @ApiModelProperty(value = "辅助单位每基础单位数量（1计划转移数量≈多少辅助单位，例如1吨≈10桶）")
        @com.fasterxml.jackson.annotation.JsonProperty("auxPerBase")
        private java.math.BigDecimal auxPerBase;

        @ApiModelProperty(value = "按辅助计量单位表达的数量，通常对应页面上的桶/袋等数量")
        @com.fasterxml.jackson.annotation.JsonProperty("auxQuantity")
        private java.math.BigDecimal auxQuantity;

        @ApiModelProperty(value = "辅助计价单价（元/辅助计量单位，如元/桶）")
        @com.fasterxml.jackson.annotation.JsonProperty("auxUnitPrice")
        private java.math.BigDecimal auxUnitPrice;

        @ApiModelProperty(value = "计价方案（按量结算时使用）")
        private String pricingPlan;

        @ApiModelProperty(value = "付款方（按量结算时使用，甲方/乙方）")
        private String payer;

        @ApiModelProperty(value = "低价备注（低价说明等备注信息）")
        private String floorPriceRemark;

        @ApiModelProperty(value = "备注（按量结算时使用）")
        private String remark;
    }
}



