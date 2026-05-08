package com.erp.controller.oa.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 重新提交审核响应DTO
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
@ApiModel(description = "重新提交审核响应")
public class ResubmitApprovalResponse {

    @ApiModelProperty(value = "新审核记录ID")
    private Integer newApprovalRecordId;

    @ApiModelProperty(value = "新审核编号")
    private String approvalNo;

    @ApiModelProperty(value = "审核次数")
    private Integer approvalCount;
}
