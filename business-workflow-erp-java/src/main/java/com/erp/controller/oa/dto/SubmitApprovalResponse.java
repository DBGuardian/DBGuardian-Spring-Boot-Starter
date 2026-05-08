package com.erp.controller.oa.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 提交审核响应DTO
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
@ApiModel(description = "提交审核响应")
public class SubmitApprovalResponse {

    @ApiModelProperty(value = "审核记录ID")
    private Integer approvalRecordId;

    @ApiModelProperty(value = "审核编号")
    private String approvalNo;
}
