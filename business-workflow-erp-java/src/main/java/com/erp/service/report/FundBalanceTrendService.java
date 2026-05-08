package com.erp.service.report;

import com.erp.controller.report.dto.FundBalanceTrendRequest;
import com.erp.controller.report.dto.FundBalanceTrendResponse;

import java.util.Map;

/**
 * 资金余额变动趋势 Service
 *
 * 功能描述：
 * - 获取资金余额变动趋势图表数据（支持 Redis 缓存，有效期 7 天）
 * - 重新计算（清除缓存后强制重算）
 * - 清除所有资金余额趋势缓存
 *
 * 缓存策略：
 * - 首次加载：自动计算数据并缓存到 Redis（TTL = 7 天）
 * - 后续加载：直接从 Redis 返回缓存数据（fromCache = true）
 * - 防并发：使用 :computing 标记防止同一缓存键被并发重复计算
 * - 手动刷新：调用 recalculate 接口清除缓存并重算
 */
public interface FundBalanceTrendService {

    /**
     * 获取资金余额变动趋势（优先读取 Redis 缓存）
     *
     * @param request 查询请求
     * @return 趋势图表数据
     */
    FundBalanceTrendResponse getBalanceTrend(FundBalanceTrendRequest request);

    /**
     * 重新计算资金余额变动趋势（清除缓存后强制重算）
     *
     * @param request 查询请求
     * @return 趋势图表数据（fromCache 固定为 false）
     */
    FundBalanceTrendResponse recalculate(FundBalanceTrendRequest request);

    /**
     * 清除所有资金余额趋势缓存（管理员操作）
     *
     * @return 操作结果
     */
    Map<String, Object> clearAllCache();
}
