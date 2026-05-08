package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 收运通知单分页响应
 * 
 * 注意：该DTO同时服务于"收运通知"和"车辆安排"两个页面，字段权限通过valueAliases支持多页面复用
 */
@Data
@ApiModel("收运通知单分页响应")
public class TransportApplyPageResponse {

    @ApiModelProperty("收运通知单编号")
    private Integer noticeId;

    @ApiModelProperty("收运通知单号")
    private String noticeCode;

    @ApiModelProperty("合同号")
    private String contractCode;

    @ApiModelProperty("合同待补标记")
    private Boolean contractPending;

    @ApiModelProperty("客户编码")
    private Integer customerId;

    @ApiModelProperty("单位名称")
    private String companyName;

    @ApiModelProperty("统一社会信用代码")
    private String creditCode;

    @ApiModelProperty("运输地址")
    private String transportAddress;

    @ApiModelProperty("现场联系人")
    private String onsiteContact;

    @ApiModelProperty("现场联系电话")
    private String onsitePhone;

    @ApiModelProperty("应急联系电话")
    private String emergencyPhone;

    @ApiModelProperty("计划收运日期")
    private String planTransferDate;

    @ApiModelProperty("转移交付时间（兼容前端字段）")
    private String transferTime;

    @ApiModelProperty("合同补齐时间")
    private String contractFixTime;

    @ApiModelProperty("危废详情文件")
    private String wasteDetailFile;

    @ApiModelProperty("产废单位二维码")
    private String qrCode;

    @ApiModelProperty("审核人编码")
    private Integer auditorId;

    @ApiModelProperty("创建人编码")
    private Integer creatorId;

    @ApiModelProperty("锁定时间")
    private String lockTime;

    @ApiModelProperty("锁定人编码")
    private Integer lockUserId;

    @ApiModelProperty("更新时间")
    private String updateTime;

    @ApiModelProperty("状态：未提交/审核中/审核失败/待调度/已派单/已完成/已取消")
    private String status;

    @ApiModelProperty("提交时间")
    private String submittedAt;

    @ApiModelProperty("审核时间")
    private String auditedAt;

    @ApiModelProperty("审核人姓名")
    private String auditorName;

    @ApiModelProperty("审核意见，记录审核通过或驳回的原因/说明")
    private String auditOpinion;

    @ApiModelProperty("创建人姓名")
    private String creatorName;

    @ApiModelProperty("创建时间")
    private String createdAt;

    @ApiModelProperty("是否锁定")
    private Boolean locked;

    @ApiModelProperty("锁定原因")
    private String lockReason;

    @ApiModelProperty("备注")
    private String remark;
}

