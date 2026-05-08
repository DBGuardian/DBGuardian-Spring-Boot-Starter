package com.erp.service.report.impl;

import com.erp.controller.report.dto.ContractSignTrendRequest;
import com.erp.controller.report.dto.ContractSignTrendResponse;
import com.erp.controller.report.dto.ContractSignTrendResponse.ContractSignCountDTO;
import com.erp.controller.report.dto.ContractSignTrendResponse.ContractSignSeries;
import com.erp.controller.report.dto.ContractSignTrendResponse.ContractSignSummary;
import com.erp.mapper.report.ContractSignTrendMapper;
import com.erp.service.report.ContractSignTrendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 合同签订数量变动趋势 ServiceImpl
 *
 * 计算逻辑：
 * 1. 生成 [startDate, endDate] 的时间粒度序列（xAxis）
 * 2. 查询区间内各粒度各计价类型的签订合同数量（按 CONTRACT.签订日期 统计）
 * 3. 补零：xAxis 中无数据的节点补 0
 * 4. 计价类型：PACKAGE（总价包干）/ UNIT（按量结算）/ MIXED（混合计价）
 * 5. series 顺序固定：PACKAGE → UNIT → MIXED
 *
 * 数据来源：
 * - 签订口径：CONTRACT.签订日期
 * - 排除：合同状态 IN ('草稿', '作废') 或 签订日期 IS NULL
 * - 计价类型：CONTRACT_ITEM.报价模式（总价包干 → PACKAGE，按量结算 → UNIT，两者都有 → MIXED）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractSignTrendServiceImpl implements ContractSignTrendService {

    private static final String CACHE_KEY_PREFIX = "contract:sign-trend";
    private static final long CACHE_TTL_SECONDS = 604800L;   // 7 天
    private static final long COMPUTING_TTL_SECONDS = 300L;  // 5 分钟

    // series 固定顺序
    private static final List<String> PRICING_TYPES = Arrays.asList("PACKAGE", "UNIT", "MIXED", "TOTAL");
    private static final Map<String, String> PRICING_NAMES;
    static {
        PRICING_NAMES = new LinkedHashMap<>();
        PRICING_NAMES.put("PACKAGE", "总价包干");
        PRICING_NAMES.put("UNIT",    "按量结算");
        PRICING_NAMES.put("MIXED",   "混合计价");
        PRICING_NAMES.put("TOTAL",   "总数量");
    }

    private final ContractSignTrendMapper mapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // ─────────────────────────── 接口实现 ───────────────────────────

    @Override
    public ContractSignTrendResponse getSignTrend(ContractSignTrendRequest request) {
        normalizeRequest(request);
        String cacheKey = generateCacheKey(request);

        // 尝试读缓存
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ContractSignTrendResponse) {
            log.info("[ContractSignTrend] 缓存命中 key={}", cacheKey);
            ContractSignTrendResponse resp = (ContractSignTrendResponse) cached;
            resp.setFromCache(true);
            return resp;
        }

        // 防并发
        String computingKey = cacheKey + ":computing";
        Object isComputing = redisTemplate.opsForValue().get(computingKey);
        if (Boolean.TRUE.equals(isComputing)) {
            throw new RuntimeException("数据正在计算中，请稍候...");
        }

        redisTemplate.opsForValue().set(computingKey, true, Duration.ofSeconds(COMPUTING_TTL_SECONDS));
        try {
            ContractSignTrendResponse data = calculate(request);
            data.setFromCache(false);
            redisTemplate.opsForValue().set(cacheKey, data, Duration.ofSeconds(CACHE_TTL_SECONDS));
            log.info("[ContractSignTrend] 计算完成并缓存 key={}", cacheKey);
            return data;
        } finally {
            redisTemplate.delete(computingKey);
        }
    }

    @Override
    public ContractSignTrendResponse recalculate(ContractSignTrendRequest request) {
        normalizeRequest(request);
        String cacheKey = generateCacheKey(request);
        redisTemplate.delete(cacheKey);
        redisTemplate.delete(cacheKey + ":computing");
        log.info("[ContractSignTrend] 缓存已清除，重新计算 key={}", cacheKey);
        ContractSignTrendResponse resp = getSignTrend(request);
        resp.setFromCache(false);
        return resp;
    }

    @Override
    public Map<String, Object> clearAllCache() {
        Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + ":*");
        int count = 0;
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            count = keys.size();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "所有合同签订趋势缓存已清除");
        result.put("count", count);
        return result;
    }

    // ─────────────────────────── 核心计算 ───────────────────────────

    private ContractSignTrendResponse calculate(ContractSignTrendRequest request) {
        String startDate = request.getStartDate();
        String endDate   = request.getEndDate();
        String gran      = request.getGranularity();

        // Step 1: 生成 xAxis 时间粒度序列
        List<String> xAxis = generateXAxis(startDate, endDate, gran);
        if (xAxis.isEmpty()) {
            return buildEmptyResponse(request);
        }

        // Step 2: 查询区间内各粒度各计价类型的签订数量
        List<ContractSignCountDTO> rawData = mapper.selectSignCountByPeriod(startDate, endDate, gran);

        // Step 3: 转成 Map: dateLabel|pricingType -> count
        Map<String, Integer> countMap = new HashMap<>();
        for (ContractSignCountDTO dto : rawData) {
            if (dto.getDateLabel() == null || dto.getPricingType() == null) continue;
            String key = dto.getDateLabel() + "|" + dto.getPricingType();
            countMap.merge(key, dto.getCount(), Integer::sum);
        }

        // Step 4: 按计价类型构建 series data（xAxis 无数据节点补 0）
        Map<String, List<Integer>> seriesDataMap = new LinkedHashMap<>();
        for (String pt : Arrays.asList("PACKAGE", "UNIT", "MIXED")) {
            List<Integer> data = new ArrayList<>();
            for (String label : xAxis) {
                data.add(countMap.getOrDefault(label + "|" + pt, 0));
            }
            seriesDataMap.put(pt, data);
        }

        List<Integer> totalSeriesData = new ArrayList<>();
        for (int i = 0; i < xAxis.size(); i++) {
            int total = seriesDataMap.get("PACKAGE").get(i)
                    + seriesDataMap.get("UNIT").get(i)
                    + seriesDataMap.get("MIXED").get(i);
            totalSeriesData.add(total);
        }
        seriesDataMap.put("TOTAL", totalSeriesData);

        // Step 5: 构建 series 列表（固定顺序 PACKAGE / UNIT / MIXED / TOTAL）
        List<ContractSignSeries> series = new ArrayList<>();
        for (String pt : PRICING_TYPES) {
            series.add(new ContractSignSeries(
                    PRICING_NAMES.get(pt),
                    pt,
                    seriesDataMap.get(pt)
            ));
        }

        // Step 6: 计算 summary
        int packageTotal = sum(seriesDataMap.get("PACKAGE"));
        int unitTotal    = sum(seriesDataMap.get("UNIT"));
        int mixedTotal   = sum(seriesDataMap.get("MIXED"));
        int totalCount   = packageTotal + unitTotal + mixedTotal;

        int lastPackage  = last(seriesDataMap.get("PACKAGE"));
        int lastUnit     = last(seriesDataMap.get("UNIT"));
        int lastMixed    = last(seriesDataMap.get("MIXED"));
        int lastTotal    = lastPackage + lastUnit + lastMixed;

        ContractSignSummary summary = new ContractSignSummary(
                totalCount, packageTotal, unitTotal, mixedTotal,
                lastTotal, lastPackage, lastUnit, lastMixed
        );

        // Step 7: 组装响应
        String computedAt = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        ContractSignTrendResponse resp = new ContractSignTrendResponse();
        resp.setStartDate(startDate);
        resp.setEndDate(endDate);
        resp.setGranularity(gran);
        resp.setFromCache(false);
        resp.setComputedAt(computedAt);
        resp.setXAxis(xAxis);
        resp.setSeries(series);
        resp.setSummary(summary);
        return resp;
    }

    // ─────────────────────────── 工具方法 ───────────────────────────

    /**
     * 生成 xAxis 时间粒度序列
     */
    private List<String> generateXAxis(String startDate, String endDate, String gran) {
        List<String> xAxis = new ArrayList<>();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end   = LocalDate.parse(endDate);
        switch (gran) {
            case "day":
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    xAxis.add(d.format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
                break;
            case "year":
                for (int y = start.getYear(); y <= end.getYear(); y++) {
                    xAxis.add(String.valueOf(y));
                }
                break;
            default: // month
                LocalDate cursor = start.withDayOfMonth(1);
                while (!cursor.isAfter(end)) {
                    xAxis.add(cursor.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                    cursor = cursor.plusMonths(1);
                }
        }
        return xAxis;
    }

    private int sum(List<Integer> list) {
        if (list == null) return 0;
        return list.stream().mapToInt(Integer::intValue).sum();
    }

    private int last(List<Integer> list) {
        if (list == null || list.isEmpty()) return 0;
        return list.get(list.size() - 1);
    }

    private void normalizeRequest(ContractSignTrendRequest request) {
        LocalDate today = LocalDate.now();
        String gran = request.getGranularity() == null ? "month" : request.getGranularity();
        request.setGranularity(gran);
        if (request.getStartDate() == null) {
            LocalDate start;
            switch (gran) {
                case "day":  start = today.withDayOfMonth(1); break;
                case "year": start = LocalDate.of(today.getYear() - 4, 1, 1); break;
                default:     start = LocalDate.of(today.getYear(), 1, 1);
            }
            request.setStartDate(start.toString());
        }
        if (request.getEndDate() == null) {
            request.setEndDate(today.toString());
        }
    }

    private String generateCacheKey(ContractSignTrendRequest request) {
        return String.format("%s:%s:%s:%s",
                CACHE_KEY_PREFIX, request.getGranularity(),
                request.getStartDate(), request.getEndDate());
    }

    private ContractSignTrendResponse buildEmptyResponse(ContractSignTrendRequest request) {
        List<ContractSignSeries> series = new ArrayList<>();
        for (String pt : PRICING_TYPES) {
            series.add(new ContractSignSeries(PRICING_NAMES.get(pt), pt, Collections.emptyList()));
        }
        ContractSignTrendResponse resp = new ContractSignTrendResponse();
        resp.setStartDate(request.getStartDate());
        resp.setEndDate(request.getEndDate());
        resp.setGranularity(request.getGranularity());
        resp.setFromCache(false);
        resp.setComputedAt(java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        resp.setXAxis(Collections.emptyList());
        resp.setSeries(series);
        resp.setSummary(new ContractSignSummary(0, 0, 0, 0, 0, 0, 0, 0));
        return resp;
    }
}
