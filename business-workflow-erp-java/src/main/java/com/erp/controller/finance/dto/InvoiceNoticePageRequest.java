package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Min;
import java.time.LocalDateTime;

/**
 * 开票通知单分页查询请求
 */
@Data
@ApiModel("开票通知单分页查询请求")
public class InvoiceNoticePageRequest {

    /**
     * 当前页码
     */
    @ApiModelProperty(value = "当前页码，从1开始", example = "1")
    @Min(value = 1, message = "当前页码必须大于等于1")
    private long current = 1;

    /**
     * 每页数量
     */
    @ApiModelProperty(value = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量必须大于等于1")
    private long size = 10;

    /**
     * 开票通知单号（模糊匹配）
     */
    @ApiModelProperty(value = "开票通知单号（模糊匹配）")
    private String noticeNo;

    /**
     * 合同号（模糊匹配）
     */
    @ApiModelProperty(value = "合同号（模糊匹配）")
    private String contractNo;

    /**
     * 合同名称（模糊匹配）
     */
    @ApiModelProperty(value = "合同名称（模糊匹配）")
    private String contractName;

    /**
     * 客户名称（模糊匹配）
     */
    @ApiModelProperty(value = "客户名称（模糊匹配）")
    private String customerName;

    /**
     * 主结算单编号（模糊匹配）
     */
    @ApiModelProperty(value = "主结算单编号（模糊匹配）")
    private String mainSettlementNo;

    /**
     * 开票类型（精确匹配）
     */
    @ApiModelProperty(value = "开票类型：开票/作废")
    private String invoiceType;

    /**
     * 状态（精确匹配）
     */
    @ApiModelProperty(value = "状态：待审核/审核中/已驳回/待开票/已开票/已归档/已取消")
    private String status;

    /**
     * 申请人编码
     */
    @ApiModelProperty(value = "申请人编码")
    private Integer applicantId;

    /**
     * 申请人姓名（模糊匹配）
     */
    @ApiModelProperty(value = "申请人姓名（模糊匹配）")
    private String applicantName;

    /**
     * 办理人编码
     */
    @ApiModelProperty(value = "办理人编码")
    private Integer handlerId;

    /**
     * 创建时间开始（格式：yyyy-MM-dd HH:mm:ss）
     */
    @ApiModelProperty(value = "创建时间开始（格式：yyyy-MM-dd HH:mm:ss）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 创建时间结束（格式：yyyy-MM-dd HH:mm:ss）
     */
    @ApiModelProperty(value = "创建时间结束（格式：yyyy-MM-dd HH:mm:ss）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 开票完成时间开始（格式：yyyy-MM-dd HH:mm:ss）
     */
    @ApiModelProperty(value = "开票完成时间开始（格式：yyyy-MM-dd HH:mm:ss）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedAtStart;

    /**
     * 开票完成时间结束（格式：yyyy-MM-dd HH:mm:ss）
     */
    @ApiModelProperty(value = "开票完成时间结束（格式：yyyy-MM-dd HH:mm:ss）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedAtEnd;

    /**
     * 排序字段：noticeNo/contractNo/contractName/customerName/status/mainSettlementNo/invoiceType/applicantName/approverName/handlerName/createTime/issuedAt/invoiceCount/totalAmount
     */
    @ApiModelProperty(value = "排序字段：noticeNo/contractNo/contractName/customerName/status/mainSettlementNo/invoiceType/applicantName/approverName/handlerName/createTime/issuedAt/invoiceCount/totalAmount")
    private String sortField;

    /**
     * 排序方向：asc/desc
     */
    @ApiModelProperty(value = "排序方向：asc/desc")
    private String sortOrder;

    /**
     * 数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断
     */
    @ApiModelProperty(value = "数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断")
    private String viewScope;
}

