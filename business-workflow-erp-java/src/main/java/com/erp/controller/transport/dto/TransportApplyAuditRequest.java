package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 收运通知单审核请求
 */
@Data
@ApiModel("收运通知单审核请求")
public class TransportApplyAuditRequest {

    @ApiModelProperty(value = "收运通知单号", required = true)
    @NotBlank(message = "收运通知单号不能为空")
    private String noticeCode;

    @ApiModelProperty(value = "审核结果：待调度/审核失败", required = true, 
            notes = "审核中状态可审核为待调度（通过）或审核失败（拒绝）。拒绝时审核意见必填")
    @NotBlank(message = "审核结果不能为空")
    private String auditResult;

    @ApiModelProperty(value = "审核意见", notes = "拒绝时必填，需详细说明拒绝原因")
    private String auditOpinion;
}

