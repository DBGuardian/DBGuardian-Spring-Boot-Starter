package com.erp.mapper.report;

import com.erp.controller.report.dto.ReceivableBalanceTrendResponse.ReceivableChangeDTO;
import com.erp.controller.report.dto.ReceivableBalanceTrendResponse.ContractPricingDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应收账款余额变动趋势 Mapper
 *
 * 功能描述：
 * - 查询各计价类型在时间粒度内的应收账款增减变动明细
 * - 应收增加来源：SETTLEMENT（结算类型=RECEIVABLE）按创建时间聚合，取结算金额
 * - 应收减少来源：SETTLEMENT_INVOICE_REL 按关联时间聚合，取发票关联金额（status != CANCELLED）
 * - 计价类型判断：CONTRACT → CONTRACT_ITEM.报价模式（PACKAGE/UNIT/MIXED）
 */
@Mapper
public interface ReceivableBalanceTrendMapper {

    /**
     * 查询截至 endDate（含）各计价类型的累计应收金额（结算单创建，按创建时间）
     *
     * 用途：计算历史起点余额（startDate 之前所有应收增加额）
     *
     * @param endDate 截止日期（YYYY-MM-DD）
     * @return 各计价类型累计结算金额列表
     */
    List<ReceivableChangeDTO> selectCumulativeSettlement(
            @Param("endDate") String endDate);

    /**
     * 查询截至 endDate（含）各计价类型的累计已收款金额（发票关联，按关联时间）
     *
     * 用途：计算历史起点余额（startDate 之前所有已收款减少额）
     * 来源：SETTLEMENT_INVOICE_REL.关联金额，排除 status=CANCELLED 的记录
     *
     * @param endDate 截止日期（YYYY-MM-DD）
     * @return 各计价类型累计已收款金额列表
     */
    List<ReceivableChangeDTO> selectCumulativeReceived(
            @Param("endDate") String endDate);

    /**
     * 按时间粒度查询区间内各计价类型的结算金额汇总（应收增加）
     *
     * 按结算单创建时间聚合，格式化为指定粒度的日期标签：
     * - day:   YYYY-MM-DD
     * - month: YYYY-MM
     * - year:  YYYY
     *
     * @param startDate   开始日期（YYYY-MM-DD，含）
     * @param endDate     结束日期（YYYY-MM-DD，含）
     * @param granularity 时间粒度：day / month / year
     * @return 各粒度各计价类型的结算金额
     */
    List<ReceivableChangeDTO> selectSettlementByPeriod(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("granularity") String granularity);

    /**
     * 按时间粒度查询区间内各计价类型的已收款金额汇总（应收减少）
     *
     * 来源：SETTLEMENT_INVOICE_REL.关联金额，按关联时间聚合，排除 status=CANCELLED 的记录
     * 格式化为指定粒度的日期标签
     *
     * @param startDate   开始日期（YYYY-MM-DD，含）
     * @param endDate     结束日期（YYYY-MM-DD，含）
     * @param granularity 时间粒度：day / month / year
     * @return 各粒度各计价类型的已收款金额
     */
    List<ReceivableChangeDTO> selectReceivedByPeriod(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("granularity") String granularity);
}
