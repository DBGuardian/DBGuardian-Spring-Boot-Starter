package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量提交审核请求
 */
@Data
@ApiModel("批量提交审核请求")
public class ContractBatchSubmitAuditRequest {

    @NotEmpty(message = "请选择要提交的合同")
    @ApiModelProperty("合同编号列表")
    private List<Integer> contractIds;
}
