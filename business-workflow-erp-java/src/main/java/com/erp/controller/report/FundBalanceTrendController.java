package com.erp.controller.report;

import com.erp.common.result.Result;
import com.erp.controller.report.dto.FundBalanceTrendRequest;
import com.erp.controller.report.dto.FundBalanceTrendResponse;
import com.erp.service.report.FundBalanceTrendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 资金余额变动趋势 Controller
 *
 * 功能描述：
 * - 获取资金余额变动趋势图表数据（支持 Redis 缓存，有效期 7 天）
 * - 重新计算（清除缓存后强制重算）
 * - 清除所有资金余额趋势缓存
 */
@Slf4j
@Tag(name = "资金余额变动趋势", description = "管理看板 - 资金余额变动折线图相关接口")
@RestController
@RequestMapping("/report/fund")
@RequiredArgsConstructor
public class FundBalanceTrendController {

    private final FundBalanceTrendService fundBalanceTrendService;

    /**
     * 获取资金余额变动趋势（优先读取缓存）
     */
    @Operation(
            summary = "获取资金余额变动趋势",
            description = "优先从 Redis 缓存返回数据；缓存不存在时后端计算并缓存，有效期 7 天")
    @PostMapping("/balance-trend")
    public Result<FundBalanceTrendResponse> getBalanceTrend(
            @RequestBody FundBalanceTrendRequest request) {
        log.info("[FundBalanceTrend] 收到请求 dateRange={}, startDate={}, endDate={}",
                request.getDateRange(), request.getStartDate(), request.getEndDate());
        FundBalanceTrendResponse response = fundBalanceTrendService.getBalanceTrend(request);
        log.info("[FundBalanceTrend] 返回 xAxis前3个={}",
                response.getXAxis() != null && !response.getXAxis().isEmpty()
                        ? response.getXAxis().subList(0, Math.min(3, response.getXAxis().size()))
                        : "empty");
        return Result.success("获取资金余额变动趋势成功", response);
    }

    /**
     * 重新计算资金余额变动趋势（清除缓存后强制重算）
     */
    @Operation(
            summary = "重新计算资金余额变动趋势",
            description = "清除对应缓存键并强制重新计算，对应管理看板「执行全量校验」按钮")
    @PostMapping("/balance-trend/recalculate")
    public Result<FundBalanceTrendResponse> recalculate(
            @RequestBody FundBalanceTrendRequest request) {
        FundBalanceTrendResponse response = fundBalanceTrendService.recalculate(request);
        return Result.success("重新计算完成", response);
    }

    /**
     * 清除所有资金余额趋势缓存
     */
    @Operation(
            summary = "清除所有资金余额趋势缓存",
            description = "管理员操作，清除所有 fund:balance-trend:* 前缀的 Redis 缓存键")
    @PostMapping("/balance-trend/cache/clear")
    public Result<Map<String, Object>> clearCache() {
        Map<String, Object> result = fundBalanceTrendService.clearAllCache();
        return Result.success("清除成功", result);
    }
}
