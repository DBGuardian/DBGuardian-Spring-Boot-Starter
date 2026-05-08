package com.erp.controller.oa.dto;

import com.erp.entity.oa.OaApprovalRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * OA审核详情响应DTO
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
@ApiModel(description = "OA审核详情响应")
public class OaApprovalDetailResponse {

    @ApiModelProperty(value = "审核记录")
    private OaApprovalRecord record;

    @ApiModelProperty(value = "审批历史")
    private List<Object> details;
}
