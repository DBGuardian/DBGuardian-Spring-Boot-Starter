package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 资金账户创建请求 DTO
 *
 * 对应文档《资金管理实现说明》中的“创建账户”接口入参：
 * {
 *   "account_name": "工商银行",
 *   "account_type": "BANK",
 *   "remark": ""
 * }
 */
@Data
@ApiModel("资金账户创建请求")
public class FundAccountCreateRequest {

    /**
     * 账户名称
     */
    @ApiModelProperty(value = "账户名称", required = true, example = "工商银行")
    @NotBlank(message = "账户名称不能为空")
    @JsonProperty("account_name")
    private String accountName;

    /**
     * 账户类型：BANK（银行）、PETTY_CASH（备用金）、CASH（现金）
     */
    @ApiModelProperty(value = "账户类型：BANK（银行）、PETTY_CASH（备用金）、CASH（现金）", required = true, example = "BANK")
    @NotBlank(message = "账户类型不能为空")
    @JsonProperty("account_type")
    private String accountType;

    /**
     * 账户银行账号
     */
    @ApiModelProperty(value = "账户银行账号", example = "6222021234567890123")
    @JsonProperty("account_bank_account")
    private String accountBankAccount;

    /**
     * 账户银行/机构
     */
    @ApiModelProperty(value = "账户银行/机构", example = "中国工商银行")
    @JsonProperty("account_bank_institution")
    private String accountBankInstitution;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注", example = "基本开户")
    @JsonProperty("remark")
    private String remark;
    
    /**
     * 所属组织ID
     */
    @ApiModelProperty(value = "所属组织ID", example = "1")
    @JsonProperty("organization_id")
    private Long organizationId;
}



