package com.erp.service.report;

import com.erp.controller.report.dto.WarehouseInOutTrendRequest;
import com.erp.controller.report.dto.WarehouseInOutTrendResponse;

import java.util.Map;

/**
 * 仓库出入库数量变动趋势 Service
 */
public interface WarehouseInOutTrendService {

    /**
     * 获取仓库出入库数量变动趋势（优先读缓存）
     *
     * @param request 请求参数
     * @return 趋势图表数据
     */
    WarehouseInOutTrendResponse getTrend(WarehouseInOutTrendRequest request);

    /**
     * 重新计算仓库出入库数量变动趋势（清缓存后强制重算）
     *
     * @param request 请求参数
     * @return 趋势图表数据
     */
    WarehouseInOutTrendResponse recalculate(WarehouseInOutTrendRequest request);

    /**
     * 清除所有仓库出入库趋势缓存
     *
     * @return 清除结果 { success, message, count }
     */
    Map<String, Object> clearAllCache();
}
