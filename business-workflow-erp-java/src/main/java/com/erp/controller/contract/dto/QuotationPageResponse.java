package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报价单分页响应
 */
@Data
@ApiModel("报价单分页响应")
public class QuotationPageResponse {

    @ApiModelProperty(value = "报价单编号")
    private Integer quotationId;

    @ApiModelProperty(value = "报价单号（业务编号）")
    private String quotationNo;

    @ApiModelProperty(value = "内部编号（等同于quotationNo）")
    private String internalCode;

    @ApiModelProperty(value = "报价单编号（用于显示，等同于quotationNo）")
    private String quotationCode;

    @ApiModelProperty(value = "客户编码")
    private Integer customerId;

    @ApiModelProperty(value = "客户名称")
    private String customerName;

    @ApiModelProperty(value = "客户快照（来自 QUOTATION.customer_snapshot，用于列表页展示历史抬头）")
    private ContractCustomerSnapshot customerSnapshot;

    @ApiModelProperty(value = "甲方名称")
    private String partyAName;

    @ApiModelProperty(value = "甲方联系人")
    private String partyAContact;

    @ApiModelProperty(value = "甲方联系电话")
    private String partyAContactPhone;

    @ApiModelProperty(value = "乙方名称")
    private String partyBName;
    
    @ApiModelProperty(value = "乙方联系人")
    private String partyBContact;

    @ApiModelProperty(value = "乙方联系电话")
    private String partyBContactPhone;

    @ApiModelProperty(value = "乙方统一社会信用代码")
    private String partyBCreditCode;

    @ApiModelProperty(value = "创建人编码")
    private Integer creatorId;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "报价状态：待审批/已通过/已驳回/已失效")
    private String quotationStatus;

    @ApiModelProperty(value = "报价日期")
    private LocalDate quotationDate;

    @ApiModelProperty(value = "报价模式：总价包干/按量结算/组合计价")
    private String pricingMode;

    @ApiModelProperty(value = "有效期开始")
    private LocalDateTime validFrom;

    @ApiModelProperty(value = "有效期结束")
    private LocalDateTime validTo;

    @ApiModelProperty(value = "PDF文件编号")
    private Integer pdfFileId;

    @ApiModelProperty(value = "是否已生成PDF")
    private Boolean pdfGenerated;

    @ApiModelProperty(value = "PDF文件URL")
    private String pdfUrl;

    @ApiModelProperty(value = "PDF文件名")
    private String pdfFileName;

    @ApiModelProperty(value = "创建人姓名")
    private String creatorName;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "审核人编码")
    private Integer auditorId;

    @ApiModelProperty(value = "审核人名称")
    private String auditorName;

    @ApiModelProperty(value = "审核意见")
    private String auditOpinion;

    @ApiModelProperty(value = "审核时间")
    private LocalDateTime auditTime;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "计价方案高亮标签（JSON数组格式）")
    private String pricingHighlights;

    @ApiModelProperty(value = "计价方案摘要")
    private String pricingSummary;
}
