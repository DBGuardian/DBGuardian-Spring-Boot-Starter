package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量操作业务费结算单请求DTO
 */
@Data
@ApiModel("批量操作业务费结算单请求")
public class BusinessFeeBatchRequest {

    @NotEmpty(message = "请选择要操作的业务费结算单")
    @ApiModelProperty(value = "业务序号列表", required = true)
    private List<Integer> businessSeqs;
}
