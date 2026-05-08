package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报价单详情响应
 */
@Data
@ApiModel("报价单详情响应")
public class QuotationDetailResponse {

    @ApiModelProperty(value = "报价单编号")
    private Integer quotationId;

    @ApiModelProperty(value = "报价单号")
    private String quotationNo;

    @ApiModelProperty(value = "客户编码")
    private Integer customerId;

    @ApiModelProperty(value = "客户名称")
    private String customerName;

    @ApiModelProperty(value = "客户快照（来自 QUOTATION.customer_snapshot，用于展示历史甲方抬头与联系方式）")
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

    @ApiModelProperty(value = "报价状态：待审批/已通过/已驳回/已失效")
    private String quotationStatus;

    @ApiModelProperty(value = "报价日期")
    private LocalDate quotationDate;

    @ApiModelProperty(value = "有效期开始")
    private LocalDateTime validFrom;

    @ApiModelProperty(value = "有效期结束")
    private LocalDateTime validTo;

    @ApiModelProperty(value = "总数量")
    private BigDecimal totalQuantity;

    @ApiModelProperty(value = "PDF文件编号")
    private Integer pdfFileId;

    @ApiModelProperty(value = "PDF文件URL")
    private String pdfFileUrl;

    @ApiModelProperty(value = "PDF文件名称")
    private String pdfFileName;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建人编码")
    private Integer creatorId;

    @ApiModelProperty(value = "创建人姓名")
    private String creatorName;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "审核人编码")
    private Integer auditorId;

    @ApiModelProperty(value = "审核人名称")
    private String auditorName;

    @ApiModelProperty(value = "审核意见")
    private String auditOpinion;

    @ApiModelProperty(value = "审核时间")
    private LocalDateTime auditTime;

    @ApiModelProperty(value = "报价条目列表")
    private List<QuotationItemResponse> items;
    
    @ApiModelProperty(value = "价外服务列表（如有）")
    private List<OutOfScopeServiceResponse> outOfScopeServices;

    @Data
    @ApiModel("价外服务响应")
    public static class OutOfScopeServiceResponse {
        @ApiModelProperty("价外服务编号")
        private Integer outOfScopeServiceId;
        @ApiModelProperty("项目")
        private String project;
        @ApiModelProperty("规格型号")
        private String spec;
        @ApiModelProperty("单位")
        private String unit;
        @ApiModelProperty("计划数量")
        private java.math.BigDecimal plannedQuantity;
        @ApiModelProperty("合同单价")
        private java.math.BigDecimal contractUnitPrice;
        @ApiModelProperty("状态")
        private String status;
    }

    /**
     * 报价条目响应
     */
    @Data
    @ApiModel("报价条目响应")
    public static class QuotationItemResponse {

        @ApiModelProperty(value = "报价条目编号")
        private Integer quotationItemId;

        @ApiModelProperty(value = "报价模式：总价包干/按量结算")
        private String quotationMode;

        @ApiModelProperty(value = "付款方：甲方/乙方")
        private String payer;

        @ApiModelProperty(value = "计价方案（总价包干时使用）")
        private String pricingPlan;

        @ApiModelProperty(value = "备注（总价包干时使用）")
        private String remark;

        @ApiModelProperty(value = "小计摘要（记录各计量单位小计，JSON数组形式 [{unit,total}]）")
        private String subtotalSummary;
        
        @ApiModelProperty(value = "创建时间")
        private LocalDateTime createTime;

        @ApiModelProperty(value = "更新时间")
        private LocalDateTime updateTime;

        @ApiModelProperty(value = "危废条目明细列表")
        private List<QuotationWasteItemResponse> wasteItems;
    }

    /**
     * 报价危废条目明细响应
     */
    @Data
    @ApiModel("报价危废条目明细响应")
    public static class QuotationWasteItemResponse {

        @ApiModelProperty(value = "报价危废明细编号")
        private Integer quotationWasteItemId;

        @ApiModelProperty(value = "危废条目编号")
        private Integer hazardousWasteItemId;

        @ApiModelProperty(value = "废物类别")
        private String wasteCategory;

        @ApiModelProperty(value = "行业来源")
        private String industrySource;

        @ApiModelProperty(value = "废物代码")
        private String wasteCode;

        @ApiModelProperty(value = "危险废物")
        private String hazardousWaste;

        @ApiModelProperty(value = "形态")
        private String form;

        @ApiModelProperty(value = "计量单位")
        private String unit;

        @ApiModelProperty(value = "计划数量（-1表示不限量）")
        private BigDecimal plannedQuantity;

        @ApiModelProperty(value = "单价（元/计量单位）")
        private BigDecimal unitPrice;

        @ApiModelProperty(value = "金额")
        private BigDecimal amount;

        @ApiModelProperty(value = "是否启用辅助核算")
        private Boolean enableAuxiliaryAccounting;

        @ApiModelProperty(value = "计价方案（按量结算时使用）")
        private String pricingPlan;

        @ApiModelProperty(value = "付款方（按量结算时使用，甲方/乙方）")
        private String payer;

        @ApiModelProperty(value = "低价备注（低价说明等备注信息）")
        private String floorPriceRemark;

        @ApiModelProperty(value = "备注（按量结算时使用）")
        private String remark;

        // ====== 基础/辅助计量单位与换算关系（以表结构为准） ======

        @ApiModelProperty(value = "基础计量单位（如吨，用于统一按基础单位统计与展示）")
        private String baseUnit;

        @ApiModelProperty(value = "基础计量数量（按基础计量单位换算后的数量，如吨）")
        private BigDecimal baseQuantity;

        @ApiModelProperty(value = "基础计价单价（元/基础计量单位，如元/吨）")
        private BigDecimal baseUnitPrice;

        @ApiModelProperty(value = "辅助计量单位（业务友好展示单位，如桶/袋/车等）")
        private String auxUnit;

        @ApiModelProperty(
                value = "辅助单位每基础单位数量（1计划转移数量≈多少辅助单位，例如1吨≈10桶）"
        )
        private BigDecimal auxPerBase;

        @ApiModelProperty(
                value = "辅助数量（按辅助计量单位表达的数量，通常对应页面上的桶/袋等数量）"
        )
        private BigDecimal auxQuantity;

        @ApiModelProperty(
                value = "辅助计价单价（元/辅助计量单位，如元/桶；在报价阶段用于更贴近业务场景的价格表达）"
        )
        private BigDecimal auxUnitPrice;
        
        @ApiModelProperty(value = "创建时间")
        private LocalDateTime createTime;

        @ApiModelProperty(value = "更新时间")
        private LocalDateTime updateTime;
    }
}









