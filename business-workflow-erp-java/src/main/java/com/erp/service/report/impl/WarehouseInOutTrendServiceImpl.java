package com.erp.service.report.impl;

import com.erp.controller.report.dto.WarehouseInOutTrendRequest;
import com.erp.controller.report.dto.WarehouseInOutTrendResponse;
import com.erp.controller.report.dto.WarehouseInOutTrendResponse.WarehouseInOutCountDTO;
import com.erp.controller.report.dto.WarehouseInOutTrendResponse.WarehouseInOutSeries;
import com.erp.controller.report.dto.WarehouseInOutTrendResponse.WarehouseInOutSummary;
import com.erp.mapper.report.WarehouseInOutTrendMapper;
import com.erp.service.report.WarehouseInOutTrendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 仓库出入库数量变动趋势 ServiceImpl
 *
 * 计算逻辑：
 * 1. 生成 [startDate, endDate] 的时间粒度序列（xAxis）
 * 2. 查询区间内各粒度的入库、出库危废重量
 * 3. 补零：xAxis 中无数据的节点在 Service 层补 0
 * 4. 系列顺序固定：IN（入库）→ OUT（出库）→ TOTAL（总量）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseInOutTrendServiceImpl implements WarehouseInOutTrendService {

    private static final String CACHE_KEY_PREFIX = "warehouse:in-out-trend";
    private static final long CACHE_TTL_SECONDS = 604800L;
    private static final long COMPUTING_TTL_SECONDS = 300L;

    private final WarehouseInOutTrendMapper mapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public WarehouseInOutTrendResponse getTrend(WarehouseInOutTrendRequest request) {
        normalizeRequest(request);
        String cacheKey = generateCacheKey(request);

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof WarehouseInOutTrendResponse) {
            log.info("[WarehouseInOutTrend] 缓存命中 key={}", cacheKey);
            WarehouseInOutTrendResponse resp = (WarehouseInOutTrendResponse) cached;
            resp.setFromCache(true);
            return resp;
        }

        String computingKey = cacheKey + ":computing";
        Object isComputing = redisTemplate.opsForValue().get(computingKey);
        if (Boolean.TRUE.equals(isComputing)) {
            throw new RuntimeException("数据正在计算中，请稍候...");
        }

        redisTemplate.opsForValue().set(computingKey, true, Duration.ofSeconds(COMPUTING_TTL_SECONDS));
        try {
            WarehouseInOutTrendResponse data = calculate(request);
            data.setFromCache(false);
            redisTemplate.opsForValue().set(cacheKey, data, Duration.ofSeconds(CACHE_TTL_SECONDS));
            log.info("[WarehouseInOutTrend] 计算完成并缓存 key={}", cacheKey);
            return data;
        } finally {
            redisTemplate.delete(computingKey);
        }
    }

    @Override
    public WarehouseInOutTrendResponse recalculate(WarehouseInOutTrendRequest request) {
        normalizeRequest(request);
        String cacheKey = generateCacheKey(request);
        redisTemplate.delete(cacheKey);
        redisTemplate.delete(cacheKey + ":computing");
        log.info("[WarehouseInOutTrend] 缓存已清除，重新计算 key={}", cacheKey);
        WarehouseInOutTrendResponse resp = getTrend(request);
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
        result.put("message", "所有仓库出入库趋势缓存已清除");
        result.put("count", count);
        return result;
    }

    private WarehouseInOutTrendResponse calculate(WarehouseInOutTrendRequest request) {
        String startDate = request.getStartDate();
        String endDate = request.getEndDate();
        String gran = request.getGranularity();

        List<String> xAxis = generateXAxis(startDate, endDate, gran);
        if (xAxis.isEmpty()) {
            return buildEmptyResponse(request);
        }

        List<WarehouseInOutCountDTO> inRawData = mapper.selectInWeightByPeriod(startDate, endDate, gran);

        Map<String, BigDecimal> inWeightMap = new HashMap<>();
        for (WarehouseInOutCountDTO dto : inRawData) {
            if (dto.getDateLabel() == null) continue;
            BigDecimal weight = dto.getInWeight() == null ? BigDecimal.ZERO : dto.getInWeight();
            inWeightMap.merge(dto.getDateLabel(), weight, BigDecimal::add);
        }

        List<BigDecimal> inData = new ArrayList<>();
        List<BigDecimal> outData = new ArrayList<>();
        List<BigDecimal> totalData = new ArrayList<>();
        for (String label : xAxis) {
            BigDecimal inWeight = scale3(inWeightMap.getOrDefault(label, BigDecimal.ZERO));
            BigDecimal outWeight = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
            BigDecimal totalWeight = scale3(inWeight.add(outWeight));
            inData.add(inWeight);
            outData.add(outWeight);
            totalData.add(totalWeight);
        }

        BigDecimal totalIn = scale3(sum(inData));
        BigDecimal totalOut = scale3(sum(outData));
        BigDecimal totalMovement = scale3(totalIn.add(totalOut));
        BigDecimal lastIn = last(inData);
        BigDecimal lastOut = last(outData);
        BigDecimal lastMovement = scale3(lastIn.add(lastOut));

        List<WarehouseInOutSeries> series = new ArrayList<>();
        series.add(new WarehouseInOutSeries("入库", "IN", inData));
        series.add(new WarehouseInOutSeries("出库", "OUT", outData));
        series.add(new WarehouseInOutSeries("总量", "TOTAL", totalData));

        String computedAt = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        WarehouseInOutTrendResponse resp = new WarehouseInOutTrendResponse();
        resp.setStartDate(startDate);
        resp.setEndDate(endDate);
        resp.setGranularity(gran);
        resp.setFromCache(false);
        resp.setComputedAt(computedAt);
        resp.setXAxis(xAxis);
        resp.setSeries(series);
        resp.setSummary(new WarehouseInOutSummary(
                totalIn,
                totalOut,
                totalMovement,
                lastIn,
                lastOut,
                lastMovement
        ));
        return resp;
    }

    private List<String> generateXAxis(String startDate, String endDate, String gran) {
        List<String> xAxis = new ArrayList<>();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
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
            default:
                LocalDate cursor = start.withDayOfMonth(1);
                while (!cursor.isAfter(end)) {
                    xAxis.add(cursor.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                    cursor = cursor.plusMonths(1);
                }
        }
        return xAxis;
    }

    private BigDecimal scale3(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(List<BigDecimal> list) {
        if (list == null || list.isEmpty()) return BigDecimal.ZERO;
        return list.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal last(List<BigDecimal> list) {
        if (list == null || list.isEmpty()) return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        return scale3(list.get(list.size() - 1));
    }

    private void normalizeRequest(WarehouseInOutTrendRequest request) {
        LocalDate today = LocalDate.now();
        String gran = request.getGranularity() == null ? "month" : request.getGranularity();
        request.setGranularity(gran);
        if (request.getStartDate() == null) {
            LocalDate start;
            switch (gran) {
                case "day":
                    start = today.withDayOfMonth(1);
                    break;
                case "year":
                    start = LocalDate.of(today.getYear() - 4, 1, 1);
                    break;
                default:
                    start = LocalDate.of(today.getYear(), 1, 1);
            }
            request.setStartDate(start.toString());
        }
        if (request.getEndDate() == null) {
            request.setEndDate(today.toString());
        }
    }

    private String generateCacheKey(WarehouseInOutTrendRequest request) {
        return String.format("%s:%s:%s:%s",
                CACHE_KEY_PREFIX, request.getGranularity(),
                request.getStartDate(), request.getEndDate());
    }

    private WarehouseInOutTrendResponse buildEmptyResponse(WarehouseInOutTrendRequest request) {
        List<WarehouseInOutSeries> series = new ArrayList<>();
        series.add(new WarehouseInOutSeries("入库", "IN", Collections.emptyList()));
        series.add(new WarehouseInOutSeries("出库", "OUT", Collections.emptyList()));
        series.add(new WarehouseInOutSeries("总量", "TOTAL", Collections.emptyList()));

        WarehouseInOutTrendResponse resp = new WarehouseInOutTrendResponse();
        resp.setStartDate(request.getStartDate());
        resp.setEndDate(request.getEndDate());
        resp.setGranularity(request.getGranularity());
        resp.setFromCache(false);
        resp.setComputedAt(java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        resp.setXAxis(Collections.emptyList());
        resp.setSeries(series);
        resp.setSummary(new WarehouseInOutSummary(
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
        ));
        return resp;
    }
}
