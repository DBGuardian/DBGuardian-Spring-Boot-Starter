package com.erp.controller.report;

import com.erp.common.result.Result;
import com.erp.controller.report.dto.ReceivableBalanceTrendRequest;
import com.erp.controller.report.dto.ReceivableBalanceTrendResponse;
import com.erp.service.report.ReceivableBalanceTrendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 应收账款余额变动趋势 Controller
 *
 * 功能描述：
 * - 获取各计价类型（总价包干/按量结算/混合计价/总金额）的应收账款余额变动趋势图表数据
 * - 应收增加来源：RECEIVABLE 类型结算单创建（以创建时间计）
 * - 应收减少来源：SETTLEMENT_FUND_TRANSACTION_REL 关联资金流水（以关联时间计）
 * - 计价类型依据：结算单关联合同 → CONTRACT_ITEM.报价模式 判断 PACKAGE/UNIT/MIXED
 * - 余额为历史累计净值：截至该时间点所有结算金额 - 所有已收款金额
 */
@Slf4j
@Tag(name = "应收账款余额变动趋势", description = "业务闭环监控 - 应收账款余额变动折线图相关接口")
@RestController
@RequestMapping("/report/receivable")
@RequiredArgsConstructor
public class ReceivableBalanceTrendController {

    private final ReceivableBalanceTrendService receivableBalanceTrendService;

    /**
     * 获取应收账款余额变动趋势
     *
     * 功能描述：按时间粒度（day/month/year）统计各计价类型应收账款余额累计净变动
     * 入参：{ startDate, endDate, granularity }
     * 返回参数：{ xaxis, series, summary, computedAt }
     * url地址：/api/report/receivable/balance-trend
     * 请求方式：POST
     */
    @Operation(
            summary = "获取应收账款余额变动趋势",
            description = "按计价类型（总价包干/按量结算/混合计价/总金额）统计应收账款余额随时间变化趋势，余额为历史累计净值")
    @PostMapping("/balance-trend")
    public Result<ReceivableBalanceTrendResponse> getBalanceTrend(
            @RequestBody ReceivableBalanceTrendRequest request) {
        log.info("[ReceivableBalanceTrend] 收到请求 startDate={}, endDate={}, granularity={}",
                request.getStartDate(), request.getEndDate(), request.getGranularity());
        ReceivableBalanceTrendResponse response = receivableBalanceTrendService.getBalanceTrend(request);
        return Result.success("获取应收账款余额变动趋势成功", response);
    }

    /**
     * 重新计算应收账款余额变动趋势（清除缓存后强制重算）
     *
     * 功能描述：清除对应缓存键并强制重新计算
     * 入参：{ startDate, endDate, granularity }
     * 返回参数：同上
     * url地址：/api/report/receivable/balance-trend/recalculate
     * 请求方式：POST
     */
    @Operation(
            summary = "重新计算应收账款余额变动趋势",
            description = "清除缓存后强制重新计算，对应看板「执行全量校验」按钮")
    @PostMapping("/balance-trend/recalculate")
    public Result<ReceivableBalanceTrendResponse> recalculate(
            @RequestBody ReceivableBalanceTrendRequest request) {
        log.info("[ReceivableBalanceTrend] 重新计算 startDate={}, endDate={}, granularity={}",
                request.getStartDate(), request.getEndDate(), request.getGranularity());
        ReceivableBalanceTrendResponse response = receivableBalanceTrendService.recalculate(request);
        return Result.success("重新计算完成", response);
    }

    /**
     * 清除所有应收账款余额趋势缓存
     *
     * 功能描述：清除所有 receivable:balance-trend:* 前缀的 Redis 缓存键
     * 入参：无
     * 返回参数：{ success, message, count }
     * url地址：/api/report/receivable/balance-trend/cache/clear
     * 请求方式：POST
     */
    @Operation(
            summary = "清除所有应收账款余额趋势缓存",
            description = "管理员操作，清除所有 receivable:balance-trend:* 前缀的 Redis 缓存键")
    @PostMapping("/balance-trend/cache/clear")
    public Result<Map<String, Object>> clearCache() {
        Map<String, Object> result = receivableBalanceTrendService.clearAllCache();
        return Result.success("清除成功", result);
    }
}
