package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("业务员下拉框响应")
public class SalespersonSelectResponse {

    @ApiModelProperty("业务员主键ID")
    private Integer salespersonId;

    @ApiModelProperty("业务员姓名")
    private String salespersonName;
}
