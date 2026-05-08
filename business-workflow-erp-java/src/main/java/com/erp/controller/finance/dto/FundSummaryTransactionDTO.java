package com.erp.controller.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 汇总表资金流水数据传输对象
 * 用于汇总表查询优化，连表查询科目信息
 */
@Data
public class FundSummaryTransactionDTO {

    /**
     * 交易ID
     */
    private Long transactionId;

    /**
     * 账户编号
     */
    private Long accountId;

    /**
     * 科目编号
     */
    private Long subjectId;

    /**
     * 科目编码
     */
    private String subjectCode;

    /**
     * 科目名称
     */
    private String subjectName;

    /**
     * 科目分类
     */
    private String subjectCategory;

    /**
     * 科目全称
     */
    private String fullSubjectName;

    /**
     * 余额方向
     */
    private String balanceDirection;

    /**
     * 交易日期
     */
    private LocalDate transactionDate;

    /**
     * 交易类型
     */
    private String transactionType;

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 是否内部往来
     */
    private Boolean internalTransfer;

    /**
     * 数据类型标识
     * QUARTER: 季度内数据
     * PRE_QUARTER: 季度前数据
     * YEAR: 全年数据
     */
    private String dataType;
}