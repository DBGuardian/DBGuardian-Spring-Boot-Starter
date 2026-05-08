package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 审核出库单请求参数
 */
@Data
@ApiModel(description = "审核出库单请求参数")
public class AuditOutboundRequest {

    /**
     * 出库单号
     */
    @NotBlank(message = "出库单号不能为空")
    @ApiModelProperty(value = "出库单号", required = true, example = "CKD-20240101-0001")
    private String outboundNo;

    /**
     * 审核结果（通过/拒绝）
     */
    @NotBlank(message = "审核结果不能为空")
    @ApiModelProperty(value = "审核结果", required = true, example = "通过", allowableValues = "通过,拒绝")
    private String auditResult;

    /**
     * 审核意见
     */
    @NotBlank(message = "审核意见不能为空")
    @ApiModelProperty(value = "审核意见", required = true, example = "审核通过，库存扣减校验通过")
    private String auditOpinion;
}
