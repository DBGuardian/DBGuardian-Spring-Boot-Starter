package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 资金账户更新请求 DTO
 *
 * 用于更新资金账户基础信息（不修改创建人编码和创建时间）。
 */
@Data
@ApiModel("资金账户更新请求")
public class FundAccountUpdateRequest {

    /**
     * 账户名称
     */
    @ApiModelProperty(value = "账户名称", required = true, example = "工商银行基本户")
    @NotBlank(message = "账户名称不能为空")
    private String accountName;

    /**
     * 账户类型：BANK（银行）、PETTY_CASH（备用金）、CASH（现金）
     */
    @ApiModelProperty(value = "账户类型：BANK（银行）、PETTY_CASH（备用金）、CASH（现金）", required = true, example = "BANK")
    @NotBlank(message = "账户类型不能为空")
    private String accountType;

    /**
     * 账户银行账号
     */
    @ApiModelProperty(value = "账户银行账号", example = "6222021234567890123")
    private String accountBankAccount;

    /**
     * 账户银行/机构
     */
    @ApiModelProperty(value = "账户银行/机构", example = "中国工商银行")
    private String accountBankInstitution;

    /**
     * 是否启用
     */
    @ApiModelProperty(value = "是否启用：true-启用，false-停用", example = "true")
    private Boolean enabled;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注", example = "基本开户")
    private String remark;
}





