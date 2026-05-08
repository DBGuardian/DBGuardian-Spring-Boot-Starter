package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 业务费结算单审核请求
 *
 * 功能描述：审核业务费结算单，通过或驳回审核
 *
 * @author ERP System
 * @date 2026-04-28
 */
@Data
@ApiModel("业务费结算单审核请求")
public class BusinessFeeAuditRequest {

    /**
     * 审核结果：approved=通过 / rejected=驳回
     */
    @NotBlank(message = "审核结果不能为空")
    @ApiModelProperty(value = "审核结果：approved=通过 / rejected=驳回", required = true)
    private String auditResult;

    /**
     * 审核意见
     */
    @ApiModelProperty(value = "审核意见")
    private String auditOpinion;

    /**
     * 是否跳过权限检查（OA回调时使用）
     */
    @ApiModelProperty(value = "是否跳过权限检查，OA回调时设置为true", notes = "OA回调时跳过操作范围权限校验")
    private Boolean skipPermissionCheck = false;
}
