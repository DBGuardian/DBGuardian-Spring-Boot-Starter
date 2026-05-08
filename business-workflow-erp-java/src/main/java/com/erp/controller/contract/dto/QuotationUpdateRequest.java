package com.erp.controller.contract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 更新报价单请求
 */
@Data
@ApiModel("更新报价单请求")
public class QuotationUpdateRequest {

    @ApiModelProperty(value = "报价单编号", required = true)
    @NotNull(message = "报价单编号不能为空")
    private Integer quotationId;

    @ApiModelProperty(value = "客户快照（如需同步调整报价单抬头/联系人信息时可传入，结构与合同模块一致）")
    private ContractCustomerSnapshot customerSnapshot;

    @ApiModelProperty(value = "报价单号")
    private String quotationNo;

    @ApiModelProperty(value = "报价日期（支持字符串格式：YYYY-MM-DD）")
    @JsonProperty("quotationDate")
    private Object quotationDate;

    @ApiModelProperty(value = "甲方名称（默认使用客户企业名称，可覆盖）")
    @JsonProperty("partyAName")
    private String partyAName;

    @ApiModelProperty(value = "有效期开始（支持字符串格式：YYYY-MM-DD HH:mm:ss）")
    @JsonProperty("validFrom")
    private Object validFrom;

    @ApiModelProperty(value = "有效期结束（支持字符串格式：YYYY-MM-DD HH:mm:ss）")
    @JsonProperty("validTo")
    private Object validTo;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "甲方联系人")
    @JsonProperty("partyAContact")
    private String partyAContact;

    @ApiModelProperty(value = "甲方联系电话")
    @JsonProperty("partyAContactPhone")
    private String partyAContactPhone;

    @ApiModelProperty(value = "甲方统一社会信用代码")
    @JsonProperty("partyACreditCode")
    private String partyACreditCode;

    @ApiModelProperty(value = "乙方名称")
    @JsonProperty("partyBName")
    private String partyBName;

    @ApiModelProperty(value = "乙方联系人")
    @JsonProperty("partyBContact")
    private String partyBContact;

    @ApiModelProperty(value = "乙方联系电话")
    @JsonProperty("partyBContactPhone")
    private String partyBContactPhone;

    @ApiModelProperty(value = "乙方统一社会信用代码")
    @JsonProperty("partyBCreditCode")
    private String partyBCreditCode;

    @ApiModelProperty(value = "报价条目列表", required = true)
    @NotEmpty(message = "至少需要一条报价条目")
    @Valid
    private List<QuotationItemRequest> items;

    @ApiModelProperty(value = "价外服务列表（编辑时传入，用于差分同步）")
    private List<com.erp.controller.contract.dto.OutOfScopeServiceCreateDTO> outOfScopeServices;

    /**
     * 报价条目请求
     */
    @Data
    @ApiModel("报价条目请求")
    public static class QuotationItemRequest {

        @ApiModelProperty(value = "报价条目编号（更新时使用）")
        private Integer quotationItemId;

        @ApiModelProperty(value = "报价模式：总价包干/按量结算", required = true)
        @NotNull(message = "报价模式不能为空")
        private String quotationMode;

        @ApiModelProperty(value = "付款方：甲方/乙方/共同", required = true)
        @NotNull(message = "付款方不能为空")
        private String payer;

        @ApiModelProperty(value = "计价方案（总价包干时使用）")
        private String pricingPlan;

        @ApiModelProperty(value = "备注（总价包干时使用）")
        private String remark;

        @ApiModelProperty(value = "小计摘要（记录各计量单位小计，JSON数组形式 [{unit,total}]，由前端计算并传递）")
        private String subtotalSummary;

        @ApiModelProperty(value = "危废条目明细列表（按量结算时必填）")
        @Valid
        private List<QuotationWasteItemRequest> wasteItems;
    }

    /**
     * 报价危废条目明细请求
     */
    @Data
    @ApiModel("报价危废条目明细请求")
    public static class QuotationWasteItemRequest {

        @ApiModelProperty(value = "报价危废明细编号（更新时使用）")
        private Integer quotationWasteItemId;

        @ApiModelProperty(value = "危废条目编号")
        private Integer hazardousWasteItemId;

        @ApiModelProperty(value = "废物类别（HW/SW 类别名称，如：HW01 医疗废物）", required = true)
        @NotNull(message = "废物类别不能为空")
        private String wasteCategory;

        @ApiModelProperty(value = "行业来源（卫生、炼铁等）")
        private String industrySource;

        @ApiModelProperty(value = "废物代码（官方公布的废物代码，如：841-001-01）", required = true)
        @NotNull(message = "废物代码不能为空")
        private String wasteCode;

        @ApiModelProperty(value = "危险废物（危废或固废名称）", required = true)
        @NotNull(message = "危险废物不能为空")
        private String hazardousWaste;

        @ApiModelProperty(value = "形态（固态/液态/气态/半固态等）")
        private String form;

        @ApiModelProperty(value = "计量单位（吨/桶/个等）", required = true)
        @NotNull(message = "计量单位不能为空")
        private String unit;

        @ApiModelProperty(value = "计划数量（计划转移数量，-1表示不限量）", required = true)
        @NotNull(message = "计划数量不能为空")
        private java.math.BigDecimal plannedQuantity;

        @ApiModelProperty(value = "单价（元/计量单位）")
        private java.math.BigDecimal unitPrice;

        @ApiModelProperty(value = "金额")
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

        @ApiModelProperty(value = "基础计价单价（元/基础计量单位，如元/吨）")
        @com.fasterxml.jackson.annotation.JsonProperty("baseUnitPrice")
        private java.math.BigDecimal baseUnitPrice;

        @ApiModelProperty(value = "辅助计价单价（元/辅助计量单位，如元/桶）")
        @com.fasterxml.jackson.annotation.JsonProperty("auxUnitPrice")
        private java.math.BigDecimal auxUnitPrice;

        @ApiModelProperty(value = "计价方案（按量结算时使用）")
        private String pricingPlan;

        @ApiModelProperty(value = "付款方（按量结算时使用，甲方/乙方）")
        private String payer;

        @ApiModelProperty(value = "低价备注（低价说明等备注信息）")
        @com.fasterxml.jackson.annotation.JsonProperty("floorPriceRemark")
        private String floorPriceRemark;

        @ApiModelProperty(value = "备注（按量结算时使用）")
        private String remark;
    }
}









