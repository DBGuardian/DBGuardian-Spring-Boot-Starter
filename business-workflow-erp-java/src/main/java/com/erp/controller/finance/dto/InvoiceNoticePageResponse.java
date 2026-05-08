package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 开票通知单分页响应
 */
@Data
@ApiModel("开票通知单分页响应")
public class InvoiceNoticePageResponse {

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

    @ApiModelProperty("主结算单编号")
    private Integer mainSettlementId;

    @ApiModelProperty("主结算单编码")
    private String mainSettlementCode;

    @ApiModelProperty("已绑定结算摘要")
    private String boundSettlementSummary;

    @ApiModelProperty("开票类型：开票/作废")
    private String invoiceType;

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

    @ApiModelProperty("创建人姓名（操作人）")
    private String creatorName;

    @ApiModelProperty("创建人编码")
    private Integer createUserId;

    @ApiModelProperty("创建人编码（兼容前端字段creatorId）")
    private Integer creatorId;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("更新人编码")
    private Integer updateUserId;

    @ApiModelProperty("版本号")
    private Integer version;
}

