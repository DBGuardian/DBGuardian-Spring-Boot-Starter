package com.erp.service.report.impl;

import com.erp.controller.report.dto.WasteLimitAnalysisRequest;
import com.erp.controller.report.dto.WasteLimitAnalysisResponse;
import com.erp.controller.report.dto.WasteLimitAnalysisResponse.WasteCategoryDTO;
import com.erp.controller.report.dto.WasteLimitAnalysisResponse.WasteLimitSummary;
import com.erp.mapper.report.WasteLimitAnalysisMapper;
import com.erp.service.report.WasteLimitAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 危险废物限额分析 ServiceImpl
 *
 * 计算逻辑：
 * 1. 查询所有有效废物类别及其限额
 * 2. 查询指定时间区间内各废物类别的合同签订量（计划转移数量）
 * 3. 查询指定时间区间内各废物类别的实际收运量（实际入库数量）
 * 4. 查询指定时间区间内各废物类别的已生成联单量（确认数量）
 * 5. 汇总统计：总限额使用率等
 *
 * 数据来源：
 * - 限额：HAZARDOUS_WASTE_CATEGORY.限额
 * - 合同签订量：CONTRACT_WASTE_ITEM → CONTRACT_ITEM → CONTRACT（合同状态：已通过/执行中/已完结/已归档）
 * - 实际收运量：WAREHOUSING_WASTE_ITEM.实际收运数量
 * - 已生成联单量：TRANSFER_MANIFEST_ITEM.确认数量
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WasteLimitAnalysisServiceImpl implements WasteLimitAnalysisService {

    private static final String CACHE_KEY_PREFIX = "waste:limit-analysis";
    private static final long CACHE_TTL_SECONDS = 604800L;   // 7 天
    private static final long COMPUTING_TTL_SECONDS = 300L;  // 5 分钟

    private final WasteLimitAnalysisMapper mapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // ─────────────────────────── 接口实现 ───────────────────────────

    @Override
    public WasteLimitAnalysisResponse getLimitAnalysis(WasteLimitAnalysisRequest request) {
        normalizeRequest(request);
        String cacheKey = generateCacheKey(request);

        // 尝试读缓存
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof WasteLimitAnalysisResponse) {
            log.info("[WasteLimitAnalysis] 缓存命中 key={}", cacheKey);
            WasteLimitAnalysisResponse resp = (WasteLimitAnalysisResponse) cached;
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
            WasteLimitAnalysisResponse data = calculate(request);
            data.setFromCache(false);
            redisTemplate.opsForValue().set(cacheKey, data, Duration.ofSeconds(CACHE_TTL_SECONDS));
            log.info("[WasteLimitAnalysis] 计算完成并缓存 key={}", cacheKey);
            return data;
        } finally {
            redisTemplate.delete(computingKey);
        }
    }

    @Override
    public WasteLimitAnalysisResponse recalculate(WasteLimitAnalysisRequest request) {
        normalizeRequest(request);
        String cacheKey = generateCacheKey(request);
        redisTemplate.delete(cacheKey);
        redisTemplate.delete(cacheKey + ":computing");
        log.info("[WasteLimitAnalysis] 缓存已清除，重新计算 key={}", cacheKey);
        WasteLimitAnalysisResponse resp = getLimitAnalysis(request);
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
        result.put("message", "所有危险废物限额分析缓存已清除");
        result.put("count", count);
        return result;
    }

    // ─────────────────────────── 核心计算 ───────────────────────────

    private WasteLimitAnalysisResponse calculate(WasteLimitAnalysisRequest request) {
        String startDate = request.getStartDate();
        String endDate   = request.getEndDate();
        String[] wasteCategories = request.getWasteCategories() != null
                ? request.getWasteCategories()
                : new String[0];

        // Step 1: 查询所有废物类别及其限额（按限额升序）
        List<WasteCategoryDTO> allCategories = mapper.selectAllCategories(wasteCategories);
        if (allCategories.isEmpty()) {
            return buildEmptyResponse(request);
        }

        // 按 categoryCode 建立映射
        Map<String, WasteCategoryDTO> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(
                        dto -> dto.getCategoryCode(),
                        dto -> {
                            // 初始化 pending 和 manifest 字段
                            dto.setPending(0.0);
                            dto.setManifest(0.0);
                            return dto;
                        }
                ));

        // Step 2: 查询合同签订量（计划转移数量）
        List<WasteCategoryDTO> plannedList = mapper.selectPlannedByPeriod(startDate, endDate, wasteCategories);
        for (WasteCategoryDTO dto : plannedList) {
            WasteCategoryDTO target = categoryMap.get(dto.getCategoryCode());
            if (target != null) {
                // 无限量类别（wasteLimit IS NULL）的合同签订量置为 0
                if (target.getWasteLimit() == null) {
                    target.setPlanned(0.0);
                } else {
                    target.setPlanned(dto.getPlanned());
                }
            }
        }

        // Step 3: 查询实际收运量
        List<WasteCategoryDTO> actualList = mapper.selectActualByPeriod(startDate, endDate, wasteCategories);
        // 构建 actualMap 方便后续计算
        Map<String, Double> actualMap = actualList.stream()
                .collect(Collectors.toMap(WasteCategoryDTO::getCategoryCode, WasteCategoryDTO::getActual));
        for (WasteCategoryDTO dto : actualList) {
            WasteCategoryDTO target = categoryMap.get(dto.getCategoryCode());
            if (target != null) {
                target.setActual(dto.getActual());
            }
        }

        // Step 4: 计算 pending（针对所有类别）
        // 计算逻辑：
        // 1. 无限量类别（wasteLimit IS NULL）的计划量为0
        // 2. pending = planned - actual，如果结果为负数则置为0
        for (WasteCategoryDTO dto : categoryMap.values()) {
            // 无限量类别：计划量置为0
            Double planned = dto.getWasteLimit() == null ? 0.0 : (dto.getPlanned() != null ? dto.getPlanned() : 0.0);
            Double actual = actualMap.getOrDefault(dto.getCategoryCode(), 0.0);

            // pending = planned - actual，负数置为0
            double pending = planned - actual;
            dto.setPending(pending < 0 ? 0.0 : round(pending));
        }

        // Step 5: 查询已生成联单量
        List<WasteCategoryDTO> manifestList = mapper.selectManifestByPeriod(startDate, endDate, wasteCategories);
        for (WasteCategoryDTO dto : manifestList) {
            WasteCategoryDTO target = categoryMap.get(dto.getCategoryCode());
            if (target != null) {
                target.setManifest(dto.getManifest());
            }
        }

        // Step 6: 计算 usageRate 并构建最终列表
        List<WasteCategoryDTO> finalCategories = new ArrayList<>(categoryMap.values());
        for (WasteCategoryDTO dto : finalCategories) {
            Double wasteLimit = dto.getWasteLimit();
            Double actual = dto.getActual() != null ? dto.getActual() : 0.0;
            if (wasteLimit != null && wasteLimit > 0) {
                dto.setUsageRate(round2(actual / wasteLimit * 100));
            } else {
                dto.setUsageRate(0.0);  // 无限额或限额为0时显示 0%
            }
        }

        // Step 7: 构建 series 数据（与 categories 一一对应）
        List<WasteLimitAnalysisResponse.WasteLimitSeries> seriesList = new ArrayList<>();
        for (WasteCategoryDTO cat : finalCategories) {
            WasteLimitAnalysisResponse.WasteLimitSeries s = new WasteLimitAnalysisResponse.WasteLimitSeries();
            s.setWasteLimit(cat.getWasteLimit() != null ? cat.getWasteLimit() : 0.0);
            s.setPending(cat.getPending() != null ? cat.getPending() : 0.0);
            s.setManifest(cat.getManifest() != null ? cat.getManifest() : 0.0);
            seriesList.add(s);
        }

        // Step 8: 计算汇总统计
        WasteLimitSummary summary = calculateSummary(finalCategories);

        // Step 7: 组装响应
        String computedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        WasteLimitAnalysisResponse resp = new WasteLimitAnalysisResponse();
        resp.setStartDate(startDate);
        resp.setEndDate(endDate);
        resp.setFromCache(false);
        resp.setComputedAt(computedAt);
        resp.setCategories(finalCategories);
        resp.setSeries(seriesList);
        resp.setSummary(summary);

        return resp;
    }

    // ─────────────────────────── 辅助方法 ───────────────────────────

    /**
     * 计算汇总统计数据
     */
    private WasteLimitSummary calculateSummary(List<WasteCategoryDTO> categories) {
        int totalCategories = categories.size();

        double totalLimit = categories.stream()
                .mapToDouble(dto -> dto.getWasteLimit() != null ? dto.getWasteLimit() : 0.0)
                .sum();

        // 计算总计划量：无限量类别计划量为0
        double totalPlanned = categories.stream()
                .mapToDouble(dto -> {
                    // 无限量类别计划量为0
                    if (dto.getWasteLimit() == null) {
                        return 0.0;
                    }
                    return dto.getPlanned() != null ? dto.getPlanned() : 0.0;
                })
                .sum();

        double totalActual = categories.stream()
                .mapToDouble(dto -> dto.getActual() != null ? dto.getActual() : 0.0)
                .sum();

        // 计算总pending：直接使用各明细的pending（已经是处理过负数的值）
        double totalPending = categories.stream()
                .mapToDouble(dto -> dto.getPending() != null ? dto.getPending() : 0.0)
                .sum();

        // 如果总pending为负数，置为0
        if (totalPending < 0) {
            totalPending = 0.0;
        }

        double totalManifest = categories.stream()
                .mapToDouble(dto -> dto.getManifest() != null ? dto.getManifest() : 0.0)
                .sum();

        double limitUsageRate = 0.0;
        if (totalLimit > 0) {
            limitUsageRate = round(totalActual / totalLimit * 100);
        }

        return new WasteLimitSummary(
                totalCategories,
                round(totalLimit),
                round(totalPlanned),
                round(totalActual),
                round(totalPending),
                round(totalManifest),
                limitUsageRate
        );
    }

    /**
     * 生成缓存键
     * 格式：waste:limit-analysis:{dateRangeMode}:{startDate}:{endDate}:{year}:categories=...
     */
    private String generateCacheKey(WasteLimitAnalysisRequest request) {
        StringBuilder sb = new StringBuilder(CACHE_KEY_PREFIX).append(":");
        sb.append(request.getDateRangeMode()).append(":");
        if ("CURRENT_YEAR".equals(request.getDateRangeMode())) {
            sb.append(request.getStartDate()).append(":").append(request.getEndDate());
        } else {
            sb.append(request.getYear());
        }
        if (request.getWasteCategories() != null && request.getWasteCategories().length > 0) {
            String cats = String.join(",", request.getWasteCategories());
            sb.append(":categories=").append(cats);
        }
        return sb.toString();
    }

    /**
     * 规范化请求参数
     */
    private void normalizeRequest(WasteLimitAnalysisRequest request) {
        if (request.getDateRangeMode() == null) {
            request.setDateRangeMode("CURRENT_YEAR");
        }

        if ("CURRENT_YEAR".equals(request.getDateRangeMode())) {
            // 当年模式：必须提供 startDate 和 endDate
            if (request.getStartDate() == null || request.getEndDate() == null) {
                // 默认：当年1月1日 至 今天
                LocalDate now = LocalDate.now();
                request.setStartDate(now.withDayOfYear(1).toString());
                request.setEndDate(now.toString());
            }
        } else {
            // 往年模式：必须提供 year
            if (request.getYear() == null) {
                request.setYear(LocalDate.now().getYear() - 1);
            }
            // 将 year 转换为 startDate/endDate 供 Mapper 使用
            int y = request.getYear();
            request.setStartDate(y + "-01-01");
            request.setEndDate(y + "-12-31");
        }
    }

    /**
     * 构建空响应
     */
    private WasteLimitAnalysisResponse buildEmptyResponse(WasteLimitAnalysisRequest request) {
        String computedAt = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        return new WasteLimitAnalysisResponse(
                request.getStartDate(),
                request.getEndDate(),
                false,
                computedAt,
                new ArrayList<>(),
                new ArrayList<>(),
                new WasteLimitSummary(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        );
    }

    /**
     * 保留3位小数
     */
    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 保留2位小数（百分比）
     */
    private double round2(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
