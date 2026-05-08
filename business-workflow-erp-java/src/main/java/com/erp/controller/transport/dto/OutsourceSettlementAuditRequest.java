package com.erp.controller.transport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

/**
 * 委外运输结算单审核请求
 */
@Data
@Schema(description = "委外运输结算单审核请求")
public class OutsourceSettlementAuditRequest {

    @Schema(description = "审核结果（approved/rejected）")
    @NotBlank(message = "审核结果不能为空")
    private String auditResult;

    @Schema(description = "审核意见")
    @NotBlank(message = "审核意见不能为空")
    private String auditOpinion;
}
