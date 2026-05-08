package com.erp.controller.oa.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 重新提交审核请求DTO
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
@ApiModel(description = "重新提交审核请求")
public class ResubmitApprovalRequest {

    @NotNull(message = "原审核记录ID不能为空")
    @ApiModelProperty(value = "原审核记录ID", required = true)
    private Integer originalRecordId;

    @NotBlank(message = "来源表名不能为空")
    @ApiModelProperty(value = "来源表名", required = true)
    private String sourceTable;

    @NotNull(message = "来源记录ID不能为空")
    @ApiModelProperty(value = "来源记录ID", required = true)
    private Integer sourceId;

    @NotNull(message = "提交人ID不能为空")
    @ApiModelProperty(value = "提交人ID", required = true)
    private Integer submitterId;

    @NotBlank(message = "提交人姓名不能为空")
    @ApiModelProperty(value = "提交人姓名", required = true)
    private String submitterName;
}
