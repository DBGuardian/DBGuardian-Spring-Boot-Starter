package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 驳回开票通知单请求DTO
 *
 * @author ERP System
 * @date 2026-01-06
 */
@Data
@ApiModel("驳回开票通知单请求")
public class InvoiceNoticeRejectRequest {

    /**
     * 驳回原因
     */
    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 1000, message = "驳回原因长度不能超过1000个字符")
    @ApiModelProperty(value = "驳回原因", required = true)
    private String reason;
}

