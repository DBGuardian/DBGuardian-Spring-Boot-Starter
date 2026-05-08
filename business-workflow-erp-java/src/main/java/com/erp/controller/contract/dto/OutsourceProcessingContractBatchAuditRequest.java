package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 委外处理合同批量审核请求
 */
@Data
@ApiModel("委外处理合同批量审核请求")
public class OutsourceProcessingContractBatchAuditRequest {

    @ApiModelProperty(value = "合同编号列表")
    @NotEmpty(message = "合同编号列表不能为空")
    private List<Integer> contractIds;
}
