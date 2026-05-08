package com.erp.controller.report;

import com.erp.common.result.Result;
import com.erp.controller.report.dto.EmployeePerformancePieRequest;
import com.erp.controller.report.dto.EmployeePerformancePieResponse;
import com.erp.service.report.EmployeePerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 员工业绩占比饼图 Controller
 *
 * 功能描述：
 * - 统计各在职员工在3个维度（合同签订数量/入库重量/结算金额）的业绩数据
 * - 支持按维度切换，返回饼状图所需数据
 */
@Slf4j
@Tag(name = "员工业绩占比饼图", description = "业务闭环监控 - 员工业绩占比饼状图相关接口")
@RestController
@RequestMapping("/report/employee-performance")
@RequiredArgsConstructor
public class EmployeePerformanceController {

    private final EmployeePerformanceService employeePerformanceService;

    /**
     * 获取员工业绩占比饼图数据
     *
     * 功能描述：根据维度统计每位在职员工的业绩数据及占比，支持按日期区间筛选
     * 入参：{ dimension, dateRange, startDate, endDate }
     * 返回参数：{ dimension, dimensionLabel, unit, total, list }
     * url地址：/api/report/employee-performance/pie
     * 请求方式：POST
     */
    @Operation(
            summary = "获取员工业绩占比饼图数据",
            description = "根据维度（合同签订数量/入库重量/结算金额）统计各在职员工业绩占比")
    @PostMapping("/pie")
    public Result<EmployeePerformancePieResponse> getPie(
            @RequestBody EmployeePerformancePieRequest request) {
        log.info("[EmployeePerformance] 收到请求 dimension={}, startDate={}, endDate={}",
                request.getDimension(), request.getStartDate(), request.getEndDate());
        EmployeePerformancePieResponse response = employeePerformanceService.getPie(request);
        return Result.success("获取员工业绩占比成功", response);
    }

    /**
     * 重新计算员工业绩占比（清缓存后强制重算）
     *
     * 功能描述：清除对应缓存键并强制重新计算
     * 入参：{ dimension, startDate, endDate }
     * 返回参数：同上
     * url地址：/api/report/employee-performance/pie/recalculate
     * 请求方式：POST
     */
    @Operation(
            summary = "重新计算员工业绩占比",
            description = "清除缓存后强制重新计算")
    @PostMapping("/pie/recalculate")
    public Result<EmployeePerformancePieResponse> recalculate(
            @RequestBody EmployeePerformancePieRequest request) {
        log.info("[EmployeePerformance] 重新计算 dimension={}, startDate={}, endDate={}",
                request.getDimension(), request.getStartDate(), request.getEndDate());
        EmployeePerformancePieResponse response = employeePerformanceService.recalculate(request);
        return Result.success("重新计算完成", response);
    }

    /**
     * 清除所有员工业绩占比缓存
     *
     * 功能描述：清除所有 employee-performance:pie:* 前缀的 Redis 缓存键
     * 入参：无
     * 返回参数：{ success, message, count }
     * url地址：/api/report/employee-performance/pie/cache/clear
     * 请求方式：POST
     */
    @Operation(
            summary = "清除所有员工业绩占比缓存",
            description = "清除所有 employee-performance:pie:* 前缀的 Redis 缓存键")
    @PostMapping("/pie/cache/clear")
    public Result<Map<String, Object>> clearCache() {
        Map<String, Object> result = employeePerformanceService.clearAllCache();
        return Result.success("清除成功", result);
    }
}
