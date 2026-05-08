package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算导出明细 DTO（按量结算模式）
 */
@Data
@ApiModel("SettlementExportDetailDTO - 结算导出明细（按量结算）")
public class SettlementExportDetailDTO {

    @ApiModelProperty("结算单编号")
    private Long settlementId;

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

    @ApiModelProperty("结算头上的关联来源类型")
    private String headerSourceType;

    @ApiModelProperty("结算单状态")
    private String status;

    // ======== 明细字段（按量结算 / 总价包干） ========

    @ApiModelProperty("废物类别（按量结算为当前行，总价包干为合并后的多废物类别，分号分隔）")
    private String wasteCategory;

    @ApiModelProperty("废物代码（按量结算为当前行，总价包干为合并后的多废物代码，分号分隔）")
    private String wasteCode;

    @ApiModelProperty("废物名称（按量结算为当前行，总价包干为合并后的多废物名称，分号分隔）")
    private String wasteName;

    @ApiModelProperty("接收日期")
    private String receiveDate;

    @ApiModelProperty("明细关联来源类型")
    private String detailSourceType;

    @ApiModelProperty("明细关联来源单号")
    private String sourceOrderCode;

    @ApiModelProperty("广东省联单号")
    private String provinceManifestCode;

    @ApiModelProperty("结算模式")
    private String settlementMode;

    @ApiModelProperty("基本结算数量")
    private BigDecimal baseQuantity;

    @ApiModelProperty("基本计量单位")
    private String baseUnit;

    @ApiModelProperty("辅助结算数量（仅启用辅助核算时有值）")
    private BigDecimal auxQuantity;

    @ApiModelProperty("辅助计量单位（仅启用辅助核算时有值）")
    private String auxUnit;

    @ApiModelProperty("合同计划总量（包干使用）")
    private BigDecimal contractPlanTotal;

    @ApiModelProperty("辅助合同计划总量（包干使用，启用辅助核算时有值）")
    private BigDecimal auxContractPlanTotal;

    @ApiModelProperty("累积已结算量（包干使用）")
    private BigDecimal accumulatedQuantity;

    @ApiModelProperty("辅助累积已结算量（包干使用，启用辅助核算时有值）")
    private BigDecimal auxAccumulatedQuantity;

    @ApiModelProperty("本次累积量")
    private BigDecimal currentAccumulatedQuantity;

    @ApiModelProperty("超出量")
    private BigDecimal exceedQuantity;

    @ApiModelProperty("结算单价")
    private BigDecimal price;

    @ApiModelProperty("超出单价")
    private BigDecimal exceedPrice;

    @ApiModelProperty("超出金额")
    private BigDecimal exceedAmount;

    @ApiModelProperty("本次明细金额")
    private BigDecimal amount;
}

