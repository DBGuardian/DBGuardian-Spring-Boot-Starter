package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 闭环监控看板统计数据DTO
 * 支持合同维度、收款中心、异常监控三大类别的统计指标
 *
 * @author ERP System
 * @date 2025-02-05
 */
@Data
@ApiModel(description = "闭环监控看板统计数据")
public class ClosureDashboardStats implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 合同维度指标 ==========
    /**
     * 合同签订数量
     */
    @ApiModelProperty(value = "合同签订数量", example = "156")
    private Integer contractCount;

    /**
     * 合同签订金额
     */
    @ApiModelProperty(value = "合同签订金额", example = "12580000")
    private BigDecimal contractAmount;

    /**
     * 合同签订数量同比趋势（与去年同期相比，百分比）
     */
    @ApiModelProperty(value = "合同签订数量同比趋势（%）", example = "12.5")
    private BigDecimal contractCountTrend;

    /**
     * 合同签订数量环比趋势（与上月相比，百分比）
     */
    @ApiModelProperty(value = "合同签订数量环比趋势（%）", example = "5.2")
    private BigDecimal contractCountMomTrend;

    /**
     * 合同签订金额同比趋势（百分比）
     */
    @ApiModelProperty(value = "合同签订金额趋势（%）", example = "8.3")
    private BigDecimal contractAmountTrend;

    /**
     * 合同签订金额环比趋势（百分比）
     */
    @ApiModelProperty(value = "合同签订金额环比趋势（%）", example = "3.1")
    private BigDecimal contractAmountMomTrend;

    /**
     * 合同执行率（百分比）
     */
    @ApiModelProperty(value = "合同执行率（%）", example = "78.5")
    private BigDecimal executionRate;

    /**
     * 合同执行率趋势（百分比）
     */
    @ApiModelProperty(value = "合同执行率趋势（%）", example = "2.1")
    private BigDecimal executionRateTrend;

    /**
     * 超期未执行合同数量
     */
    @ApiModelProperty(value = "超期未执行合同数量", example = "12")
    private Integer overdueExecutionCount;

    /**
     * 超期未执行合同数量趋势（百分比）
     */
    @ApiModelProperty(value = "超期未执行合同数量趋势（%）", example = "-5.2")
    private BigDecimal overdueExecutionTrend;

    // ========== 合同维度扩展指标 ==========

    /**
     * 合同签订重量（本月累积，单位：吨）
     */
    @ApiModelProperty(value = "合同签订重量（吨）", example = "1250.5")
    private BigDecimal contractWeight;

    /**
     * 合同签订重量同比趋势（%）
     */
    @ApiModelProperty(value = "合同签订重量同比趋势（%）", example = "8.2")
    private BigDecimal contractWeightTrend;

    /**
     * 合同签订重量环比趋势（%）
     */
    @ApiModelProperty(value = "合同签订重量环比趋势（%）", example = "3.5")
    private BigDecimal contractWeightMomTrend;

    /**
     * 合同变动-增量（本月新增合同数）
     */
    @ApiModelProperty(value = "本月新增合同数", example = "23")
    private Integer contractNewCount;

    /**
     * 合同变动-存量（本月有效执行中合同数）
     */
    @ApiModelProperty(value = "本月存量合同数", example = "133")
    private Integer contractActiveCount;

    /**
     * 合同跟进-即将到期（30天内到期合同数）
     */
    @ApiModelProperty(value = "即将到期合同数（30天内）", example = "12")
    private Integer contractExpiringCount;

    /**
     * 合同跟进-客户丢失（本月状态变为已拒绝/终止的合同数）
     */
    @ApiModelProperty(value = "本月客户丢失合同数", example = "5")
    private Integer contractLostCount;

    // ========== 收款中心指标 ==========
    /**
     * 应收金额（本月1日起结算的收款结算单结算金额）
     */
    @ApiModelProperty(value = "应收金额", example = "500000")
    private BigDecimal receivableAmount;

    /**
     * 应收金额同比趋势（%）
     */
    @ApiModelProperty(value = "应收金额同比趋势（%）", example = "8.3")
    private BigDecimal receivableAmountYoy;

    /**
     * 应收金额环比趋势（%）
     */
    @ApiModelProperty(value = "应收金额环比趋势（%）", example = "3.1")
    private BigDecimal receivableAmountMom;

    /**
     * 已收金额（本月1日起结算的收款结算单已收金额）
     */
    @ApiModelProperty(value = "已收金额", example = "300000")
    private BigDecimal receivedAmount;

    /**
     * 已收金额同比趋势（%）
     */
    @ApiModelProperty(value = "已收金额同比趋势（%）", example = "6.2")
    private BigDecimal receivedAmountYoy;

    /**
     * 已收金额环比趋势（%）
     */
    @ApiModelProperty(value = "已收金额环比趋势（%）", example = "2.7")
    private BigDecimal receivedAmountMom;

    /**
     * 未收金额（应收金额-已收金额）
     */
    @ApiModelProperty(value = "未收金额", example = "200000")
    private BigDecimal unreceivedAmount;

    /**
     * 未收金额同比趋势（%）
     */
    @ApiModelProperty(value = "未收金额同比趋势（%）", example = "-5.8")
    private BigDecimal unreceivedAmountYoy;

    /**
     * 未收金额环比趋势（%）
     */
    @ApiModelProperty(value = "未收金额环比趋势（%）", example = "-2.4")
    private BigDecimal unreceivedAmountMom;

    /**
     * 逾期收款金额
     */
    @ApiModelProperty(value = "逾期收款金额", example = "50000")
    private BigDecimal overduePaymentAmount;

    /**
     * 逾期收款金额趋势（百分比）
     */
    @ApiModelProperty(value = "逾期收款金额趋势（%）", example = "-12.3")
    private BigDecimal overduePaymentAmountTrend;

    // ========== 异常监控指标 ==========
    /**
     * 时间顺序异常数量
     */
    @ApiModelProperty(value = "时间顺序异常数量", example = "12")
    private Integer timeSequenceViolations;

    /**
     * 时间顺序异常风险等级分布
     */
    @ApiModelProperty(value = "时间顺序异常风险等级分布")
    private Map<String, Integer> timeSequenceRiskBreakdown;

    /**
     * 金额异常数量
     */
    @ApiModelProperty(value = "金额异常数量", example = "8")
    private Integer amountMismatches;

    /**
     * 金额异常风险等级分布
     */
    @ApiModelProperty(value = "金额异常风险等级分布")
    private Map<String, Integer> amountMismatchRiskBreakdown;

    /**
     * 状态异常数量
     */
    @ApiModelProperty(value = "状态异常数量", example = "6")
    private Integer statusInconsistencies;

    /**
     * 状态异常风险等级分布
     */
    @ApiModelProperty(value = "状态异常风险等级分布")
    private Map<String, Integer> statusInconsistencyRiskBreakdown;

    /**
     * 数据不一致数量
     */
    @ApiModelProperty(value = "数据不一致数量", example = "4")
    private Integer dataInconsistencies;

    /**
     * 数据不一致风险等级分布
     */
    @ApiModelProperty(value = "数据不一致风险等级分布")
    private Map<String, Integer> dataInconsistencyRiskBreakdown;

    /**
     * 关联缺失数量
     */
    @ApiModelProperty(value = "关联缺失数量", example = "18")
    private Integer missingAssociations;

    /**
     * 关联缺失风险等级分布
     */
    @ApiModelProperty(value = "关联缺失风险等级分布")
    private Map<String, Integer> missingAssociationRiskBreakdown;

    // ========== 兼容旧版本字段 ==========
    /**
     * 结算单总金额
     */
    @ApiModelProperty(value = "结算单总金额", example = "500000")
    private BigDecimal totalSettlementAmount;

    /**
     * 已收款总金额
     */
    @ApiModelProperty(value = "已收款总金额", example = "300000")
    private BigDecimal totalReceivedAmount;

    /**
     * 未收款总金额
     */
    @ApiModelProperty(value = "未收款总金额", example = "200000")
    private BigDecimal totalUnreceivedAmount;

    /**
     * 逾期结算单数量
     */
    @ApiModelProperty(value = "逾期结算单数量", example = "5")
    private Integer overdueSettlementsCount;

    /**
     * 高风险结算单数量
     */
    @ApiModelProperty(value = "高风险结算单数量", example = "3")
    private Integer highRiskSettlementsCount;

    /**
     * 近期收款数量
     */
    @ApiModelProperty(value = "近期收款数量", example = "12")
    private Integer recentReceiptsCount;

    /**
     * 待审核数量
     */
    @ApiModelProperty(value = "待审核数量", example = "8")
    private Integer pendingReviewsCount;
}
