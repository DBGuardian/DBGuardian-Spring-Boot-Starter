package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("业务员创建/更新请求")
public class SalespersonCreateRequest {

    @ApiModelProperty("关联系统员工编码（外部业务员为空）")
    private Integer employeeId;

    @ApiModelProperty("业务员姓名")
    private String salespersonName;

    @ApiModelProperty("业务员联系电话")
    private String salespersonPhone;

    @ApiModelProperty("业务员身份证号")
    private String salespersonIdCard;

    @ApiModelProperty("甲方名称")
    private String partyAName;

    @ApiModelProperty("甲方统一社会信用代码")
    private String partyACreditCode;

    @ApiModelProperty("乙方名称")
    private String partyBName;

    @ApiModelProperty("乙方统一社会信用代码")
    private String partyBCreditCode;

    @ApiModelProperty("乙方联系人")
    private String partyBContactPerson;

    @ApiModelProperty("乙方联系电话")
    private String partyBContactPhone;

    @ApiModelProperty("开户银行")
    private String bankName;

    @ApiModelProperty("银行卡号")
    private String cardNumber;

    @ApiModelProperty("账户名称")
    private String accountName;

    @ApiModelProperty("备注")
    private String remark;
}
