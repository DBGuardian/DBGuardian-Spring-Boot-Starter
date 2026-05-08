package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 审批开票通知单请求DTO
 *
 * @author ERP System
 * @date 2026-01-06
 */
@Data
@ApiModel("审批开票通知单请求")
public class InvoiceNoticeApproveRequest {

    /**
     * 审批意见
     */
    @NotBlank(message = "审批意见不能为空")
    @Size(max = 1000, message = "审批意见长度不能超过1000个字符")
    @ApiModelProperty(value = "审批意见", required = true)
    private String opinion;
}

