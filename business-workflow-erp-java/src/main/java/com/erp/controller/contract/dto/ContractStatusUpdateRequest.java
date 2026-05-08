package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 合同状态更新请求
 */
@Data
@ApiModel("合同状态更新请求")
public class ContractStatusUpdateRequest {

    @ApiModelProperty(value = "合同编号（从路径参数获取，无需在请求体中传递）")
    private Integer contractId;

    @ApiModelProperty(value = "合同状态：待审核/已通过/执行中/已完结/已归档/已驳回", required = true)
    @NotBlank(message = "合同状态不能为空")
    private String contractStatus;

    @ApiModelProperty(value = "审核意见（可选）")
    private String auditOpinion;
}


