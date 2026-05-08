package com.erp.controller.oa.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 撤回审核请求
 */
@Data
@ApiModel(description = "撤回审核请求")
public class WithdrawApprovalRequest {

    @NotNull(message = "审批记录ID不能为空")
    @ApiModelProperty(value = "审批记录ID", required = true)
    private Integer approvalRecordId;

    @NotNull(message = "来源表不能为空")
    @ApiModelProperty(value = "来源表名", required = true)
    private String sourceTable;

    @NotNull(message = "来源ID不能为空")
    @ApiModelProperty(value = "来源记录ID", required = true)
    private Integer sourceId;

    @ApiModelProperty(value = "撤回原因")
    private String reason;
}
