package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 业务费结算专用合同查询响应
 * 
 * 功能描述：为业务费结算页面提供专用的合同查询响应，仅包含必要字段
 * 展示字段：合同号、甲方名称、合同状态
 * 程序使用字段：合同编号、合同金额、签订时间
 */
@Data
@ApiModel("业务费结算专用合同查询响应")
public class BusinessFeeSettlementContractPageResponse {

    @ApiModelProperty("合同编号（程序使用）")
    private Integer contractId;

    @ApiModelProperty("合同号（业务可见的合同编号：HQ-YYYYMMDD-XXXXX）")
    private String contractNo;

    @ApiModelProperty("甲方名称")
    private String partyAName;

    @ApiModelProperty("合同状态")
    private String contractStatus;

    @ApiModelProperty("合同金额（程序使用）")
    private BigDecimal contractAmount;

    @ApiModelProperty("签订时间（程序使用）")
    private LocalDateTime signTime;

    @ApiModelProperty("创建时间（程序使用）")
    private LocalDateTime createTime;
}
