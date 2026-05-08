package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 审核阶段补充合同号请求
 */
@Data
@ApiModel("审核阶段补充合同号请求")
public class TransportApplyAuditBindContractRequest {

    @ApiModelProperty(value = "收运通知单号", required = true)
    @NotBlank(message = "收运通知单号不能为空")
    private String noticeCode;

    @ApiModelProperty(value = "合同号", required = true)
    @NotBlank(message = "合同号不能为空")
    private String contractCode;
}

