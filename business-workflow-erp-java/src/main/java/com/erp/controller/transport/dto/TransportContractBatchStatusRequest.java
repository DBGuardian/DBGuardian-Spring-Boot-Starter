package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 运输合同批量状态更新请求DTO
 */
@Data
@ApiModel(description = "运输合同批量状态更新请求")
public class TransportContractBatchStatusRequest {

    @NotNull(message = "合同ID列表不能为空")
    @NotEmpty(message = "合同ID列表不能为空")
    @ApiModelProperty(value = "合同ID列表", required = true)
    private List<Integer> contractIds;

    @NotNull(message = "目标状态不能为空")
    @ApiModelProperty(value = "目标状态", required = true)
    private String status;
}
