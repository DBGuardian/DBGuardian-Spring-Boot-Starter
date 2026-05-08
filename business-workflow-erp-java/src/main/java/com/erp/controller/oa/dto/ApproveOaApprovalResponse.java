package com.erp.controller.oa.dto;

import com.erp.entity.oa.OaApprovalRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * OA审核操作响应DTO
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
@ApiModel(description = "OA审核操作响应")
public class ApproveOaApprovalResponse {

    @ApiModelProperty(value = "是否成功")
    private boolean success;

    @ApiModelProperty(value = "更新后的审核记录")
    private OaApprovalRecord updatedRecord;
}
