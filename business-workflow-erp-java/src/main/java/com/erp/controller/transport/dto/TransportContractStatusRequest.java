package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 更新运输合同状态请求
 */
@Data
@ApiModel(description = "更新运输合同状态请求")
public class TransportContractStatusRequest {

    @ApiModelProperty("目标状态")
    @NotBlank(message = "目标状态不能为空")
    private String status;

    @ApiModelProperty("审核意见")
    private String auditOpinion;
}
