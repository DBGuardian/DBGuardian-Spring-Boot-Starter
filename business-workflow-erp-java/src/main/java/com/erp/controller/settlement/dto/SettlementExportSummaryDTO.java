package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算汇总导出 DTO（仅汇总表字段）
 */
@Data
@ApiModel("SettlementExportSummaryDTO - 结算汇总导出汇总行")
public class SettlementExportSummaryDTO {

    @ApiModelProperty("结算单单号")
    private String settlementCode;

    @ApiModelProperty("合同号")
    private String contractCode;

    @ApiModelProperty("甲方名称（客户名称）")
    private String partyAName;

    @ApiModelProperty("甲方统一社会信用代码")
    private String partyASocialCreditCode;

    @ApiModelProperty("结算类型：RECEIVABLE=收款 / PAYABLE=付款")
    private String settlementType;

    @ApiModelProperty("关联来源类型：CONTRACT/WAREHOUSING/TRANSPORT")
    private String sourceType;

    @ApiModelProperty("结算金额")
    private BigDecimal settlementAmount;

    @ApiModelProperty("已收/已付金额")
    private BigDecimal receivedAmount;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("制单人名称")
    private String creatorName;
}

