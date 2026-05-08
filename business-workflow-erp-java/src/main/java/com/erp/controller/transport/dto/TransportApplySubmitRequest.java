package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 收运通知单提交审核请求
 */
@Data
@ApiModel("收运通知单提交审核请求")
public class TransportApplySubmitRequest {

    @ApiModelProperty(value = "收运通知单号", required = true)
    @NotBlank(message = "收运通知单号不能为空")
    private String noticeCode;
}

