package com.erp.service.report;

import com.erp.controller.report.dto.ReceivableBalanceTrendRequest;
import com.erp.controller.report.dto.ReceivableBalanceTrendResponse;

import java.util.Map;

/**
 * 应收账款余额变动趋势 Service
 *
 * 功能描述：
 * - 按计价类型（总价包干/按量结算/混合计价/总金额）统计应收账款余额随时间变动趋势
 * - 应收增加：RECEIVABLE 类型结算单创建（以创建时间计入对应时间粒度）
 * - 应收减少：SETTLEMENT_FUND_TRANSACTION_REL 关联资金流水（以关联时间计）
 * - 计价类型：结算单.合同号 → CONTRACT → CONTRACT_ITEM.报价模式 判断 PACKAGE/UNIT/MIXED
 * - 余额为累计净值：截至该时间点所有增加额 - 所有减少额
 */
public interface ReceivableBalanceTrendService {

    /**
     * 获取应收账款余额变动趋势（优先读取 Redis 缓存）
     *
     * @param request 查询请求（startDate, endDate, granularity）
     * @return 趋势图表数据
     */
    ReceivableBalanceTrendResponse getBalanceTrend(ReceivableBalanceTrendRequest request);

    /**
     * 重新计算应收账款余额变动趋势（清除缓存后强制重算）
     *
     * @param request 查询请求
     * @return 趋势图表数据
     */
    ReceivableBalanceTrendResponse recalculate(ReceivableBalanceTrendRequest request);

    /**
     * 清除所有应收账款余额趋势缓存
     *
     * @return 操作结果
     */
    Map<String, Object> clearAllCache();
}
