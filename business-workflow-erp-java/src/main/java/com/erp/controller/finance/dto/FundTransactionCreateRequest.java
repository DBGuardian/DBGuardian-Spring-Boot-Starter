package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建资金流水请求DTO
 */
@Data
@ApiModel("创建资金流水请求")
public class FundTransactionCreateRequest {

    @ApiModelProperty(value = "账户ID", example = "1", required = true)
    @NotNull(message = "账户ID不能为空")
    @JsonProperty("account_id")
    private Long accountId;

    @ApiModelProperty(value = "账期ID", example = "3", required = true)
    @NotNull(message = "账期ID不能为空")
    @JsonProperty("period_id")
    private Long periodId;

    @ApiModelProperty(value = "交易日期（格式：YYYY-MM-DD）", example = "2023-03-31", required = true)
    @NotNull(message = "交易日期不能为空")
    @JsonProperty("transaction_date")
    private LocalDate transactionDate;

    @ApiModelProperty(value = "交易类型：INCOME（收入）、EXPENDITURE（支出）", example = "EXPENDITURE", required = true)
    @NotNull(message = "交易类型不能为空")
    @JsonProperty("transaction_type")
    private String transactionType;

    @ApiModelProperty(value = "交易金额", example = "600.00", required = true)
    @NotNull(message = "交易金额不能为空")
    @Min(value = 0, message = "交易金额必须大于0")
    private BigDecimal amount;

    @ApiModelProperty(value = "往来单位账号", example = "1234567890123456789")
    @JsonProperty("counterparty_account")
    private String counterpartyAccount;

    @ApiModelProperty(value = "往来单位账户名称", example = "供应商A")
    @JsonProperty("counterparty_name")
    private String counterpartyName;

    @ApiModelProperty(value = "往来单位开户银行名称", example = "中国工商银行")
    @JsonProperty("counterparty_bank")
    private String counterpartyBank;

    @ApiModelProperty(value = "用途", example = "采购原材料")
    @JsonProperty("purpose")
    private String purpose;

    @ApiModelProperty(value = "摘要", example = "支付运费", required = true)
    @NotNull(message = "摘要不能为空")
    private String summary;

    @ApiModelProperty(value = "科目ID", example = "1")
    @JsonProperty("subject_id")
    private Long subjectId;

    @ApiModelProperty(value = "是否内部往来", example = "false")
    @JsonProperty("internal_transfer")
    private Boolean internalTransfer;

    @ApiModelProperty(value = "关联账户ID（内部往来时使用）", example = "2")
    @JsonProperty("related_account_id")
    private Long relatedAccountId;

    @ApiModelProperty(value = "关联流水ID（内部往来时使用）", example = "123")
    @JsonProperty("related_transaction_id")
    private Long relatedTransactionId;

    @ApiModelProperty(value = "回单编号", example = "RC20231201001")
    @JsonProperty("receipt_no")
    private String receiptNo;

    @ApiModelProperty(value = "备注", example = "备注信息")
    @JsonProperty("remark")
    private String remark;

    @ApiModelProperty(value = "时间戳", example = "2025-01-02-09.16.05.396711")
    @JsonProperty("timestamp")
    private String timestamp;
}


