package com.erp.service.dashboard;

import com.erp.controller.dashboard.dto.DashboardDataResponse;

/**
 * 工作台服务接口
 *
 * @author ERP System
 * @date 2026-01-12
 */
public interface DashboardService {

    /**
     * 获取工作台数据
     *
     * @param userId 用户ID
     * @return 工作台数据
     */
    DashboardDataResponse getDashboardData(Integer userId);
}


