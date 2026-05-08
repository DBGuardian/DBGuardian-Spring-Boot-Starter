package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 开票通知单详情响应
 */
@Data
@ApiModel("开票通知单详情响应")
public class InvoiceNoticeDetailResponse {

    @ApiModelProperty("开票通知单编号")
    private Integer noticeId;

    @ApiModelProperty("开票通知单号")
    private String noticeNo;

    @ApiModelProperty("合同编号")
    private Integer contractId;

    @ApiModelProperty("合同号")
    private String contractNo;

    @ApiModelProperty("合同名称")
    private String contractName;

    @ApiModelProperty("客户编码")
    private Integer customerId;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty("结算单编号")
    private Integer mainSettlementId;

    @ApiModelProperty("结算单编码")
    private String mainSettlementCode;

    @ApiModelProperty("已绑定结算摘要")
    private String boundSettlementSummary;

    @ApiModelProperty("开票类型")
    private String invoiceType;

    // 结算单常规字段
    @ApiModelProperty("结算单单号")
    private String settlementCode;

    @ApiModelProperty("结算类型（RECEIVABLE=收款 / PAYABLE=付款）")
    private String settlementType;

    @ApiModelProperty("结算周期起")
    private LocalDate settlementPeriodStart;

    @ApiModelProperty("结算周期止")
    private LocalDate settlementPeriodEnd;

    @ApiModelProperty("结算金额")
    private BigDecimal settlementAmount;

    @ApiModelProperty("已收金额")
    private BigDecimal receivedAmount;

    @ApiModelProperty("结算单状态")
    private String settlementStatus;

    @ApiModelProperty("结算单创建时间")
    private LocalDateTime settlementCreateTime;

    @ApiModelProperty("结算单制单人名称")
    private String settlementCreatorName;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("状态：草稿/待审批/已驳回/待开票/已开票/已归档/已取消")
    private String status;

    @ApiModelProperty("申请人编码")
    private Integer applicantId;

    @ApiModelProperty("申请人姓名")
    private String applicantName;

    @ApiModelProperty("审批人编码")
    private Integer approverId;

    @ApiModelProperty("审批人姓名")
    private String approverName;

    @ApiModelProperty("审批意见")
    private String approvalOpinion;

    @ApiModelProperty("办理人编码")
    private Integer handlerId;

    @ApiModelProperty("办理人姓名")
    private String handlerName;

    @ApiModelProperty("已开票张数")
    private Integer invoiceCount;

    @ApiModelProperty("已开票价税合计")
    private BigDecimal totalAmount;

    @ApiModelProperty("开票完成时间")
    private LocalDateTime issuedAt;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("创建人编码")
    private Integer createUserId;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("更新人编码")
    private Integer updateUserId;

    @ApiModelProperty("版本号")
    private Integer version;

    @ApiModelProperty("是否锁定")
    private Boolean locked;

    @ApiModelProperty("锁定原因")
    private String lockReason;

    @ApiModelProperty("创建时间（前端字段名：createdAt）")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间（前端字段名：updatedAt）")
    private LocalDateTime updatedAt;

    // 客户详细信息（从合同关联的客户信息读取）
    @ApiModelProperty("企业名称")
    private String enterpriseName;

    @ApiModelProperty("统一社会信用代码")
    private String creditCode;

    @ApiModelProperty("地址")
    private String address;

    @ApiModelProperty("电话")
    private String phone;

    @ApiModelProperty("法定代表人")
    private String legalRepresentative;

    @ApiModelProperty("联系人")
    private String contactPerson;

    @ApiModelProperty("联系电话")
    private String contactPhone;

    // 关联的发票列表
    @ApiModelProperty("关联的发票列表")
    private java.util.List<InvoiceNoticeInvoiceDTO> invoices;
}

