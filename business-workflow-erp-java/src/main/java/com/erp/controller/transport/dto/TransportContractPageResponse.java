package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 运输合同分页响应
 */
@Data
@ApiModel(description = "运输合同分页响应")
public class TransportContractPageResponse {

    @ApiModelProperty("合同主键ID")
    private Integer contractId;

    @ApiModelProperty("合同单号")
    private String contractNo;

    @ApiModelProperty("签约类型")
    private String signingType;

    @ApiModelProperty("承运方名称")
    private String carrierName;

    @ApiModelProperty("联系人")
    private String contactPerson;

    @ApiModelProperty("联系电话")
    private String contactPhone;

    @ApiModelProperty("身份证号")
    private String idCardNo;

    @ApiModelProperty("统一社会信用代码")
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

    @ApiModelProperty("乙方联系电话")
    private String partyBContactPhone;

    @ApiModelProperty("签订时间")
    private LocalDate signTime;

    @ApiModelProperty("有效期开始")
    private LocalDate validFrom;

    @ApiModelProperty("有效期结束")
    private LocalDate validTo;

    @ApiModelProperty("结算方式")
    private String settlementMethod;

    @ApiModelProperty("单价")
    private BigDecimal unitPrice;

    @ApiModelProperty("计量单位")
    private String unit;

    @ApiModelProperty("合同总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty("已结算金额")
    private BigDecimal settledAmount;

    @ApiModelProperty("合同状态")
    private String status;

    @ApiModelProperty("审核意见")
    private String auditOpinion;

    @ApiModelProperty("审核人编码")
    private Integer auditorId;

    @ApiModelProperty("审核人姓名")
    private String auditorName;

    @ApiModelProperty("审核时间")
    private java.time.LocalDateTime auditTime;

    @ApiModelProperty("合同文件编号")
    private Integer contractFileId;

    @ApiModelProperty("合同文件路径")
    private String contractFilePath;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建人编码")
    private Integer creatorId;

    @ApiModelProperty("创建人姓名")
    private String creatorName;

    @ApiModelProperty("创建时间")
    private java.time.LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private java.time.LocalDateTime updateTime;
}
