package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量撤回报价单请求
 */
@Data
@ApiModel("批量撤回报价单请求")
public class QuotationBatchRevokeRequest {

    @NotEmpty(message = "请选择要撤回的报价单")
    @ApiModelProperty(value = "报价单ID列表", required = true)
    private List<Integer> quotationIds;
}
