package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;

/**
 * 汇总表响应DTO
 */
@Data
@ApiModel("汇总表响应")
public class FundSummaryResponse {

    @ApiModelProperty(value = "账期ID", example = "3")
    private Long periodId;

    @ApiModelProperty(value = "账期编码", example = "202303")
    private String periodCode;

    @ApiModelProperty(value = "年份", example = "2023")
    private Integer year;

    @ApiModelProperty(value = "月份", example = "3")
    private Integer month;

    @ApiModelProperty(value = "账户汇总列表")
    private List<AccountSummaryInfo> accounts;
    
    @ApiModelProperty(value = "按科目汇总的行数据（前端动态字段，如 m4_income/m4_expenditure）")
    private List<Map<String, Object>> rows;

    @ApiModelProperty(value = "收入小计（按列名，如 m4_income）")
    private Map<String, BigDecimal> incomeSubtotal;

    @ApiModelProperty(value = "支出小计（按列名，如 m4_expenditure）")
    private Map<String, BigDecimal> expenditureSubtotal;

    @ApiModelProperty(value = "账户余额汇总")
    private AccountBalance accountBalance;

    @Data
    @ApiModel("账户余额汇总")
    public static class AccountBalance {
        @ApiModelProperty("期初余额")
        private BigDecimal openingBalance;
        @ApiModelProperty("期末余额")
        private BigDecimal closingBalance;
    }

    /**
     * 账户汇总信息
     */
    @Data
    @ApiModel("账户汇总信息")
    public static class AccountSummaryInfo {
        @ApiModelProperty(value = "账户ID", example = "1")
        private Long accountId;

        @ApiModelProperty(value = "账户编码", example = "1001")
        private String accountCode;

        @ApiModelProperty(value = "账户名称", example = "库存现金")
        private String accountName;

        @ApiModelProperty(value = "期初余额")
        private InitialBalanceInfo initialBalance;

        @ApiModelProperty(value = "本期合计")
        private PeriodTotalInfo periodTotal;

        @ApiModelProperty(value = "本年累计")
        private YearTotalInfo yearTotal;
    }

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

