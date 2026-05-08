package com.erp.service.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * OA审批提交结果DTO
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
@ApiModel(description = "OA审批提交结果")
public class OaApprovalSubmitResult {

    @ApiModelProperty(value = "OA审批记录ID")
    private Integer approvalRecordId;

    @ApiModelProperty(value = "OA审批编号")
    private String approvalNo;
}
