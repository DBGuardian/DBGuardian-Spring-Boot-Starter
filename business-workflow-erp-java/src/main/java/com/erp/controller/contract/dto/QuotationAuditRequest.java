package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 报价单审核请求
 */
@Data
@ApiModel("报价单审核请求")
public class QuotationAuditRequest {

    @ApiModelProperty(value = "报价单编号", required = true)
    @NotNull(message = "报价单编号不能为空")
    private Integer quotationId;

    @ApiModelProperty(value = "审核结果/状态：草稿/待审批/已通过/已驳回/已失效", required = true,
            notes = "待审批状态可审核为已通过或已驳回，其他状态也可修改（已失效除外）。驳回时审核意见必填")
    @NotBlank(message = "审核结果不能为空")
    private String auditResult;

    @ApiModelProperty(value = "审核意见", notes = "拒绝时必填，需详细说明拒绝原因")
    private String auditOpinion;

    @ApiModelProperty(value = "是否跳过权限检查", notes = "OA回调时设置为true，跳过操作范围权限校验")
    private Boolean skipPermissionCheck = false;
}



