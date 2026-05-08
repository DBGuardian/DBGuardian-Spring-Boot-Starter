package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量操作委外运输结算单请求DTO
 */
@Data
@ApiModel("批量操作委外运输结算单请求")
public class OutsourceSettlementBatchRequest {

    @NotEmpty(message = "请选择要操作的结算单")
    @ApiModelProperty(value = "结算单编号列表", required = true)
    private List<Integer> settlementIds;
}
