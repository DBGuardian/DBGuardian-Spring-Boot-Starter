package com.erp.controller.contract.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 新增报价单请求
 */
@Data
@ApiModel("新增报价单请求")
public class QuotationCreateRequest {

    @ApiModelProperty(value = "客户编码（存在正式客户时传入；可为空，配合 customerSnapshot 实现临时客户抬头）")
    private Integer customerId;

    @ApiModelProperty(value = "客户快照（customerId 为空或需要自定义抬头时传入，结构与合同模块一致）")
    private ContractCustomerSnapshot customerSnapshot;

    @ApiModelProperty(value = "内部编号（已废弃，建议使用quotationNo字段。如果提供，会优先使用，需验证唯一性）")
    @JsonProperty("internalCode")
    private String internalCode;

    @ApiModelProperty(value = "报价单号（可选，如果未提供，系统自动生成格式：QT-YYYYMMDD-XXXXX）")
    private String quotationNo;

    @ApiModelProperty(value = "报价日期（支持字符串格式：YYYY-MM-DD）")
    @JsonProperty("quotationDate")
    private String quotationDate;

    @ApiModelProperty(value = "甲方名称（默认使用客户企业名称，可覆盖）")
    @JsonProperty("partyAName")
    private String partyAName;

    @ApiModelProperty(value = "有效期开始（支持字符串格式：YYYY-MM-DD HH:mm:ss）")
    @JsonProperty("validFrom")
    private Object validFrom;

    @ApiModelProperty(value = "有效期结束（支持字符串格式：YYYY-MM-DD HH:mm:ss）")
    @JsonProperty("validTo")
    private Object validTo;

    @ApiModelProperty(value = "甲方联系人")
    @JsonProperty("partyAContact")
    private String partyAContact;

    @ApiModelProperty(value = "甲方联系电话（选填）")
    @JsonProperty("partyAContactPhone")
    private String partyAContactPhone;

    @ApiModelProperty(value = "甲方统一社会信用代码")
    @JsonProperty("partyACreditCode")
    private String partyACreditCode;

    @ApiModelProperty(value = "乙方名称", required = true)
    @NotBlank(message = "乙方名称不能为空")
    @JsonProperty("partyBName")
    private String partyBName;

    @ApiModelProperty(value = "乙方联系人", required = true)
    @NotBlank(message = "乙方联系人不能为空")
    @JsonProperty("partyBContact")
    private String partyBContact;

    @ApiModelProperty(value = "乙方联系电话（选填）")
    @JsonProperty("partyBContactPhone")
    private String partyBContactPhone;

    @ApiModelProperty(value = "乙方统一社会信用代码")
    @JsonProperty("partyBCreditCode")
    private String partyBCreditCode;

    @ApiModelProperty(value = "服务条款（前端字段，暂不存储）")
    @JsonProperty("deliveryPlan")
    private String deliveryPlan;

    @ApiModelProperty(value = "付款约定（前端字段，暂不存储）")
    @JsonProperty("paymentTerm")
    private String paymentTerm;

    @ApiModelProperty(value = "补充条款（前端字段，暂不存储）")
    @JsonProperty("extraTerms")
    private String extraTerms;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "报价条目列表", required = true)
    @NotEmpty(message = "至少需要一条报价条目")
    @Valid
    private List<QuotationItemRequest> items;

    @ApiModelProperty(value = "价外服务列表（可选），在创建时一并保存")
    private List<com.erp.controller.contract.dto.OutOfScopeServiceCreateDTO> outOfScopeServices;

    /**
     * 报价条目请求
     */
    @Data
    @ApiModel("报价条目请求")
    public static class QuotationItemRequest {

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
        @JsonProperty("subtotalSummary")
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

        @ApiModelProperty(value = "是否启用辅助核算")
        @JsonProperty("enableAuxiliaryAccounting")
        private Boolean enableAuxiliaryAccounting;

        @ApiModelProperty(value = "基础计量单位（如吨），用于统一按基础单位统计与换算；可选字段，未传时可由计量单位推导")
        @JsonProperty("baseUnit")
        private String baseUnit;

        @ApiModelProperty(value = "基础计量数量（按基础计量单位换算后的数量，如吨）；可选字段，未传时可由计划数量和换算关系推导")
        @JsonProperty("baseQuantity")
        private java.math.BigDecimal baseQuantity;

        @ApiModelProperty(value = "辅助计量单位（业务友好展示单位，如桶/袋/车等）")
        @JsonProperty("auxUnit")
        private String auxUnit;

        @ApiModelProperty(value = "辅助单位每基础单位数量（1计划转移数量≈多少辅助单位，例如1吨≈10桶）")
        @JsonProperty("auxPerBase")
        private java.math.BigDecimal auxPerBase;

        @ApiModelProperty(value = "辅助数量（按辅助计量单位表达的数量，通常等同于以桶/袋等为单位录入的计划数量）")
        @JsonProperty("auxQuantity")
        private java.math.BigDecimal auxQuantity;

        @ApiModelProperty(value = "基础计价单价（元/基础计量单位，通常为元/吨；可通过辅助单价与换算关系折算得到）")
        @JsonProperty("baseUnitPrice")
        private java.math.BigDecimal baseUnitPrice;

        @ApiModelProperty(value = "辅助计价单价（元/辅助计量单位，如元/桶；可选字段，便于在报价阶段使用业务友好单位表达价格）")
        @JsonProperty("auxUnitPrice")
        private java.math.BigDecimal auxUnitPrice;

        @ApiModelProperty(value = "计价方案（按量结算时使用）")
        private String pricingPlan;

        @ApiModelProperty(value = "付款方（按量结算时使用，甲方/乙方）")
        private String payer;

        @ApiModelProperty(value = "低价备注（低价说明等备注信息）")
        @JsonProperty("floorPriceRemark")
        private String floorPriceRemark;

        @ApiModelProperty(value = "备注（按量结算时使用）")
        private String remark;
    }
}
