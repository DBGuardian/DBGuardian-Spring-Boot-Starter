package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 收运通知单创建/更新请求
 */
@Data
@ApiModel("收运通知单创建/更新请求")
public class TransportApplyDetailRequest {

    @ApiModelProperty("收运通知单编号（更新时必填）")
    private Integer noticeId;

    @ApiModelProperty("收运通知单号（更新时必填）")
    private String noticeCode;

    @ApiModelProperty("合同号")
    private String contractCode;

    @ApiModelProperty("合同待补标记")
    private Boolean contractPending;

    @ApiModelProperty("客户编码")
    private Integer customerId;

    @ApiModelProperty("单位名称（产生单位名称）")
    private String companyName;

    @ApiModelProperty("统一社会信用代码")
    private String creditCode;

    @ApiModelProperty("运输地址")
    private String transportAddress;

    @ApiModelProperty("现场联系人")
    private String onsiteContact;

    @ApiModelProperty("现场联系电话（选填）")
    private String onsitePhone;

    @ApiModelProperty("业务联系人")
    private String businessContact;

    @ApiModelProperty("业务联系电话（选填）")
    private String businessPhone;

    @ApiModelProperty("应急联系电话（选填）")
    private String emergencyPhone;

    @ApiModelProperty("计划收运日期（格式：YYYY-MM-DD）")
    private String planTransferDate;

    @ApiModelProperty("提交申请日期（格式：YYYY-MM-DD）")
    private String submitDate;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("危废明细列表")
    @Valid
    private List<TransportApplyItemRequest> items;

    @ApiModelProperty("附件列表")
    private List<TransportAttachmentRequest> attachments;
}

