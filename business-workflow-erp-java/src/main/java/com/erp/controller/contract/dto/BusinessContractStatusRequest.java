package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 更新业务合作合同状态请求
 */
@Data
@ApiModel("业务合作合同状态更新请求")
public class BusinessContractStatusRequest {

    @ApiModelProperty(value = "合同ID列表（批量操作时使用）")
    private List<Integer> contractIds;

    @ApiModelProperty(value = "目标状态：待审核/审核中/执行中/已驳回/已完结/已归档", required = true)
    @NotBlank(message = "状态不能为空")
    private String status;

    @ApiModelProperty("审核意见（拒绝时必填）")
    private String auditOpinion;
}
