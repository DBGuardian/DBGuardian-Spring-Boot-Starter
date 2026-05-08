package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 日记账响应DTO
 */
@Data
@ApiModel("日记账响应")
public class FundDiaryResponse {

    @ApiModelProperty(value = "账户ID", example = "1")
    private Long accountId;

    @ApiModelProperty(value = "账户名称", example = "库存现金")
    private String accountName;

    @ApiModelProperty(value = "账户编码", example = "1001")
    private String accountCode;

    @ApiModelProperty(value = "账期ID", example = "3")
    private Long periodId;

    @ApiModelProperty(value = "账期编码", example = "202303")
    private String periodCode;

    @ApiModelProperty(value = "年份", example = "2023")
    private Integer year;

    @ApiModelProperty(value = "月份", example = "3")
    private Integer month;

    @ApiModelProperty(value = "期初余额")
    private InitialBalanceInfo initialBalance;

    @ApiModelProperty(value = "流水明细列表")
    private List<TransactionDetailInfo> transactions;

    @ApiModelProperty(value = "本期合计")
    private PeriodTotalInfo periodTotal;

    @ApiModelProperty(value = "本年累计")
    private YearTotalInfo yearTotal;

    /**
     * 期初余额信息
     */
    @Data
    @ApiModel("期初余额信息")
    public static class InitialBalanceInfo {
        @ApiModelProperty(value = "金额", example = "244604.00")
        private BigDecimal amount;

        @ApiModelProperty(value = "方向", example = "收入")
        private String direction;
    }

    /**
     * 流水明细信息
     */
    @Data
    @ApiModel("流水明细信息")
    public static class TransactionDetailInfo {
        @ApiModelProperty(value = "流水ID", example = "1")
        private Long transactionId;

        @ApiModelProperty(value = "流水编码", example = "LS-20230331-0001")
        private String transactionCode;

        @ApiModelProperty(value = "账户ID", example = "1")
        private Long accountId;

        @ApiModelProperty(value = "交易日期", example = "2023-03-31")
        private String transactionDate;

        @ApiModelProperty(value = "往来单位账号", example = "1234567890123456789")
        private String counterpartyAccount;

        @ApiModelProperty(value = "往来单位账户名称", example = "供应商A")
        private String counterpartyName;

        @ApiModelProperty(value = "往来单位开户银行名称", example = "中国工商银行")
        private String counterpartyBank;

        @ApiModelProperty(value = "用途", example = "采购原材料")
        private String purpose;

        @ApiModelProperty(value = "摘要", example = "支付运费")
        private String summary;

        @ApiModelProperty(value = "科目ID", example = "1")
        private Long subjectId;

        @ApiModelProperty(value = "科目编码", example = "1001")
        private String subjectCode;

        @ApiModelProperty(value = "科目名称", example = "库存现金")
        private String subjectName;

        @ApiModelProperty(value = "是否内部往来", example = "false")
        private Boolean internalTransfer;

        @ApiModelProperty(value = "关联账户ID（内部往来时使用）", example = "2")
        private Long relatedAccountId;

        @ApiModelProperty(value = "关联流水ID（内部往来时使用）", example = "3")
        private Long relatedTransactionId;
        
        @ApiModelProperty(value = "关联流水编码（内部往来时使用）", example = "LS-20260110-0002")
        private String relatedTransactionCode;

        @ApiModelProperty(value = "收入金额", example = "2000.00")
        private BigDecimal income;

        @ApiModelProperty(value = "支出金额", example = "600.00")
        private BigDecimal expenditure;

        @ApiModelProperty(value = "余额方向", example = "收入")
        private String direction;

        @ApiModelProperty(value = "余额", example = "244004.00")
        private BigDecimal balance;
        
        @ApiModelProperty(value = "回单文件ID", example = "123")
        private Integer receiptFileId;

        @ApiModelProperty(value = "回单文件访问URL", example = "http://.../file.pdf")
        private String receiptFileUrl;

        @ApiModelProperty(value = "回单文件名", example = "receipt.pdf")
        private String receiptFileName;

        @ApiModelProperty(value = "回单编号", example = "RN-20230101-001")
        private String receiptNo;

        @ApiModelProperty(value = "时间戳", example = "2025-01-02-09.16.05.396711")
        private String timestamp;

        @ApiModelProperty(value = "备注", example = "备注信息")
        private String remark;
    }

    /**
     * 本期合计信息
     */
    @Data
    @ApiModel("本期合计信息")
    public static class PeriodTotalInfo {
        @ApiModelProperty(value = "收入合计", example = "2000.00")
        private BigDecimal income;

        @ApiModelProperty(value = "支出合计", example = "35962.00")
        private BigDecimal expenditure;

        @ApiModelProperty(value = "余额方向", example = "收入")
        private String direction;

        @ApiModelProperty(value = "期末余额", example = "210642.00")
        private BigDecimal balance;
    }

    /**
     * 本年累计信息
     */
    @Data
    @ApiModel("本年累计信息")
    public static class YearTotalInfo {
        @ApiModelProperty(value = "累计收入", example = "234000.00")
        private BigDecimal income;

        @ApiModelProperty(value = "累计支出", example = "51068.00")
        private BigDecimal expenditure;

        @ApiModelProperty(value = "余额方向", example = "收入")
        private String direction;

        @ApiModelProperty(value = "期末余额", example = "210642.00")
        private BigDecimal balance;
    }
}

