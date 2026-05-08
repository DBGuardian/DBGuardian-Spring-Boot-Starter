package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 委外处理合同状态更新请求
 */
@Data
@ApiModel("委外处理合同状态更新请求")
public class OutsourceProcessingContractStatusRequest {

    @ApiModelProperty(value = "合同状态")
    @NotNull(message = "合同状态不能为空")
    private String contractStatus;

    @ApiModelProperty(value = "审核意见")
    private String auditOpinion;
}
