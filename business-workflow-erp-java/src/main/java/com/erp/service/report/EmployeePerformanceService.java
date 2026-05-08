package com.erp.service.report;

import com.erp.controller.report.dto.EmployeePerformancePieRequest;
import com.erp.controller.report.dto.EmployeePerformancePieResponse;

import java.util.Map;

/**
 * 员工业绩占比饼图 Service
 */
public interface EmployeePerformanceService {

    /**
     * 获取员工业绩占比饼图数据（优先读缓存）
     *
     * @param request 请求参数
     * @return 饼图数据
     */
    EmployeePerformancePieResponse getPie(EmployeePerformancePieRequest request);

    /**
     * 重新计算员工业绩占比（清缓存后强制重算）
     *
     * @param request 请求参数
     * @return 饼图数据
     */
    EmployeePerformancePieResponse recalculate(EmployeePerformancePieRequest request);

    /**
     * 清除所有员工业绩占比缓存
     *
     * @return 清除结果 { success, message, count }
     */
    Map<String, Object> clearAllCache();
}
