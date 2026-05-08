package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 危废合同下拉列表响应
 */
@Data
@ApiModel("危废合同下拉列表响应")
public class ContractSelectResponse {

    @ApiModelProperty("合同ID")
    private Integer contractId;

    @ApiModelProperty("合同号")
    private String contractNo;

    @ApiModelProperty("企业名称")
    private String enterpriseName;

    @ApiModelProperty("合同状态")
    private String status;

    @ApiModelProperty("签订日期")
    private String signDate;
}
