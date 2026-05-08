package com.erp.controller.oa.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * OA审核操作请求DTO
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
@ApiModel(description = "OA审核操作请求")
public class ApproveOaApprovalRequest {

    @NotNull(message = "审核记录ID不能为空")
    @ApiModelProperty(value = "审核记录ID", required = true)
    private Integer approvalRecordId;

    @NotBlank(message = "来源表名不能为空")
    @ApiModelProperty(value = "来源表名", required = true)
    private String sourceTable;

    @NotNull(message = "来源记录ID不能为空")
    @ApiModelProperty(value = "来源记录ID", required = true)
    private Integer sourceId;

    @NotBlank(message = "审核结果不能为空")
    @ApiModelProperty(value = "审核结果：通过/驳回", required = true)
    private String result;

    @ApiModelProperty(value = "审核意见")
    private String opinion;

    @ApiModelProperty(value = "审核人ID")
    private Integer approverId;

    @ApiModelProperty(value = "审核人姓名")
    private String approverName;
}
