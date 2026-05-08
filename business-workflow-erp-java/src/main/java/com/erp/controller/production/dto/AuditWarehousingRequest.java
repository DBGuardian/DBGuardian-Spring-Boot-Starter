package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 审核入库单请求参数
 */
@Data
@ApiModel(description = "审核入库单请求参数")
public class AuditWarehousingRequest {

    /**
     * 入库单号
     */
    @NotBlank(message = "入库单号不能为空")
    @ApiModelProperty(value = "入库单号", required = true, example = "RKD-20240101-0001")
    private String warehousingNo;

    /**
     * 审核结果（通过/拒绝）
     */
    @NotBlank(message = "审核结果不能为空")
    @ApiModelProperty(value = "审核结果", required = true, example = "通过", allowableValues = "通过,拒绝")
    private String auditResult;

    /**
     * 审核意见（可选，拒绝时建议填写）
     */
    @ApiModelProperty(value = "审核意见", example = "审核通过，符合入库要求")
    private String auditOpinion;
}

