package com.erp.controller.report;

import com.erp.common.result.Result;
import com.erp.controller.report.dto.ContractSignTrendRequest;
import com.erp.controller.report.dto.ContractSignTrendResponse;
import com.erp.service.report.ContractSignTrendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 合同签订数量变动趋势 Controller
 *
 * 功能描述：
 * - 获取各计价类型（总价包干/按量结算/混合计价）的合同签订数量随时间变动趋势图表数据
 * - 统计口径：CONTRACT.签订日期（非系统创建时间）
 * - 排除：合同状态为草稿、作废的合同
 * - 计价类型依据：CONTRACT → CONTRACT_ITEM.报价模式 判断 PACKAGE/UNIT/MIXED
 */
@Slf4j
@Tag(name = "合同签订数量变动趋势", description = "业务闭环监控 - 合同签订数量变动柱线混合图相关接口")
@RestController
@RequestMapping("/report/contract")
@RequiredArgsConstructor
public class ContractSignTrendController {

    private final ContractSignTrendService contractSignTrendService;

    /**
     * 获取合同签订数量变动趋势
     *
     * 功能描述：按时间粒度（day/month/year）统计各计价类型合同签订数量
     * 入参：{ startDate, endDate, granularity }
     * 返回参数：{ xAxis, series, summary, computedAt }
     * url地址：/api/report/contract/sign-trend
     * 请求方式：POST
     */
    @Operation(
            summary = "获取合同签订数量变动趋势",
            description = "按计价类型（总价包干/按量结算/混合计价）统计合同签订数量随时间变化趋势")
    @PostMapping("/sign-trend")
    public Result<ContractSignTrendResponse> getSignTrend(
            @RequestBody ContractSignTrendRequest request) {
        log.info("[ContractSignTrend] 收到请求 startDate={}, endDate={}, granularity={}",
                request.getStartDate(), request.getEndDate(), request.getGranularity());
        ContractSignTrendResponse response = contractSignTrendService.getSignTrend(request);
        return Result.success("获取合同签订数量变动趋势成功", response);
    }

    /**
     * 重新计算合同签订数量变动趋势（清除缓存后强制重算）
     *
     * 功能描述：清除对应缓存键并强制重新计算
     * 入参：{ startDate, endDate, granularity }
     * 返回参数：同上
     * url地址：/api/report/contract/sign-trend/recalculate
     * 请求方式：POST
     */
    @Operation(
            summary = "重新计算合同签订数量变动趋势",
            description = "清除缓存后强制重新计算")
    @PostMapping("/sign-trend/recalculate")
    public Result<ContractSignTrendResponse> recalculate(
            @RequestBody ContractSignTrendRequest request) {
        log.info("[ContractSignTrend] 重新计算 startDate={}, endDate={}, granularity={}",
                request.getStartDate(), request.getEndDate(), request.getGranularity());
        ContractSignTrendResponse response = contractSignTrendService.recalculate(request);
        return Result.success("重新计算完成", response);
    }

    /**
     * 清除所有合同签订趋势缓存
     *
     * 功能描述：清除所有 contract:sign-trend:* 前缀的 Redis 缓存键
     * 入参：无
     * 返回参数：{ success, message, count }
     * url地址：/api/report/contract/sign-trend/cache/clear
     * 请求方式：POST
     */
    @Operation(
            summary = "清除所有合同签订趋势缓存",
            description = "清除所有 contract:sign-trend:* 前缀的 Redis 缓存键")
    @PostMapping("/sign-trend/cache/clear")
    public Result<Map<String, Object>> clearCache() {
        Map<String, Object> result = contractSignTrendService.clearAllCache();
        return Result.success("清除成功", result);
    }
}
