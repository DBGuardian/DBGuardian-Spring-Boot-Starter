package com.erp.service.report.impl;

import com.erp.controller.report.dto.ReceivableBalanceTrendRequest;
import com.erp.controller.report.dto.ReceivableBalanceTrendResponse;
import com.erp.controller.report.dto.ReceivableBalanceTrendResponse.ReceivableSeries;
import com.erp.controller.report.dto.ReceivableBalanceTrendResponse.ReceivableSummary;
import com.erp.controller.report.dto.ReceivableBalanceTrendResponse.ReceivableChangeDTO;
import com.erp.mapper.report.ReceivableBalanceTrendMapper;
import com.erp.service.report.ReceivableBalanceTrendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应收账款余额变动趋势 ServiceImpl
 *
 * 计算逻辑：
 * 1. 以 startDate 之前（不含）所有数据累计作为起始余额
 * 2. 生成 [startDate, endDate] 的时间粒度序列（xAxis）
 * 3. 对每个时间粒度点：累计净余额 = 历史起点余额 + 区间内各期累计增减
 * 4. 计价类型：PACKAGE（总价包干）/ UNIT（按量结算）/ MIXED（混合计价）
 * 5. TOTAL = PACKAGE + UNIT + MIXED 各粒度点之和
 *
 * 数据来源：
 * - 增加：SETTLEMENT（结算类型=RECEIVABLE）.结算金额，按创建时间聚合
 * - 减少：SETTLEMENT_INVOICE_REL.关联金额，按关联时间聚合，排除 status=CANCELLED
 * - 计价类型：SETTLEMENT.合同号 → CONTRACT → CONTRACT_ITEM.报价模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableBalanceTrendServiceImpl implements ReceivableBalanceTrendService {

    private static final String CACHE_KEY_PREFIX = "receivable:balance-trend";
    private static final long CACHE_TTL_SECONDS = 604800L;   // 7 天
    private static final long COMPUTING_TTL_SECONDS = 300L;  // 5 分钟

    // 支持的计价类型（不含 TOTAL，TOTAL 由前三类合计得出）
    private static final List<String> PRICING_TYPES = Arrays.asList("PACKAGE", "UNIT", "MIXED");

    private final ReceivableBalanceTrendMapper mapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // ─────────────────────────── 接口实现 ───────────────────────────

    @Override
    public ReceivableBalanceTrendResponse getBalanceTrend(ReceivableBalanceTrendRequest request) {
        normalizeRequest(request);
        String cacheKey = generateCacheKey(request);

        // 尝试读缓存
        ReceivableBalanceTrendResponse cached =
                (ReceivableBalanceTrendResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            cached.setFromCache(true);
            log.info("[ReceivableBalanceTrend] 缓存命中 key={}", cacheKey);
            return cached;
        }

        // 防并发
        String computingKey = cacheKey + ":computing";
        Boolean isComputing = (Boolean) redisTemplate.opsForValue().get(computingKey);
        if (Boolean.TRUE.equals(isComputing)) {
            throw new RuntimeException("数据正在计算中，请稍候...");
        }

        redisTemplate.delete(cacheKey);
        redisTemplate.opsForValue().set(computingKey, true, Duration.ofSeconds(COMPUTING_TTL_SECONDS));
        try {
            ReceivableBalanceTrendResponse data = calculate(request);
            data.setFromCache(false);
            redisTemplate.opsForValue().set(cacheKey, data, Duration.ofSeconds(CACHE_TTL_SECONDS));
            log.info("[ReceivableBalanceTrend] 计算完成并缓存 key={}", cacheKey);
            return data;
        } finally {
            redisTemplate.delete(computingKey);
        }
    }

    @Override
    public ReceivableBalanceTrendResponse recalculate(ReceivableBalanceTrendRequest request) {
        normalizeRequest(request);
        String cacheKey = generateCacheKey(request);
        redisTemplate.delete(cacheKey);
        redisTemplate.delete(cacheKey + ":computing");
        log.info("[ReceivableBalanceTrend] 缓存已清除，重新计算 key={}", cacheKey);
        return getBalanceTrend(request);
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
        result.put("message", "所有应收账款余额趋势缓存已清除");
        result.put("count", count);
        return result;
    }

    // ─────────────────────────── 核心计算 ───────────────────────────

    private ReceivableBalanceTrendResponse calculate(ReceivableBalanceTrendRequest request) {
        String startDate = request.getStartDate();
        String endDate   = request.getEndDate();
        String gran      = request.getGranularity();

        // Step 1: 计算历史起点余额（startDate 之前所有数据累计净值，按计价类型）
        // 历史起点 = startDate 前一天
        String historyEnd = LocalDate.parse(startDate).minusDays(1).toString();
        Map<String, BigDecimal> historyBalance = buildHistoryBalance(historyEnd);

        // Step 2: 生成 xAxis 时间粒度序列
        List<String> xAxis = generateXAxis(startDate, endDate, gran);
        if (xAxis.isEmpty()) {
            return buildEmptyResponse(request);
        }

        // Step 3: 查询区间内各粒度各计价类型的增减变动
        List<ReceivableChangeDTO> settlements = mapper.selectSettlementByPeriod(startDate, endDate, gran);
        List<ReceivableChangeDTO> received    = mapper.selectReceivedByPeriod(startDate, endDate, gran);

        // 转成 Map: dateLabel+pricingType -> 金额
        Map<String, BigDecimal> settlementMap = toMap(settlements, true);
        Map<String, BigDecimal> receivedMap   = toMap(received, false);

        // Step 4: 按计价类型逐粒度累计计算余额序列
        // key: pricingType, value: 各 xAxis 点的累计余额
        Map<String, List<BigDecimal>> seriesDataMap = new LinkedHashMap<>();
        // 当前累计余额（从历史起点开始，逐期累积区间内变动）
        Map<String, BigDecimal> runningBalance = new HashMap<>(historyBalance);

        for (String pt : PRICING_TYPES) {
            seriesDataMap.put(pt, new ArrayList<>());
        }

        for (String label : xAxis) {
            for (String pt : PRICING_TYPES) {
                BigDecimal addAmt = settlementMap.getOrDefault(label + "|" + pt, BigDecimal.ZERO);
                BigDecimal subAmt = receivedMap.getOrDefault(label + "|" + pt, BigDecimal.ZERO);
                BigDecimal cur = runningBalance.getOrDefault(pt, BigDecimal.ZERO)
                        .add(addAmt).subtract(subAmt);
                cur = cur.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : cur;
                runningBalance.put(pt, cur);
                seriesDataMap.get(pt).add(cur);
            }
        }

        // Step 5: 构建 TOTAL 序列（各粒度点三类之和）
        List<BigDecimal> totalData = new ArrayList<>();
        for (int i = 0; i < xAxis.size(); i++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (String pt : PRICING_TYPES) {
                List<BigDecimal> d = seriesDataMap.get(pt);
                if (d != null && i < d.size()) sum = sum.add(d.get(i));
            }
            totalData.add(sum);
        }

        // Step 6: 当前余额（最后一个粒度点）
        BigDecimal lastPackage = last(seriesDataMap.get("PACKAGE"));
        BigDecimal lastUnit    = last(seriesDataMap.get("UNIT"));
        BigDecimal lastMixed   = last(seriesDataMap.get("MIXED"));
        BigDecimal lastTotal   = last(totalData);

        // Step 7: 组装 series（TOTAL 在首位）
        List<ReceivableSeries> series = new ArrayList<>();
        series.add(new ReceivableSeries("总金额",   "TOTAL",   totalData,                    lastTotal));
        series.add(new ReceivableSeries("总价包干", "PACKAGE", seriesDataMap.get("PACKAGE"), lastPackage));
        series.add(new ReceivableSeries("按量结算", "UNIT",    seriesDataMap.get("UNIT"),    lastUnit));
        series.add(new ReceivableSeries("混合计价", "MIXED",   seriesDataMap.get("MIXED"),   lastMixed));

        // Step 8: 组装响应
        String computedAt = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        ReceivableBalanceTrendResponse resp = new ReceivableBalanceTrendResponse();
        resp.setStartDate(startDate);
        resp.setEndDate(endDate);
        resp.setGranularity(gran);
        resp.setComputedAt(computedAt);
        resp.setXaxis(xAxis);
        resp.setSeries(series);
        resp.setSummary(new ReceivableSummary(lastTotal, lastPackage, lastUnit, lastMixed));
        return resp;
    }

    /**
     * 计算 endDate（含）之前的历史累计净余额，按计价类型分组
     * 历史余额 = 累计结算金额 - 累计已收款金额
     */
    private Map<String, BigDecimal> buildHistoryBalance(String endDate) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (String pt : PRICING_TYPES) result.put(pt, BigDecimal.ZERO);

        // 查询截至 endDate 的历史累计结算金额
        List<ReceivableChangeDTO> histSettlements = mapper.selectCumulativeSettlement(endDate);
        for (ReceivableChangeDTO dto : histSettlements) {
            if (dto.getPricingType() != null && dto.getSettlementAmount() != null) {
                result.merge(dto.getPricingType(), dto.getSettlementAmount(), BigDecimal::add);
            }
        }

        // 查询截至 endDate 的历史累计已收款金额
        List<ReceivableChangeDTO> histReceived = mapper.selectCumulativeReceived(endDate);
        for (ReceivableChangeDTO dto : histReceived) {
            if (dto.getPricingType() != null && dto.getReceivedAmount() != null) {
                result.merge(dto.getPricingType(), dto.getReceivedAmount().negate(), BigDecimal::add);
            }
        }

        // 确保非负
        result.replaceAll((k, v) -> v.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : v);
        return result;
    }

    /**
     * 将 ReceivableChangeDTO 列表转为 Map，key = dateLabel|pricingType
     * isSettlement=true 取 settlementAmount，false 取 receivedAmount
     */
    private Map<String, BigDecimal> toMap(List<ReceivableChangeDTO> list, boolean isSettlement) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (ReceivableChangeDTO dto : list) {
            if (dto.getDateLabel() == null || dto.getPricingType() == null) continue;
            BigDecimal amt = isSettlement ? dto.getSettlementAmount() : dto.getReceivedAmount();
            if (amt == null) amt = BigDecimal.ZERO;
            map.merge(dto.getDateLabel() + "|" + dto.getPricingType(), amt, BigDecimal::add);
        }
        return map;
    }

    /**
     * 生成 xAxis 时间粒度序列
     * day:   YYYY-MM-DD
     * month: YYYY-MM
     * year:  YYYY
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

    private BigDecimal last(List<BigDecimal> list) {
        if (list == null || list.isEmpty()) return BigDecimal.ZERO;
        return list.get(list.size() - 1);
    }

    private void normalizeRequest(ReceivableBalanceTrendRequest request) {
        if (request.getGranularity() == null) {
            request.setGranularity("month");
        }
        if (request.getStartDate() != null && request.getEndDate() != null) return;

        LocalDate today = LocalDate.now();
        String gran = request.getGranularity();
        LocalDate start;
        LocalDate end;
        switch (gran) {
            case "day":
                start = today.withDayOfMonth(1);
                end = today.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
                break;
            case "year":
                start = LocalDate.of(today.getYear() - 4, 1, 1);
                end = LocalDate.of(today.getYear(), 12, 31);
                break;
            default:
                start = LocalDate.of(today.getYear(), 1, 1);
                end = LocalDate.of(today.getYear(), 12, 31);
                break;
        }
        if (request.getStartDate() == null) request.setStartDate(start.toString());
        if (request.getEndDate() == null) request.setEndDate(end.toString());
    }

    private String generateCacheKey(ReceivableBalanceTrendRequest request) {
        return String.format("%s:%s:%s:%s",
                CACHE_KEY_PREFIX, request.getGranularity(),
                request.getStartDate(), request.getEndDate());
    }

    private ReceivableBalanceTrendResponse buildEmptyResponse(ReceivableBalanceTrendRequest request) {
        ReceivableBalanceTrendResponse resp = new ReceivableBalanceTrendResponse();
        resp.setStartDate(request.getStartDate());
        resp.setEndDate(request.getEndDate());
        resp.setGranularity(request.getGranularity());
        resp.setFromCache(false);
        resp.setComputedAt(java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        resp.setXaxis(Collections.emptyList());
        resp.setSeries(Collections.emptyList());
        resp.setSummary(new ReceivableSummary(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        return resp;
    }
}
