package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 运输合同审核请求（审核通过时使用，包含完整字段验证）
 */
@Data
@ApiModel(description = "运输合同审核请求")
public class TransportContractAuditRequest {

    @ApiModelProperty(value = "签约类型：个人司机/运输公司", required = true)
    @NotBlank(message = "签约类型不能为空")
    private String signingType;

    @ApiModelProperty(value = "承运方名称", required = true)
    @NotBlank(message = "承运方名称不能为空")
    private String carrierName;

    @ApiModelProperty(value = "联系人")
    private String contactPerson;

    @ApiModelProperty(value = "联系电话")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的11位手机号")
    private String contactPhone;

    @ApiModelProperty(value = "身份证号（个人司机必填）")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "请输入有效的18位身份证号")
    private String idCardNo;

    @ApiModelProperty(value = "统一社会信用代码（运输公司必填）")
    private String creditCode;

    @ApiModelProperty(value = "开户银行", required = true)
    @NotBlank(message = "开户银行不能为空")
    private String bankName;

    @ApiModelProperty(value = "银行卡号", required = true)
    @NotBlank(message = "银行卡号不能为空")
    @Pattern(regexp = "^\\d{12,19}$", message = "银行卡号应为12-19位数字")
    private String cardNumber;

    @ApiModelProperty(value = "账户名称", required = true)
    @NotBlank(message = "账户名称不能为空")
    private String accountName;

    @ApiModelProperty(value = "乙方名称", required = true)
    @NotBlank(message = "乙方名称不能为空")
    private String partyBName;

    @ApiModelProperty(value = "乙方统一社会信用代码")
    private String partyBCreditCode;

    @ApiModelProperty(value = "乙方联系人")
    private String partyBContactPerson;

    @ApiModelProperty(value = "乙方联系电话")
    private String partyBContactPhone;

    @ApiModelProperty(value = "签订时间", required = true)
    @NotBlank(message = "签订时间不能为空")
    private String signTime;

    @ApiModelProperty(value = "有效期开始", required = true)
    @NotBlank(message = "有效期开始时间不能为空")
    private String validFrom;

    @ApiModelProperty(value = "有效期结束", required = true)
    @NotBlank(message = "有效期结束时间不能为空")
    private String validTo;

    @ApiModelProperty(value = "结算方式：按趟次结算/按重量结算/按距离结算", required = true)
    @NotBlank(message = "结算方式不能为空")
    private String settlementMethod;

    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "计量单位")
    private String unit;

    @ApiModelProperty(value = "备注")
    @Size(max = 500, message = "备注不能超过500字")
    private String remark;

    @ApiModelProperty("关联车辆编号列表（签约类型为运输公司时使用）")
    private List<Integer> vehicleIds;
}
