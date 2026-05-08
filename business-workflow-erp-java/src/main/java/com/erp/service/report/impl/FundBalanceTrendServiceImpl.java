package com.erp.service.report.impl;

import com.erp.controller.report.dto.FundBalanceTrendRequest;
import com.erp.controller.report.dto.FundBalanceTrendResponse;
import com.erp.controller.report.dto.FundBalanceTrendResponse.AccountBalanceSummary;
import com.erp.controller.report.dto.FundBalanceTrendResponse.AccountInitialDTO;
import com.erp.controller.report.dto.FundBalanceTrendResponse.BalanceTrendSeries;
import com.erp.controller.report.dto.FundBalanceTrendResponse.DayFlowDTO;
import com.erp.controller.report.dto.FundBalanceTrendResponse.TransactionFlowDTO;
import com.erp.controller.report.dto.FundBalanceTrendResponse.YearlyBalanceDTO;
import com.erp.entity.finance.FundAccount;
import com.erp.mapper.report.FundBalanceTrendMapper;
import com.erp.service.report.FundBalanceTrendService;
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
 * 资金余额变动趋势 ServiceImpl
 *
 * 计算逻辑：
 * 1. 查询所有启用账户
 * 2. 查询各账户期初余额（取 startDate 之前最近账期）
 * 3. 从 periodStart 累积到 startDate 前一天，得到 startDate 的初始余额
 * 4. 按横轴日期逐日递推余额（无流水则沿用前日余额，保持连续折线）
 * 5. 聚合总余额折线（各账户当日余额之和）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundBalanceTrendServiceImpl implements FundBalanceTrendService {

    private static final String CACHE_KEY_PREFIX = "fund:balance-trend";
    /** 缓存有效期：7 天 */
    private static final long CACHE_TTL_SECONDS = 604800L;
    /** 防并发标记有效期：5 分钟 */
    private static final long COMPUTING_TTL_SECONDS = 300L;

    private final FundBalanceTrendMapper mapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // ─────────────────────────── 接口实现 ───────────────────────────

    @Override
    public FundBalanceTrendResponse getBalanceTrend(FundBalanceTrendRequest request) {
        normalizeRequest(request);
        String cacheKey = generateCacheKey(request);

        // 1. 尝试读取缓存
        FundBalanceTrendResponse cached =
                (FundBalanceTrendResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            cached.setFromCache(true);
            log.info("[FundBalanceTrend] 缓存命中，key={}", cacheKey);
            return cached;
        }

        // 2. 防并发：检查是否有其他线程正在计算
        String computingKey = cacheKey + ":computing";
        Boolean isComputing = (Boolean) redisTemplate.opsForValue().get(computingKey);
        if (Boolean.TRUE.equals(isComputing)) {
            log.warn("[FundBalanceTrend] 数据正在计算中，key={}", cacheKey);
            throw new RuntimeException("数据正在计算中，请稍候...");
        }

        // 3. 标记计算中，清除旧缓存，重新计算
        redisTemplate.delete(cacheKey);
        redisTemplate.opsForValue().set(computingKey, true,
                Duration.ofSeconds(COMPUTING_TTL_SECONDS));
        try {
            FundBalanceTrendResponse data = calculateBalanceTrend(request);
            data.setFromCache(false);
            redisTemplate.opsForValue().set(cacheKey, data,
                    Duration.ofSeconds(CACHE_TTL_SECONDS));
            log.info("[FundBalanceTrend] 计算完成并缓存，key={}", cacheKey);
            return data;
        } finally {
            redisTemplate.delete(computingKey);
        }
    }

    @Override
    public FundBalanceTrendResponse recalculate(FundBalanceTrendRequest request) {
        normalizeRequest(request);
        String cacheKey = generateCacheKey(request);
        redisTemplate.delete(cacheKey);
        redisTemplate.delete(cacheKey + ":computing");
        log.info("[FundBalanceTrend] 缓存已清除，重新计算，key={}", cacheKey);
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
        log.info("[FundBalanceTrend] 已清除所有缓存，共 {} 个", count);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "所有资金余额趋势缓存已清除");
        result.put("count", count);
        return result;
    }

    // ─────────────────────────── 核心计算 ───────────────────────────

    private FundBalanceTrendResponse calculateBalanceTrend(FundBalanceTrendRequest request) {
        // Step 1: 查询启用账户
        List<FundAccount> accounts = mapper.selectEnabledAccounts(request.getAccountIds());
        if (accounts.isEmpty()) {
            return buildEmptyResponse(request);
        }
        List<Long> accountIds = accounts.stream()
                .map(FundAccount::getAccountId).collect(Collectors.toList());

        // Step 2: 按时间粒度分发处理
        return calculatePeriodTrend(request, accounts, accountIds);
    }

    /**
     * 计算周期趋势（按粒度分发）
     * day   = 当月每天（按日统计流水）
     * month = 当年每月（按月统计期初余额）
     * year  = 近几年每年（按年统计期初余额）
     */
    private FundBalanceTrendResponse calculatePeriodTrend(FundBalanceTrendRequest request,
                                                           List<FundAccount> accounts,
                                                           List<Long> accountIds) {
        String dateRange = request.getDateRange();
        if ("day".equals(dateRange)) {
            return calculateDailyTrend(request, accounts, accountIds);
        } else if ("month".equals(dateRange)) {
            return calculateMonthlyTrend(request, accounts, accountIds);
        } else if ("year".equals(dateRange)) {
            return calculateYearlyTrend(request, accounts, accountIds);
        } else {
            // 兼容旧值：week/quarter 按日处理，year 旧逻辑按月处理
            if ("week".equals(dateRange)) return calculateDailyTrend(request, accounts, accountIds);
            return calculateMonthlyTrend(request, accounts, accountIds);
        }
    }

    /**
     * 按日统计流水（用于周和月维度）
     */
    private FundBalanceTrendResponse calculateDailyTrend(FundBalanceTrendRequest request,
                                                          List<FundAccount> accounts,
                                                          List<Long> accountIds) {
        String startDate = request.getStartDate().substring(0, 10);
        String endDate = request.getEndDate().substring(0, 10);
        
        // 查询期初余额（取开始日期所在月份的期初余额，即该月1号的期初余额）
        LocalDate startLocalDate = LocalDate.parse(startDate);
        String monthStart = startLocalDate.withDayOfMonth(1).toString();
        List<AccountInitialDTO> initialList = mapper.selectInitialBalances(accountIds, monthStart, true);
        
        Map<Long, BigDecimal> initialBalanceMap = new HashMap<>();
        for (FundAccount account : accounts) {
            BigDecimal balance = BigDecimal.ZERO;
            for (AccountInitialDTO dto : initialList) {
                if (dto.getAccountId().equals(account.getAccountId())) {
                    balance = dto.getInitialBalance();
                    break;
                }
            }
            initialBalanceMap.put(account.getAccountId(), balance);
        }

        // 查询区间内按日的流水
        List<DayFlowDTO> dayFlows = mapper.selectDayFlows(accountIds, startDate, endDate);
        Map<String, DayFlowDTO> dayFlowMap = new HashMap<>();
        for (DayFlowDTO flow : dayFlows) {
            String key = flow.getAccountId() + ":" + flow.getTransactionDate();
            dayFlowMap.put(key, flow);
        }

        // 生成日期序列
        List<String> xAxis = new ArrayList<>();
        LocalDate cursor = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        while (!cursor.isAfter(end)) {
            xAxis.add(cursor.toString());
            cursor = cursor.plusDays(1);
        }

        // 为每个账户计算每日的余额序列
        Map<Long, List<BigDecimal>> snapshotMap = new LinkedHashMap<>();
        for (FundAccount account : accounts) {
            List<BigDecimal> dailyBalances = new ArrayList<>();
            BigDecimal currentBalance = initialBalanceMap.get(account.getAccountId());
            
            for (String date : xAxis) {
                String key = account.getAccountId() + ":" + date;
                DayFlowDTO flow = dayFlowMap.get(key);
                
                if (flow != null) {
                    // 有流水：累积收支
                    currentBalance = currentBalance
                            .add(flow.getDayIncome())
                            .subtract(flow.getDayExpenditure());
                } else {
                    // 无流水：保持前一天余额（虚拟点）
                }
                
                dailyBalances.add(currentBalance);
            }
            
            snapshotMap.put(account.getAccountId(), dailyBalances);
        }

        // 构建 series（总余额在首位）
        List<BalanceTrendSeries> seriesList = new ArrayList<>();

        // 总余额折线
        List<BigDecimal> totalData = buildTotalData(xAxis.size(), accounts, snapshotMap);
        seriesList.add(new BalanceTrendSeries(0L, "总余额", "TOTAL", "#409eff", totalData, null));

        // 各账户折线
        for (FundAccount account : accounts) {
            seriesList.add(new BalanceTrendSeries(
                    account.getAccountId(),
                    account.getAccountName(),
                    account.getAccountType(),
                    null,
                    snapshotMap.get(account.getAccountId()),
                    null));
        }

        // 构建账户余额摘要卡片
        List<AccountBalanceSummary> summaries = new ArrayList<>();
        for (FundAccount account : accounts) {
            List<BigDecimal> snap = snapshotMap.get(account.getAccountId());
            BigDecimal last = snap.isEmpty() ? BigDecimal.ZERO : snap.get(snap.size() - 1);
            summaries.add(new AccountBalanceSummary(
                    account.getAccountId(),
                    account.getAccountName(),
                    account.getAccountType(),
                    last));
        }

        BigDecimal total = totalData.isEmpty() ? BigDecimal.ZERO : totalData.get(totalData.size() - 1);
        String computedAt = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        FundBalanceTrendResponse resp = new FundBalanceTrendResponse();
        resp.setDateRange(request.getDateRange());
        resp.setStartDate(startDate);
        resp.setEndDate(endDate);
        resp.setFromCache(false);
        resp.setComputedAt(computedAt);
        resp.setXAxis(xAxis);
        resp.setSeries(seriesList);
        resp.setAccounts(summaries);
        resp.setTotalCurrentBalance(total);
        
        return resp;
    }

    /**
     * 按月统计（支持自定义开始/结束日期，按月聚合）
     */
    private FundBalanceTrendResponse calculateMonthlyTrend(FundBalanceTrendRequest request,
                                                            List<FundAccount> accounts,
                                                            List<Long> accountIds) {
        LocalDate start = LocalDate.parse(request.getStartDate().substring(0, 10));
        LocalDate end = LocalDate.parse(request.getEndDate().substring(0, 10));

        List<String> xAxis = new ArrayList<>();
        LocalDate cursor = start.withDayOfMonth(1);
        while (!cursor.isAfter(end)) {
            xAxis.add(cursor.toString());
            cursor = cursor.plusMonths(1);
        }

        Map<Long, List<BigDecimal>> snapshotMap = buildMonthlySnapshots(accounts, accountIds, xAxis);
        Map<Long, BigDecimal> endBalanceMap = calcBalanceAtDate(accounts, accountIds, end);

        return buildPeriodResponse(request, accounts, xAxis, snapshotMap, endBalanceMap,
                start.toString(), end.toString());
    }

    /**
     * 按年统计（支持自定义开始/结束日期，按年聚合）
     */
    private FundBalanceTrendResponse calculateYearlyTrend(FundBalanceTrendRequest request,
                                                           List<FundAccount> accounts,
                                                           List<Long> accountIds) {
        LocalDate start = LocalDate.parse(request.getStartDate().substring(0, 10));
        LocalDate end = LocalDate.parse(request.getEndDate().substring(0, 10));
        int startYear = start.getYear();
        int endYear = end.getYear();

        List<YearlyBalanceDTO> yearlyList =
                mapper.selectYearlyClosedBalances(accountIds, startYear, endYear);

        log.info("[calculateYearlyTrend] 查询结果数量: {}", yearlyList.size());
        for (YearlyBalanceDTO dto : yearlyList) {
            log.info("[calculateYearlyTrend] 账户={}, 年份={}, 最后结账月={}, 期初余额={}",
                    dto.getAccountId(), dto.getYear(), dto.getClosedMonth(), dto.getInitialBalance());
        }

        Map<Long, BigDecimal> endBalanceMap = calcBalanceAtDate(accounts, accountIds, end);
        Map<String, BigDecimal> yearAccMap = new HashMap<>();
        for (YearlyBalanceDTO dto : yearlyList) {
            yearAccMap.put(dto.getYear() + ":" + dto.getAccountId(), dto.getInitialBalance());
        }

        List<String> xAxis = new ArrayList<>();
        for (int year = startYear; year <= endYear; year++) {
            xAxis.add(String.format("%d-01-01", year));
        }

        Map<Long, List<BigDecimal>> snapshotMap = new LinkedHashMap<>();
        for (FundAccount account : accounts) {
            List<BigDecimal> data = new ArrayList<>();
            BigDecimal lastKnown = BigDecimal.ZERO;
            for (int year = startYear; year <= endYear; year++) {
                BigDecimal bal = yearAccMap.get(year + ":" + account.getAccountId());
                if (bal != null) {
                    lastKnown = bal;
                }
                data.add(lastKnown);
            }
            snapshotMap.put(account.getAccountId(), data);
        }

        return buildPeriodResponse(request, accounts, xAxis, snapshotMap, endBalanceMap,
                start.toString(), end.toString());
    }

    /**
     * 为给定的月份日期点列表（YYYY-MM-01）查询各账户的余额快照。
     * 每个月的余额 = 该月期初余额 + 该月内所有资金流水净额（收入 - 支出）
     */
    private Map<Long, List<BigDecimal>> buildMonthlySnapshots(List<FundAccount> accounts,
                                                               List<Long> accountIds,
                                                               List<String> datePoints) {
        Map<Long, List<BigDecimal>> snapshotMap = new LinkedHashMap<>();
        for (FundAccount account : accounts) {
            snapshotMap.put(account.getAccountId(), new ArrayList<>());
        }

        // 一次性查询整个区间的所有日流水，避免 N+1
        String rangeStart = datePoints.get(0);
        // 区间末尾：最后一个月的最后一天
        LocalDate lastMonthStart = LocalDate.parse(datePoints.get(datePoints.size() - 1));
        String rangeEnd = lastMonthStart
                .with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()).toString();
        List<DayFlowDTO> allFlows = mapper.selectDayFlows(accountIds, rangeStart, rangeEnd);

        // 按 "accountId:YYYY-MM" 聚合月度净额
        Map<String, BigDecimal> monthlyNetMap = new HashMap<>();
        for (DayFlowDTO flow : allFlows) {
            // flow.getTransactionDate() 格式为 YYYY-MM-DD，取前7位得到 YYYY-MM
            String month = flow.getTransactionDate().substring(0, 7);
            String key = flow.getAccountId() + ":" + month;
            BigDecimal net = flow.getDayIncome().subtract(flow.getDayExpenditure());
            monthlyNetMap.merge(key, net, BigDecimal::add);
        }

        for (String date : datePoints) {
            // 查询该月期初余额
            List<AccountInitialDTO> initialList = mapper.selectInitialBalances(accountIds, date, true);
            String month = date.substring(0, 7); // YYYY-MM

            for (FundAccount account : accounts) {
                // 期初余额
                BigDecimal initialBalance = null;
                String periodStart = null;
                for (AccountInitialDTO dto : initialList) {
                    if (dto.getAccountId().equals(account.getAccountId())) {
                        initialBalance = dto.getInitialBalance();
                        periodStart = dto.getPeriodStart();
                        break;
                    }
                }
                if (initialBalance == null) {
                    initialBalance = BigDecimal.ZERO;
                }

                String effectiveMonth = periodStart != null && periodStart.length() >= 7
                        ? periodStart.substring(0, 7)
                        : month;
                BigDecimal monthlyNet = month.equals(effectiveMonth)
                        ? monthlyNetMap.getOrDefault(account.getAccountId() + ":" + month, BigDecimal.ZERO)
                        : BigDecimal.ZERO;
                BigDecimal monthBalance = initialBalance.add(monthlyNet);

                List<BigDecimal> accountSnapshots = snapshotMap.get(account.getAccountId());
                boolean isFutureFallbackMonth = periodStart != null && !month.equals(effectiveMonth);
                if (isFutureFallbackMonth && !accountSnapshots.isEmpty()) {
                    accountSnapshots.add(accountSnapshots.get(accountSnapshots.size() - 1));
                } else {
                    accountSnapshots.add(monthBalance);
                }
            }
        }
        return snapshotMap;
    }

    /**
     * 计算各账户在指定日期的实际余额（当月期初 + 当月截至该日流水）
     */
    private Map<Long, BigDecimal> calcBalanceAtDate(List<FundAccount> accounts,
                                                    List<Long> accountIds,
                                                    LocalDate endDate) {
        List<AccountInitialDTO> balanceList = mapper.selectEndDateBalances(accountIds, endDate.toString(), true);
        Map<Long, BigDecimal> balanceMap = new HashMap<>();
        for (AccountInitialDTO dto : balanceList) {
            balanceMap.put(dto.getAccountId(), dto.getInitialBalance());
        }

        Map<Long, BigDecimal> result = new HashMap<>();
        for (FundAccount account : accounts) {
            result.put(account.getAccountId(),
                    balanceMap.getOrDefault(account.getAccountId(), BigDecimal.ZERO));
        }
        return result;
    }

    /**
     * 计算各账户当前实际余额（本月期初 + 本月已发生流水）
     */
    private Map<Long, BigDecimal> calcCurrentBalance(List<FundAccount> accounts,
                                                      List<Long> accountIds) {
        return calcBalanceAtDate(accounts, accountIds, LocalDate.now());
    }

    /**
     * 通用：组装 series / summaries / response（适用于 month 和 year 维度）
     */
    private FundBalanceTrendResponse buildPeriodResponse(FundBalanceTrendRequest request,
                                                          List<FundAccount> accounts,
                                                          List<String> xAxis,
                                                          Map<Long, List<BigDecimal>> snapshotMap,
                                                          Map<Long, BigDecimal> currentBalanceMap,
                                                          String startDate,
                                                          String endDate) {
        List<BalanceTrendSeries> seriesList = new ArrayList<>();
        List<BigDecimal> totalData = buildTotalData(xAxis.size(), accounts, snapshotMap);
        seriesList.add(new BalanceTrendSeries(0L, "总余额", "TOTAL", "#409eff", totalData, null));
        for (FundAccount account : accounts) {
            seriesList.add(new BalanceTrendSeries(
                    account.getAccountId(),
                    account.getAccountName(),
                    account.getAccountType(),
                    null,
                    snapshotMap.get(account.getAccountId()),
                    null));
        }

        List<AccountBalanceSummary> summaries = new ArrayList<>();
        BigDecimal totalCurrentBalance = BigDecimal.ZERO;
        for (FundAccount account : accounts) {
            BigDecimal cur = currentBalanceMap.getOrDefault(account.getAccountId(), BigDecimal.ZERO);
            summaries.add(new AccountBalanceSummary(
                    account.getAccountId(), account.getAccountName(),
                    account.getAccountType(), cur));
            totalCurrentBalance = totalCurrentBalance.add(cur);
        }

        String computedAt = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        FundBalanceTrendResponse resp = new FundBalanceTrendResponse();
        resp.setDateRange(request.getDateRange());
        resp.setStartDate(startDate);
        resp.setEndDate(endDate);
        resp.setFromCache(false);
        resp.setComputedAt(computedAt);
        resp.setXAxis(xAxis);
        resp.setSeries(seriesList);
        resp.setAccounts(summaries);
        resp.setTotalCurrentBalance(totalCurrentBalance);
        return resp;
    }

    // ─────────────────────────── 工具方法 ───────────────────────────

    /**
     * 累积从 fromDate 到 toDate（含）之间的流水
     */
    private BigDecimal accumulateFlows(BigDecimal base, Long accountId,
                                        String fromDate, String toDate,
                                        Map<String, DayFlowDTO> flowMap) {
        if (toDate.compareTo(fromDate) < 0) return base;
        LocalDate cursor = LocalDate.parse(fromDate);
        LocalDate end    = LocalDate.parse(toDate);
        BigDecimal result = base;
        while (!cursor.isAfter(end)) {
            String key = accountId + ":" + cursor;
            DayFlowDTO flow = flowMap.get(key);
            if (flow != null) {
                result = result
                        .add(flow.getDayIncome())
                        .subtract(flow.getDayExpenditure());
            }
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    /**
     * 生成横轴日期序列（按时间维度划分粒度）
     * today: 按小时（00~23，共24个点）
     * week / month: 每天
     * quarter: 每周（周一）
     * year: 每月（月初）
     */
    private List<String> generateXAxis(String startDate, String endDate, String dateRange) {
        List<String> xAxis = new ArrayList<>();

        switch (dateRange) {
            case "today":
                // 当天 00~23 小时标签
                for (int h = 0; h < 24; h++) {
                    xAxis.add(String.format("%02d", h));
                }
                break;
            case "week":
            case "month": {
                LocalDate start = LocalDate.parse(startDate.length() > 10 ? startDate.substring(0, 10) : startDate);
                LocalDate end   = LocalDate.parse(endDate.length()   > 10 ? endDate.substring(0, 10)   : endDate);
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    xAxis.add(d.format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
                log.info("[generateXAxis] week/month - 生成 {} 个日期点，首个: {}, 末个: {}", 
                        xAxis.size(), xAxis.isEmpty() ? "N/A" : xAxis.get(0), 
                        xAxis.isEmpty() ? "N/A" : xAxis.get(xAxis.size() - 1));
                break;
            }
            case "quarter": {
                LocalDate start = LocalDate.parse(startDate.length() > 10 ? startDate.substring(0, 10) : startDate);
                LocalDate end   = LocalDate.parse(endDate.length()   > 10 ? endDate.substring(0, 10)   : endDate);
                // 季度模式改为按月显示（月初），而不是按周
                LocalDate monthCursor = start.withDayOfMonth(1);
                while (!monthCursor.isAfter(end)) {
                    xAxis.add(monthCursor.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    monthCursor = monthCursor.plusMonths(1);
                }
                // 确保末尾日期被包含
                if (!xAxis.isEmpty() && !xAxis.get(xAxis.size() - 1).equals(end.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        && end.isAfter(LocalDate.parse(xAxis.get(xAxis.size() - 1)))) {
                    xAxis.add(end.format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
                log.info("[generateXAxis] quarter - 生成 {} 个月份点，首个: {}, 末个: {}", 
                        xAxis.size(), xAxis.isEmpty() ? "N/A" : xAxis.get(0), 
                        xAxis.isEmpty() ? "N/A" : xAxis.get(xAxis.size() - 1));
                break;
            }
            case "year": {
                LocalDate start = LocalDate.parse(startDate.length() > 10 ? startDate.substring(0, 10) : startDate);
                LocalDate end   = LocalDate.parse(endDate.length()   > 10 ? endDate.substring(0, 10)   : endDate);
                LocalDate monthCursor = start.withDayOfMonth(1);
                while (!monthCursor.isAfter(end)) {
                    xAxis.add(monthCursor.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    monthCursor = monthCursor.plusMonths(1);
                }
                break;
            }
            default: {
                LocalDate start = LocalDate.parse(startDate.length() > 10 ? startDate.substring(0, 10) : startDate);
                LocalDate end   = LocalDate.parse(endDate.length()   > 10 ? endDate.substring(0, 10)   : endDate);
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    xAxis.add(d.format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
            }
        }
        return xAxis;
    }

    /**
     * 聚合各账户余额得到总余额序列
     */
    private List<BigDecimal> buildTotalData(int size, List<FundAccount> accounts,
                                              Map<Long, List<BigDecimal>> snapshotMap) {
        List<BigDecimal> total = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (FundAccount account : accounts) {
                List<BigDecimal> snap = snapshotMap.get(account.getAccountId());
                if (snap != null && i < snap.size()) {
                    sum = sum.add(snap.get(i));
                }
            }
            total.add(sum);
        }
        return total;
    }

    /**
     * 返回 date 的前一天（字符串格式 YYYY-MM-DD）
     */
    private String prevDay(String date) {
        return LocalDate.parse(date).minusDays(1).toString();
    }

    /**
     * 标准化请求：若未传 startDate/endDate，按 dateRange 推算
     * day   = 当月第1天 ~ 当月最后一天
     * month = 当年1月1日 ~ 当年12月31日
     * year  = 近5年第1天 ~ 今年12月31日
     */
    private void normalizeRequest(FundBalanceTrendRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null) return;
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;
        String range = request.getDateRange() == null ? "day" : request.getDateRange();
        switch (range) {
            case "day":
                // 当月第1天 ~ 当月最后一天
                start = today.withDayOfMonth(1);
                end = today.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
                break;
            case "month":
                // 当年1月1日 ~ 当年12月31日
                start = today.withDayOfYear(1);
                end = today.withMonth(12).withDayOfMonth(31);
                break;
            case "year":
                // 近5年第1天 ~ 今年12月31日
                start = today.minusYears(4).withDayOfYear(1);
                end = today.withMonth(12).withDayOfMonth(31);
                break;
            default:
                // 兼容旧值：today/week/quarter 退化为 day
                start = today.withDayOfMonth(1);
                end = today.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
                break;
        }
        request.setStartDate(start.toString());
        request.setEndDate(end.toString());
        if (request.getDateRange() == null) request.setDateRange("day");
    }

    /**
     * 生成缓存键
     * 格式：fund:balance-trend:{dateRange}:{startDate}:{endDate}:{accountIds_sorted_joined}
     */
    private String generateCacheKey(FundBalanceTrendRequest request) {
        String accountPart = (request.getAccountIds() == null || request.getAccountIds().isEmpty())
                ? "all"
                : request.getAccountIds().stream().sorted()
                        .map(String::valueOf).collect(Collectors.joining("_"));
        return String.format("%s:%s:%s:%s:%s",
                CACHE_KEY_PREFIX,
                request.getDateRange(),
                request.getStartDate(),
                request.getEndDate(),
                accountPart);
    }

    /**
     * 构建空响应（无可用账户时）
     */
    private FundBalanceTrendResponse buildEmptyResponse(FundBalanceTrendRequest request) {
        FundBalanceTrendResponse resp = new FundBalanceTrendResponse();
        resp.setDateRange(request.getDateRange());
        resp.setStartDate(request.getStartDate());
        resp.setEndDate(request.getEndDate());
        resp.setFromCache(false);
        resp.setComputedAt(java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        resp.setXAxis(Collections.emptyList());
        resp.setSeries(Collections.emptyList());
        resp.setAccounts(Collections.emptyList());
        resp.setTotalCurrentBalance(BigDecimal.ZERO);
        return resp;
    }
}
