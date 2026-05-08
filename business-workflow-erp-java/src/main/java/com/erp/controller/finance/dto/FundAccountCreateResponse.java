package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 资金账户创建响应 DTO
 *
 * 对应文档《资金管理实现说明》中的“创建账户”接口返回体 body：
 * {
 *   "account_id": 1,
 *   "account_code": "ACC-001"
 * }
 */
@Data
@ApiModel("资金账户创建响应")
public class FundAccountCreateResponse {

    /**
     * 账户编号
     */
    @ApiModelProperty(value = "账户ID", example = "1")
    @JsonProperty("account_id")
    private Long accountId;

    /**
     * 账户编码
     */
    @ApiModelProperty(value = "账户编码", example = "ACC-20260101-0001")
    @JsonProperty("account_code")
    private String accountCode;
}



