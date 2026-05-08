package com.erp.controller.report;

import com.erp.common.result.Result;
import com.erp.controller.report.dto.WasteLimitAnalysisRequest;
import com.erp.controller.report.dto.WasteLimitAnalysisResponse;
import com.erp.service.report.WasteLimitAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * 危险废物限额分析 Controller
 *
 * 功能描述：
 * - 查询每个废物类别的限额、合同签订量、实际收运量、已生成联单量
 * - 堆叠条形图展示三类指标的对比
 * - 汇总展示总限额使用率
 *
 * 时间模式说明：
 * - CURRENT_YEAR：当年模式，自定义日期范围，限额按全年计算
 * - PREVIOUS_YEAR：往年模式，按整年查询
 *
 * 数据来源：
 * - 限额：HAZARDOUS_WASTE_CATEGORY.限额
 * - 合同签订量：CONTRACT_WASTE_ITEM.计划转移数量（合同状态：已通过/执行中/已完结/已归档）
 * - 实际收运量：WAREHOUSING_WASTE_ITEM.实际收运数量
 * - 已生成联单量：TRANSFER_MANIFEST_ITEM.确认数量
 */
@Slf4j
@Tag(name = "危险废物限额分析", description = "业务闭环监控 - 危险废物限额分析堆叠条形图相关接口")
@RestController
@RequestMapping("/report/waste")
@RequiredArgsConstructor
public class WasteLimitAnalysisController {

    private final WasteLimitAnalysisService wasteLimitAnalysisService;

    /**
     * 获取危险废物限额分析
     *
     * 功能描述：查询各废物类别的限额、合同签订量、实际收运量、已生成联单量
     * 入参：{ dateRangeMode, startDate, endDate, year, wasteCategories }
     * 返回参数：{ startDate, endDate, fromCache, computedAt, categories, summary }
     * url地址：/api/report/waste/limit-analysis
     * 请求方式：POST
     */
    @Operation(
            summary = "获取危险废物限额分析",
            description = "查询各废物类别的限额、合同签订量、实际收运量、已生成联单量，用于堆叠条形图展示")
    @PostMapping("/limit-analysis")
    public Result<WasteLimitAnalysisResponse> getLimitAnalysis(
            @RequestBody WasteLimitAnalysisRequest request) {
        log.info("[WasteLimitAnalysis] 收到请求 dateRangeMode={}, startDate={}, endDate={}, year={}, categories={}",
                request.getDateRangeMode(), request.getStartDate(), request.getEndDate(),
                request.getYear(), Arrays.toString(request.getWasteCategories()));
        WasteLimitAnalysisResponse response = wasteLimitAnalysisService.getLimitAnalysis(request);
        return Result.success("获取危险废物限额分析成功", response);
    }

    /**
     * 重新计算危险废物限额分析（清除缓存后强制重算）
     *
     * 功能描述：清除对应缓存键并强制重新计算
     * 入参：{ dateRangeMode, startDate, endDate, year, wasteCategories }
     * 返回参数：同上
     * url地址：/api/report/waste/limit-analysis/recalculate
     * 请求方式：POST
     */
    @Operation(
            summary = "重新计算危险废物限额分析",
            description = "清除缓存后强制重新计算")
    @PostMapping("/limit-analysis/recalculate")
    public Result<WasteLimitAnalysisResponse> recalculate(
            @RequestBody WasteLimitAnalysisRequest request) {
        log.info("[WasteLimitAnalysis] 重新计算 dateRangeMode={}, startDate={}, endDate={}, year={}",
                request.getDateRangeMode(), request.getStartDate(), request.getEndDate(), request.getYear());
        WasteLimitAnalysisResponse response = wasteLimitAnalysisService.recalculate(request);
        return Result.success("重新计算完成", response);
    }

    /**
     * 清除所有危险废物限额分析缓存
     *
     * 功能描述：清除所有 waste:limit-analysis:* 前缀的 Redis 缓存键
     * 入参：无
     * 返回参数：{ success, message, count }
     * url地址：/api/report/waste/limit-analysis/cache/clear
     * 请求方式：POST
     */
    @Operation(
            summary = "清除所有危险废物限额分析缓存",
            description = "清除所有 waste:limit-analysis:* 前缀的 Redis 缓存键")
    @PostMapping("/limit-analysis/cache/clear")
    public Result<Map<String, Object>> clearCache() {
        Map<String, Object> result = wasteLimitAnalysisService.clearAllCache();
        return Result.success("清除成功", result);
    }
}
