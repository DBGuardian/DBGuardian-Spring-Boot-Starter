package com.erp.service.report;

import com.erp.controller.report.dto.WasteLimitAnalysisRequest;
import com.erp.controller.report.dto.WasteLimitAnalysisResponse;

import java.util.Map;

/**
 * 危险废物限额分析 Service
 *
 * 功能描述：
 * - 查询每个废物类别的限额、合同签订量、实际收运量、联单确认量
 * - 汇总统计总限额使用率
 * - 支持当年模式（自定义日期范围）和往年模式（按年份）
 */
public interface WasteLimitAnalysisService {

    /**
     * 获取危险废物限额分析数据（优先读缓存）
     *
     * @param request 请求参数
     * @return 限额分析图表数据
     */
    WasteLimitAnalysisResponse getLimitAnalysis(WasteLimitAnalysisRequest request);

    /**
     * 重新计算危险废物限额分析（清缓存后强制重算）
     *
     * @param request 请求参数
     * @return 限额分析图表数据
     */
    WasteLimitAnalysisResponse recalculate(WasteLimitAnalysisRequest request);

    /**
     * 清除所有危险废物限额分析缓存
     *
     * @return 清除结果 { success, message, count }
     */
    Map<String, Object> clearAllCache();
}
