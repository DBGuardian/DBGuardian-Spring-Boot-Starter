package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

/**
 * 运输合同保存请求（新增/修改时使用，只验证数据库必填字段）
 */
@Data
@ApiModel(description = "运输合同保存请求")
public class TransportContractSaveRequest {

    @ApiModelProperty(value = "签约类型：个人司机/运输公司", required = true)
    @NotBlank(message = "签约类型不能为空")
    private String signingType;

    @ApiModelProperty(value = "承运方名称", required = true)
    @NotBlank(message = "承运方名称不能为空")
    private String carrierName;

    @ApiModelProperty("联系人")
    private String contactPerson;

    @ApiModelProperty("联系电话（选填）")
    private String contactPhone;

    @ApiModelProperty("身份证号（个人司机必填）")
    private String idCardNo;

    @ApiModelProperty("统一社会信用代码（运输公司必填）")
    private String creditCode;

    @ApiModelProperty("开户银行")
    private String bankName;

    @ApiModelProperty("银行卡号")
    private String cardNumber;

    @ApiModelProperty("账户名称")
    private String accountName;

    @ApiModelProperty("乙方名称")
    private String partyBName;

    @ApiModelProperty("乙方统一社会信用代码")
    private String partyBCreditCode;

    @ApiModelProperty("乙方联系人")
    private String partyBContactPerson;

    @ApiModelProperty("乙方联系电话（选填）")
    private String partyBContactPhone;

    @ApiModelProperty("签订时间")
    private String signTime;

    @ApiModelProperty("有效期开始")
    private String validFrom;

    @ApiModelProperty("有效期结束")
    private String validTo;

    @ApiModelProperty(value = "结算方式：按趟次结算/按重量结算/按距离结算", required = true)
    @NotBlank(message = "结算方式不能为空")
    private String settlementMethod;

    @ApiModelProperty("单价")
    private BigDecimal unitPrice;

    @ApiModelProperty("计量单位")
    private String unit;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("关联车辆编号列表（签约类型为运输公司时使用）")
    private List<Integer> vehicleIds;
}
