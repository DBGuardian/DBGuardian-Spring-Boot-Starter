package com.erp.service.report.impl;

import com.erp.controller.report.dto.EmployeePerformancePieRequest;
import com.erp.controller.report.dto.EmployeePerformancePieResponse;
import com.erp.controller.report.dto.EmployeePerformancePieResponse.EmployeePerformanceItem;
import com.erp.controller.report.dto.EmployeePerformancePieResponse.EmployeeRawDTO;
import com.erp.mapper.report.EmployeePerformanceMapper;
import com.erp.service.report.EmployeePerformanceService;
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
 * 员工业绩占比饼图 ServiceImpl
 *
 * 计算逻辑：
 * 1. 根据 dimension 执行对应 SQL 查询，GROUP BY 员工编码
 * 2. 计算 total = SUM(所有员工 value)
 * 3. 计算每位员工 percentage = ROUND(value / total * 100, 2)
 * 4. 过滤 value = 0 的员工，按 value DESC 排序
 *
 * 3个维度：
 *   CONTRACT_COUNT    - CONTRACT.创建人编码，COUNT(合同编号)
 *   WAREHOUSE_WEIGHT  - WAREHOUSING.仓管员编码 → WAREHOUSING_WASTE_ITEM.实际收运数量 SUM
 *   SETTLEMENT_AMOUNT - SETTLEMENT.制单人编码，SUM(结算金额)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeePerformanceServiceImpl implements EmployeePerformanceService {

    private static final String CACHE_KEY_PREFIX = "employee-performance:pie";
    private static final long   CACHE_TTL_SECONDS     = 604800L; // 7 天
    private static final long   COMPUTING_TTL_SECONDS = 300L;   // 5 分钟

    // 维度配置：label / unit
    private static final Map<String, String[]> DIMENSION_CONFIG = new LinkedHashMap<>();
    static {
        DIMENSION_CONFIG.put("CONTRACT_COUNT",    new String[]{"合同签订数量", "份"});
        DIMENSION_CONFIG.put("WAREHOUSE_WEIGHT",  new String[]{"入库重量",     "吨"});
        DIMENSION_CONFIG.put("SETTLEMENT_AMOUNT", new String[]{"结算金额",     "元"});
    }

    private final EmployeePerformanceMapper mapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // ─────────────────────────── 接口实现 ───────────────────────────

    @Override
    public EmployeePerformancePieResponse getPie(EmployeePerformancePieRequest request) {
        normalizeRequest(request);
        String cacheKey = generateCacheKey(request);

        // 尝试读缓存
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof EmployeePerformancePieResponse) {
            log.info("[EmployeePerformance] 缓存命中 key={}", cacheKey);
            EmployeePerformancePieResponse resp = (EmployeePerformancePieResponse) cached;
            resp.setFromCache(true);
            return resp;
        }

        // 防并发
        String computingKey = cacheKey + ":computing";
        Object isComputing = redisTemplate.opsForValue().get(computingKey);
        if (Boolean.TRUE.equals(isComputing)) {
            throw new RuntimeException("数据正在计算中，请稍候...");
        }

        redisTemplate.opsForValue().set(computingKey, true,
                Duration.ofSeconds(COMPUTING_TTL_SECONDS));
        try {
            EmployeePerformancePieResponse data = calculate(request);
            data.setFromCache(false);
            redisTemplate.opsForValue().set(cacheKey, data,
                    Duration.ofSeconds(CACHE_TTL_SECONDS));
            log.info("[EmployeePerformance] 计算完成并缓存 key={}", cacheKey);
            return data;
        } finally {
            redisTemplate.delete(computingKey);
        }
    }

    @Override
    public EmployeePerformancePieResponse recalculate(EmployeePerformancePieRequest request) {
        normalizeRequest(request);
        String cacheKey = generateCacheKey(request);
        redisTemplate.delete(cacheKey);
        redisTemplate.delete(cacheKey + ":computing");
        log.info("[EmployeePerformance] 缓存已清除，重新计算 key={}", cacheKey);
        EmployeePerformancePieResponse resp = getPie(request);
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
        result.put("message", "所有员工业绩占比饼图缓存已清除");
        result.put("count", count);
        return result;
    }

    // ─────────────────────────── 核心计算 ───────────────────────────

    private EmployeePerformancePieResponse calculate(EmployeePerformancePieRequest request) {
        String dimension  = request.getDimension();
        String startDate  = request.getStartDate();
        String endDate    = request.getEndDate();

        // 查询原始数据
        List<EmployeeRawDTO> rawList = queryByDimension(dimension, startDate, endDate);

        // 过滤零值
        rawList = rawList.stream()
                .filter(r -> r.getValue() != null
                        && r.getValue().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        // 计算 total
        BigDecimal total = rawList.stream()
                .map(EmployeeRawDTO::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 构建 list，计算 percentage，按 value DESC 排序
        List<EmployeePerformanceItem> list;
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            list = Collections.emptyList();
        } else {
            list = rawList.stream()
                    .map(r -> {
                        BigDecimal pct = r.getValue()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(total, 2, RoundingMode.HALF_UP);
                        return new EmployeePerformanceItem(
                                r.getEmployeeId(),
                                r.getEmployeeName(),
                                r.getDept(),
                                r.getValue(),
                                pct);
                    })
                    .sorted(Comparator.comparing(EmployeePerformanceItem::getValue).reversed())
                    .collect(Collectors.toList());
        }

        // 维度标签 / 单位
        String[] cfg = DIMENSION_CONFIG.getOrDefault(dimension,
                new String[]{dimension, ""});

        String computedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        EmployeePerformancePieResponse resp = new EmployeePerformancePieResponse();
        resp.setDimension(dimension);
        resp.setDimensionLabel(cfg[0]);
        resp.setUnit(cfg[1]);
        resp.setStartDate(startDate);
        resp.setEndDate(endDate);
        resp.setFromCache(false);
        resp.setComputedAt(computedAt);
        resp.setTotal(total);
        resp.setList(list);
        return resp;
    }

    /**
     * 根据维度调用对应的 Mapper 方法
     */
    private List<EmployeeRawDTO> queryByDimension(String dimension,
                                                   String startDate, String endDate) {
        switch (dimension) {
            case "WAREHOUSE_WEIGHT":
                return mapper.selectWarehouseWeightByEmployee(startDate, endDate);
            case "SETTLEMENT_AMOUNT":
                return mapper.selectSettlementAmountByEmployee(startDate, endDate);
            case "CONTRACT_COUNT":
            default:
                return mapper.selectContractCountByEmployee(startDate, endDate);
        }
    }

    // ─────────────────────────── 工具方法 ───────────────────────────

    private void normalizeRequest(EmployeePerformancePieRequest request) {
        if (request.getDimension() == null || request.getDimension().trim().isEmpty()) {
            request.setDimension("CONTRACT_COUNT");
        }
        if (request.getDateRange() == null || request.getDateRange().trim().isEmpty()) {
            request.setDateRange("CURRENT_YEAR");
        }

        String startDate = request.getStartDate();
        String endDate = request.getEndDate();
        boolean hasCustomDateRange = startDate != null && !startDate.trim().isEmpty()
                && endDate != null && !endDate.trim().isEmpty();

        if (hasCustomDateRange) {
            request.setDateRange("ALL");
            return;
        }

        if ("ALL".equalsIgnoreCase(request.getDateRange())) {
            // 全部时间不限日期，传 null 让 SQL 忽略日期过滤
            request.setStartDate(null);
            request.setEndDate(null);
        } else {
            // 当年模式
            if (request.getStartDate() == null || request.getStartDate().trim().isEmpty()) {
                request.setStartDate(LocalDate.now().withDayOfYear(1).toString());
            }
            if (request.getEndDate() == null || request.getEndDate().trim().isEmpty()) {
                request.setEndDate(LocalDate.now().toString());
            }
        }
    }

    private String generateCacheKey(EmployeePerformancePieRequest request) {
        return String.format("%s:%s:%s:%s",
                CACHE_KEY_PREFIX,
                request.getDimension(),
                request.getStartDate(),
                request.getEndDate());
    }
}
