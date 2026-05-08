package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资金账户列表项响应
 *
 * 不直接暴露创建人编码和更新人编码，仅返回创建人/更新人姓名。
 */
@Data
@ApiModel("资金账户列表项")
public class FundAccountListItemResponse {

    @ApiModelProperty("账户ID")
    private Long accountId;

    @ApiModelProperty("账户编码")
    private String accountCode;

    @ApiModelProperty("账户名称")
    private String accountName;

    @ApiModelProperty("账户类型：BANK、PETTY_CASH、CASH")
    private String accountType;

    @ApiModelProperty("账户银行账号")
    private String accountBankAccount;

    @ApiModelProperty("账户银行/机构")
    private String accountBankInstitution;

    @ApiModelProperty("是否启用")
    private Boolean enabled;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("创建人姓名")
    private String createUserName;

    @ApiModelProperty("更新人姓名")
    private String updateUserName;

    @ApiModelProperty("创建人编码（前端用于 operateScope=SELF 时判断是否为自己创建）")
    private Integer creatorId;
}





