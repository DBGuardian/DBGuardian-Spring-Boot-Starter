package com.erp.controller.dashboard;

import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.dashboard.dto.DashboardDataResponse;
import com.erp.service.dashboard.DashboardService;
import com.erp.common.annotation.RequirePagePermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台控制器
 *
 * @author ERP System
 * @date 2026-01-12
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@Api(tags = "工作台管理")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * 获取工作台数据
     * 功能描述：一次性获取工作台所有相关数据，包括统计指标、待办事项、消息通知等
     * 入参：无
     * 返回参数：DashboardDataResponse 包含用户信息、统计数据、待办事项、消息通知等
     * url地址：/api/dashboard
     * 请求方式：GET
     * 说明：工作台页面不需要权限验证，所有登录用户都可以访问
     */
    @GetMapping
    @ApiOperation(value = "获取工作台数据", notes = "一次性获取工作台所有相关数据")
    public Result<DashboardDataResponse> getDashboardData() {
        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return Result.error(401, "未登录");
            }
            DashboardDataResponse response = dashboardService.getDashboardData(userId);
            return Result.success("获取工作台数据成功", response);
        } catch (Exception e) {
            log.error("获取工作台数据失败", e);
            return Result.error(500, "获取工作台数据失败：" + e.getMessage());
        }
    }
}


