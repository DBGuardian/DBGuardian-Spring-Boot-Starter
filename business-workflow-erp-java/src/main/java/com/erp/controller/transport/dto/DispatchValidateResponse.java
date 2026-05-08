package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 运输单校验响应
 */
@Data
@ApiModel("运输单校验响应")
public class DispatchValidateResponse {

    @ApiModelProperty("是否超限")
    private Boolean overLimit;

    @ApiModelProperty("超限条目")
    private List<DispatchOverLimitItemResponse> overLimitItems;

    @ApiModelProperty("是否缺失合同")
    private Boolean contractMissing;
}


