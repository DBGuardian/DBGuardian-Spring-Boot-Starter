package com.erp.service.report;

import com.erp.controller.report.dto.ContractSignTrendRequest;
import com.erp.controller.report.dto.ContractSignTrendResponse;

import java.util.Map;

/**
 * 合同签订数量变动趋势 Service
 */
public interface ContractSignTrendService {

    /**
     * 获取合同签订数量变动趋势（优先读缓存）
     *
     * @param request 请求参数
     * @return 趋势图表数据
     */
    ContractSignTrendResponse getSignTrend(ContractSignTrendRequest request);

    /**
     * 重新计算合同签订数量变动趋势（清缓存后强制重算）
     *
     * @param request 请求参数
     * @return 趋势图表数据
     */
    ContractSignTrendResponse recalculate(ContractSignTrendRequest request);

    /**
     * 清除所有合同签订趋势缓存
     *
     * @return 清除结果 { success, message, count }
     */
    Map<String, Object> clearAllCache();
}
