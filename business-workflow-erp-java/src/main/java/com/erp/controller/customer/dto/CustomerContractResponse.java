package com.erp.controller.customer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户合同记录响应
 */
@Data
@ApiModel("客户合同记录响应")
public class CustomerContractResponse {

    @ApiModelProperty("合同编号")
    private Integer contractId;

    @ApiModelProperty("合同金额")
    private BigDecimal contractAmount;

    @ApiModelProperty("签订时间")
    private LocalDateTime signTime;

    @ApiModelProperty("合同状态")
    private String contractStatus;

    @ApiModelProperty("合同有效期开始")
    private LocalDateTime validFrom;

    @ApiModelProperty("合同有效期结束")
    private LocalDateTime validTo;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}


