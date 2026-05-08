package com.erp.controller.report;

import com.erp.common.result.Result;
import com.erp.controller.report.dto.WarehouseInOutTrendRequest;
import com.erp.controller.report.dto.WarehouseInOutTrendResponse;
import com.erp.service.report.WarehouseInOutTrendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 仓库出入库数量变动趋势 Controller
 *
 * 功能描述：
 * - 按时间粒度（day/month/year）聚合入库、出库危废重量（单位：吨）
 * - 数据来源：入库使用 WAREHOUSING_WASTE_ITEM.实际收运数量，出库使用 OUTBOUND_WASTE_DETAIL.出库量
 * - 前端展示采用柱状图 + 柱顶连线，顶部摘要展示区间总出入库量
 */
@Slf4j
@Tag(name = "仓库出入库数量变动趋势", description = "业务闭环监控 - 仓库出入库数量变动柱线混合图相关接口")
@RestController
@RequestMapping("/report/warehouse")
@RequiredArgsConstructor
public class WarehouseInOutTrendController {

    private final WarehouseInOutTrendService warehouseInOutTrendService;

    /**
     * 获取仓库出入库数量变动趋势
     *
     * 功能描述：按时间粒度聚合入库、出库危废重量，返回柱线混合图数据
     * 入参：{ startDate, endDate, granularity }
     * 返回参数：{ xAxis, series, summary, computedAt, fromCache }
     * url地址：/api/report/warehouse/in-out-trend
     * 请求方式：POST
     */
    @Operation(
            summary = "获取仓库出入库数量变动趋势",
            description = "按时间粒度（day/month/year）统计入库/出库危废重量随时间变化趋势")
    @PostMapping("/in-out-trend")
    public Result<WarehouseInOutTrendResponse> getTrend(
            @RequestBody WarehouseInOutTrendRequest request) {
        log.info("[WarehouseInOutTrend] 收到请求 startDate={}, endDate={}, granularity={}",
                request.getStartDate(), request.getEndDate(), request.getGranularity());
        WarehouseInOutTrendResponse response = warehouseInOutTrendService.getTrend(request);
        return Result.success("获取仓库出入库数量变动趋势成功", response);
    }

    /**
     * 重新计算仓库出入库数量变动趋势（清除缓存后强制重算）
     *
     * 功能描述：清除对应缓存键并强制重新计算
     * 入参：{ startDate, endDate, granularity }
     * 返回参数：同上
     * url地址：/api/report/warehouse/in-out-trend/recalculate
     * 请求方式：POST
     */
    @Operation(
            summary = "重新计算仓库出入库数量变动趋势",
            description = "清除缓存后强制重新计算")
    @PostMapping("/in-out-trend/recalculate")
    public Result<WarehouseInOutTrendResponse> recalculate(
            @RequestBody WarehouseInOutTrendRequest request) {
        log.info("[WarehouseInOutTrend] 重新计算 startDate={}, endDate={}, granularity={}",
                request.getStartDate(), request.getEndDate(), request.getGranularity());
        WarehouseInOutTrendResponse response = warehouseInOutTrendService.recalculate(request);
        return Result.success("重新计算完成", response);
    }

    /**
     * 清除所有仓库出入库趋势缓存
     *
     * 功能描述：清除所有 warehouse:in-out-trend:* 前缀的 Redis 缓存键
     * 入参：无
     * 返回参数：{ success, message, count }
     * url地址：/api/report/warehouse/in-out-trend/cache/clear
     * 请求方式：POST
     */
    @Operation(
            summary = "清除所有仓库出入库趋势缓存",
            description = "清除所有 warehouse:in-out-trend:* 前缀的 Redis 缓存键")
    @PostMapping("/in-out-trend/cache/clear")
    public Result<Map<String, Object>> clearCache() {
        Map<String, Object> result = warehouseInOutTrendService.clearAllCache();
        return Result.success("清除成功", result);
    }
}
