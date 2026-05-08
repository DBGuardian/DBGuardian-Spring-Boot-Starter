package com.erp.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.controller.finance.dto.FundSummaryRequest;
import com.erp.controller.finance.dto.FundSummaryResponse;
import com.erp.entity.finance.FundAccount;
import com.erp.entity.finance.FundAccountGroup;
import com.erp.entity.finance.FundAccountInitialBalance;
import com.erp.entity.finance.FundPeriod;
import com.erp.entity.finance.FundTransaction;
import com.erp.mapper.finance.FundAccountGroupMapper;
import com.erp.mapper.finance.FundAccountInitialBalanceMapper;
import com.erp.mapper.finance.FundAccountMapper;
import com.erp.mapper.finance.FundPeriodMapper;
import com.erp.mapper.finance.FundTransactionMapper;
import com.erp.service.finance.FundSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 汇总表服务实现类
 */
@Service
@Slf4j
public class FundSummaryServiceImpl implements FundSummaryService {

    @Autowired
    private FundAccountMapper fundAccountMapper;

    @Autowired
    private FundAccountGroupMapper fundAccountGroupMapper;

    @Autowired
    private FundPeriodMapper fundPeriodMapper;

    @Autowired
    private FundAccountInitialBalanceMapper initialBalanceMapper;

    @Autowired
    private FundTransactionMapper transactionMapper;
    
    @Autowired
    private com.erp.service.finance.FundOrganizationService fundOrganizationService;
    
    @Autowired
    private com.erp.mapper.finance.FundSubjectMapper fundSubjectMapper;
    
    @Autowired
    private com.erp.mapper.finance.FundSubjectInitialBalanceMapper subjectInitialBalanceMapper;

    @Override
    public FundSummaryResponse getSummary(FundSummaryRequest request) {
        // 1. 确定要查询的账户ID列表（按组织聚合；不再需要外部传入 accountIds/groupId）
        if (request.getOrganizationId() == null) {
            throw new RuntimeException("必须指定组织ID（organizationId）用于汇总");
        }
        List<Long> accountIds = fundOrganizationService.getOrganizationAccountIds(request.getOrganizationId());
        if (accountIds == null || accountIds.isEmpty()) {
            throw new RuntimeException("组织下没有账户可用于汇总，organizationId：" + request.getOrganizationId());
        }

        // 2. 查询账期信息
        FundPeriod period = null;
        if (request.getPeriodId() != null) {
            period = fundPeriodMapper.selectById(request.getPeriodId());
            // 如果通过 periodId 找不到账期，抛出异常而不是回退查询
            if (period == null) {
                throw new RuntimeException("账期不存在，periodId：" + request.getPeriodId());
            }
        } else if (request.getYear() != null && request.getQuarter() != null) {
            // 将季度转换为对应的最后一个月份（例如 1 -> 3）
            int targetMonth = request.getQuarter() * 3;
            QueryWrapper<FundPeriod> periodQuery = new QueryWrapper<>();
            periodQuery.eq("年份", request.getYear());
            periodQuery.eq("月份", targetMonth);
            periodQuery.eq("组织编号", request.getOrganizationId()); // 添加组织ID过滤
            period = fundPeriodMapper.selectOne(periodQuery);
        }

        if (period == null) {
            throw new RuntimeException("账期不存在");
        }

        // 3. 计算当前 period 所属季度的首月（用于期初查询：Q1->1月，Q2->4月...）
        int selMonth = period.getMonth();
        int quarter = (selMonth - 1) / 3 + 1;
        int startMonth = (quarter - 1) * 3 + 1;

        // 4. 查询每个账户的汇总数据（优化：批量查询账户期初余额）
        List<FundSummaryResponse.AccountSummaryInfo> accountSummaries = new ArrayList<>();

        // 批量查询账户期初余额
        List<Long> relevantPeriodIds = getRelevantPeriodIds(request.getOrganizationId(), period);
        java.util.Map<Long, FundAccountInitialBalance> accountInitialBalances = new java.util.HashMap<>();
        if (!relevantPeriodIds.isEmpty() && !accountIds.isEmpty()) {
            try {
                List<FundAccountInitialBalance> accountInitList = initialBalanceMapper.selectBatchNearestAccountInitialBalances(
                        request.getOrganizationId(),
                        relevantPeriodIds,
                        accountIds
                );
                for (FundAccountInitialBalance aib : accountInitList) {
                    accountInitialBalances.put(aib.getAccountId(), aib);
                }
            } catch (Exception ex) {
                log.warn("批量查询账户期初余额失败，使用默认值", ex);
            }
        }

        for (Long accountId : accountIds) {
            FundAccount account = fundAccountMapper.selectById(accountId);
            if (account == null) {
                continue; // 跳过不存在的账户
            }

            FundSummaryResponse.AccountSummaryInfo summary = calculateAccountSummary(account, period, accountInitialBalances.get(account.getAccountId()));

            // 使用批量查询结果设置期初余额
            FundAccountInitialBalance nearestAccountInit = accountInitialBalances.get(account.getAccountId());
            FundSummaryResponse.InitialBalanceInfo ib = new FundSummaryResponse.InitialBalanceInfo();
            if (nearestAccountInit != null) {
                ib.setAmount(nearestAccountInit.getInitialBalance());
                ib.setDirection(nearestAccountInit.getBalanceDirection());
            } else {
                ib.setAmount(BigDecimal.ZERO);
                ib.setDirection("收入");
            }
            summary.setInitialBalance(ib);

            accountSummaries.add(summary);
        }

        // 4. 构建响应
        FundSummaryResponse response = new FundSummaryResponse();
        response.setPeriodId(period.getPeriodId());
        response.setPeriodCode(period.getPeriodCode());
        response.setYear(period.getYear());
        response.setMonth(period.getMonth());
        response.setAccounts(accountSummaries);
        // 5. 按科目聚合（按月，排除内部往来）
        // 计算季度月份范围（基于 period.month 所在季度）
        selMonth = period.getMonth();
        quarter = (selMonth - 1) / 3 + 1;
        startMonth = (quarter - 1) * 3 + 1;
        List<Integer> quarterMonths = new ArrayList<>();
        quarterMonths.add(startMonth);
        quarterMonths.add(startMonth + 1);
        quarterMonths.add(startMonth + 2);

        // 计算季度范围（使用 startMonth/startMonth+2 的整月范围，而非单个 period 的 start/end）
        java.time.LocalDate quarterStartDate = LocalDate.of(period.getYear(), startMonth, 1);
        java.time.YearMonth ym = java.time.YearMonth.of(period.getYear(), startMonth + 2);
        java.time.LocalDate quarterEndDate = ym.atEndOfMonth();

        // 查询年初到年底的日期范围（用于年度累计按科目，无论当前季度都计算到12月31日）
        LocalDate yearStart = LocalDate.of(period.getYear(), 1, 1);
        LocalDate yearEnd = LocalDate.of(period.getYear(), 12, 31);

        // 一次性查询所有需要的流水数据（连表查询科目信息，合并三个查询为一个）
        List<com.erp.controller.finance.dto.FundSummaryTransactionDTO> allTransactions =
                transactionMapper.selectFundSummaryTransactions(accountIds, quarterStartDate, quarterEndDate, yearStart, yearEnd);

        // 在Java中按日期范围分类数据
        List<com.erp.controller.finance.dto.FundSummaryTransactionDTO> quarterTransactions = new ArrayList<>();
        List<com.erp.controller.finance.dto.FundSummaryTransactionDTO> preTransactions = new ArrayList<>();
        List<com.erp.controller.finance.dto.FundSummaryTransactionDTO> yearTransactions = new ArrayList<>();

        for (com.erp.controller.finance.dto.FundSummaryTransactionDTO tx : allTransactions) {
            LocalDate txDate = tx.getTransactionDate();
            if (txDate.compareTo(quarterStartDate) >= 0 && txDate.compareTo(quarterEndDate) <= 0) {
                quarterTransactions.add(tx);
            } else if (txDate.compareTo(yearStart) >= 0 && txDate.compareTo(quarterStartDate) < 0) {
                preTransactions.add(tx);
            }
            // 所有在年范围内的数据都加入yearTransactions
            if (txDate.compareTo(yearStart) >= 0 && txDate.compareTo(yearEnd) <= 0) {
                yearTransactions.add(tx);
            }
        }

        // 从查询结果中提取科目信息（避免重复查询）
        java.util.Set<Long> subjectIds = allTransactions.stream()
                .filter(tx -> tx.getSubjectId() != null)
                .map(com.erp.controller.finance.dto.FundSummaryTransactionDTO::getSubjectId)
                .collect(Collectors.toSet());

        // 构建科目映射（从DTO中直接提取，避免重复查询）
        java.util.Map<Long, com.erp.controller.finance.dto.FundSummaryTransactionDTO> subjectMap = allTransactions.stream()
                .filter(tx -> tx.getSubjectId() != null)
                .collect(Collectors.toMap(
                        com.erp.controller.finance.dto.FundSummaryTransactionDTO::getSubjectId,
                        tx -> tx,
                        (existing, replacement) -> existing // 如果有重复，保留第一个
                ));

        // 按科目聚合结构
        java.util.Map<Long, java.util.Map<String, Object>> subjectRows = new java.util.LinkedHashMap<>();
        // 存放按月的净额小计（收入组为正数，支出组为正数）
        java.util.Map<String, BigDecimal> incomeSubtotal = new java.util.HashMap<>();
        java.util.Map<String, BigDecimal> expenditureSubtotal = new java.util.HashMap<>();
        BigDecimal openingBalanceTotal = BigDecimal.ZERO;
        BigDecimal closingBalanceTotal = BigDecimal.ZERO;
        // 所有科目的年度合计总额（用于“账户余额”行的年度合计列）
        BigDecimal accountYearTotal = BigDecimal.ZERO;

        // 初始化 subjectRows entries for each subjectId
        for (Long sid : subjectIds) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            com.erp.controller.finance.dto.FundSummaryTransactionDTO subj = subjectMap.get(sid);
            if (subj != null) {
                row.put("subjectCode", subj.getSubjectCode());
                row.put("subjectName", subj.getSubjectName());
                row.put("level1", subj.getSubjectCategory());
                row.put("level2", subj.getFullSubjectName());
                row.put("balanceDirection", subj.getBalanceDirection());
            } else {
                row.put("subjectCode", "");
                row.put("subjectName", "");
            }
            // init month fields
            for (Integer m : quarterMonths) {
                row.put("m" + m + "_income", BigDecimal.ZERO);
                row.put("m" + m + "_expenditure", BigDecimal.ZERO);
            }
            row.put("quarterIncome", BigDecimal.ZERO);
            row.put("quarterExpenditure", BigDecimal.ZERO);
            row.put("quarterTotal", BigDecimal.ZERO);
            row.put("openingBalance", BigDecimal.ZERO);
            row.put("closingBalance", BigDecimal.ZERO);
            subjectRows.put(sid, row);
        }

        // 处理季度内流水，按月份与方向累加
        for (com.erp.controller.finance.dto.FundSummaryTransactionDTO tx : quarterTransactions) {
            Long sid = tx.getSubjectId();
            if (sid == null) continue;
            java.util.Map<String, Object> row = subjectRows.get(sid);
            if (row == null) {
                // 新建行（防护）
                row = new java.util.LinkedHashMap<>();
                row.put("subjectCode", tx.getSubjectCode() != null ? tx.getSubjectCode() : "");
                row.put("subjectName", tx.getSubjectName() != null ? tx.getSubjectName() : "");
                for (Integer m : quarterMonths) {
                    row.put("m" + m + "_income", BigDecimal.ZERO);
                    row.put("m" + m + "_expenditure", BigDecimal.ZERO);
                }
                row.put("quarterIncome", BigDecimal.ZERO);
                row.put("quarterExpenditure", BigDecimal.ZERO);
                row.put("quarterTotal", BigDecimal.ZERO);
                row.put("openingBalance", BigDecimal.ZERO);
                row.put("closingBalance", BigDecimal.ZERO);
                subjectRows.put(sid, row);
            }
            int m = tx.getTransactionDate().getMonthValue();
            String incomeKey = "m" + m + "_income";
            String expKey = "m" + m + "_expenditure";
            if ("INCOME".equals(tx.getTransactionType())) {
                BigDecimal prev = (BigDecimal) row.getOrDefault(incomeKey, BigDecimal.ZERO);
                BigDecimal now = prev.add(tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount());
                row.put(incomeKey, now);
            } else {
                BigDecimal prev = (BigDecimal) row.getOrDefault(expKey, BigDecimal.ZERO);
                BigDecimal now = prev.add(tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount());
                row.put(expKey, now);
            }
        }

        // 计算季度汇总与 opening balances
        java.util.Map<Long, BigDecimal> openingBySubject = new java.util.HashMap<>();

        try {
            // 优先从季度首月对应的账期读取科目级期初余额
            FundPeriod quarterStartPeriod = null;
            QueryWrapper<FundPeriod> startPeriodQuery = new QueryWrapper<>();
            startPeriodQuery.eq("年份", period.getYear());
            startPeriodQuery.eq("月份", startMonth);
            startPeriodQuery.eq("组织编号", request.getOrganizationId());
            quarterStartPeriod = fundPeriodMapper.selectOne(startPeriodQuery);

            if (quarterStartPeriod != null) {
                // 查询季度首月账期的科目期初余额
                List<com.erp.entity.finance.FundSubjectInitialBalance> subjInitList =
                        subjectInitialBalanceMapper.selectList(
                                new QueryWrapper<com.erp.entity.finance.FundSubjectInitialBalance>()
                                        .eq("账期编号", quarterStartPeriod.getPeriodId())
                                        .eq("组织编号", request.getOrganizationId())
                                        .isNull("账户编号")
                        );

                // 构建科目期初余额映射
                for (com.erp.entity.finance.FundSubjectInitialBalance sib : subjInitList) {
                    if (sib.getSubjectId() != null && subjectIds.contains(sib.getSubjectId())) {
                        openingBySubject.put(sib.getSubjectId(),
                                sib.getInitialBalance() == null ? BigDecimal.ZERO : sib.getInitialBalance());
                    }
                }
            }

            // 对于没有在季度首月账期找到期初余额的科目，尝试向前查找最近的期初
            for (Long subjectId : subjectIds) {
                if (openingBySubject.containsKey(subjectId)) {
                    continue; // 已经找到了，跳过
                }

                // 向前查找最近的科目期初
                BigDecimal found = findNearestPreviousSubjectInitial(subjectId, request.getOrganizationId(), period);
                openingBySubject.put(subjectId, found != null ? found : BigDecimal.ZERO);
            }
        } catch (Exception ex) {
            // 如果读取科目期初余额失败，记录警告并保持科目期初为默认 0
            log.warn("读取 FUND_SUBJECT_INITIAL_BALANCE 失败，科目期初将使用默认 0", ex);
            // 设置所有科目的期初余额为0
            for (Long subjectId : subjectIds) {
                openingBySubject.put(subjectId, BigDecimal.ZERO);
            }
        }

        // 为没有找到期初余额的科目设置默认值 0
        for (Long sid : subjectIds) {
            openingBySubject.putIfAbsent(sid, BigDecimal.ZERO);
        }

        // 计算年度累计按科目
        java.util.Map<Long, BigDecimal> yearIncomeBySubject = new java.util.HashMap<>();
        java.util.Map<Long, BigDecimal> yearExpBySubject = new java.util.HashMap<>();
        for (com.erp.controller.finance.dto.FundSummaryTransactionDTO tx : yearTransactions) {
            Long sid = tx.getSubjectId();
            if (sid == null) continue;
            if ("INCOME".equals(tx.getTransactionType())) {
                yearIncomeBySubject.put(sid, yearIncomeBySubject.getOrDefault(sid, BigDecimal.ZERO).add(tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount()));
            } else {
                yearExpBySubject.put(sid, yearExpBySubject.getOrDefault(sid, BigDecimal.ZERO).add(tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount()));
            }
        }

        // finalize rows and subtotals, and group by subject direction
        List<Map<String, Object>> incomeRows = new ArrayList<>();
        List<Map<String, Object>> expenditureRows = new ArrayList<>();
        List<Map<String, Object>> otherRowsList = new ArrayList<>();

        for (java.util.Map.Entry<Long, java.util.Map<String, Object>> entry : subjectRows.entrySet()) {
            java.util.Map<String, Object> row = entry.getValue();
            BigDecimal quarterIncome = BigDecimal.ZERO;
            BigDecimal quarterExp = BigDecimal.ZERO;
            for (Integer m : quarterMonths) {
                BigDecimal inc = (BigDecimal) row.getOrDefault("m" + m + "_income", BigDecimal.ZERO);
                BigDecimal exp = (BigDecimal) row.getOrDefault("m" + m + "_expenditure", BigDecimal.ZERO);
                BigDecimal net = inc.subtract(exp);
                // put net column for month
                row.put("m" + m, net);
                // accumulate subtotals based on subject net:
                // compute net = inc - exp; if net >= 0, add net to income subtotal; else add abs(net) to expenditure subtotal
                if (net.compareTo(BigDecimal.ZERO) >= 0) {
                    incomeSubtotal.put("m" + m + "_income", incomeSubtotal.getOrDefault("m" + m + "_income", BigDecimal.ZERO).add(net));
                    // ensure expenditure key exists (zero) for consistency
                    expenditureSubtotal.putIfAbsent("m" + m + "_expenditure", BigDecimal.ZERO);
                } else {
                    BigDecimal absNet = net.abs();
                    expenditureSubtotal.put("m" + m + "_expenditure", expenditureSubtotal.getOrDefault("m" + m + "_expenditure", BigDecimal.ZERO).add(absNet));
                    // ensure income key exists (zero) for consistency
                    incomeSubtotal.putIfAbsent("m" + m + "_income", BigDecimal.ZERO);
                }
                quarterIncome = quarterIncome.add(inc);
                quarterExp = quarterExp.add(exp);
            }
            BigDecimal quarterTotal = quarterIncome.subtract(quarterExp);
            row.put("quarterIncome", quarterIncome);
            row.put("quarterExpenditure", quarterExp);
            row.put("quarterTotal", quarterTotal);
            // opening balance
            BigDecimal opening = openingBySubject.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            row.put("openingBalance", opening);
            BigDecimal closing = opening.add(quarterTotal);
            row.put("closingBalance", closing);
            // year totals
            BigDecimal yInc = yearIncomeBySubject.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal yExp = yearExpBySubject.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            row.put("yearIncome", yInc);
            row.put("yearExpenditure", yExp);
            BigDecimal yearTotal = yInc.subtract(yExp);
            row.put("yearTotal", yearTotal);

            openingBalanceTotal = openingBalanceTotal.add(opening);
            closingBalanceTotal = closingBalanceTotal.add(closing);
            // 累加所有科目的年度合计，用于账户余额行的年度合计列
            accountYearTotal = accountYearTotal.add(yearTotal);

            // group by quarter total sign: if quarterTotal >= 0 -> income group; else -> expenditure group
            if (quarterTotal.compareTo(BigDecimal.ZERO) >= 0) {
                incomeRows.add(row);
            } else {
                expenditureRows.add(row);
            }
        }

        // build subtotal rows
        java.util.Map<String, Object> incomeSubtotalRow = new java.util.LinkedHashMap<>();
        incomeSubtotalRow.put("subjectCode", "");
        incomeSubtotalRow.put("subjectName", "收入小计");
        BigDecimal totalQuarterIncome = BigDecimal.ZERO;
        for (Integer m : quarterMonths) {
            BigDecimal inc = incomeSubtotal.getOrDefault("m" + m + "_income", BigDecimal.ZERO);
            incomeSubtotalRow.put("m" + m + "_income", inc);
            incomeSubtotalRow.put("m" + m + "_expenditure", BigDecimal.ZERO);
            totalQuarterIncome = totalQuarterIncome.add(inc);
        }
        incomeSubtotalRow.put("quarterIncome", totalQuarterIncome);
        incomeSubtotalRow.put("quarterExpenditure", BigDecimal.ZERO);
        incomeSubtotalRow.put("quarterTotal", totalQuarterIncome);

        java.util.Map<String, Object> expenditureSubtotalRow = new java.util.LinkedHashMap<>();
        expenditureSubtotalRow.put("subjectCode", "");
        expenditureSubtotalRow.put("subjectName", "支出小计");
        BigDecimal totalQuarterExp = BigDecimal.ZERO;
        for (Integer m : quarterMonths) {
            BigDecimal exp = expenditureSubtotal.getOrDefault("m" + m + "_expenditure", BigDecimal.ZERO);
            expenditureSubtotalRow.put("m" + m + "_income", BigDecimal.ZERO);
            expenditureSubtotalRow.put("m" + m + "_expenditure", exp);
            totalQuarterExp = totalQuarterExp.add(exp);
        }
        expenditureSubtotalRow.put("quarterIncome", BigDecimal.ZERO);
        expenditureSubtotalRow.put("quarterExpenditure", totalQuarterExp);
        expenditureSubtotalRow.put("quarterTotal", totalQuarterExp.negate());

        // assemble final rows: other, income, income subtotal, expenditure, expenditure subtotal
        List<Map<String, Object>> finalRows = new ArrayList<>();
        finalRows.addAll(otherRowsList);
        finalRows.addAll(incomeRows);
        // always include income subtotal row (even if zero) to make UI consistent
        finalRows.add(incomeSubtotalRow);
        finalRows.addAll(expenditureRows);
        // always include expenditure subtotal row (even if zero) placed after income subtotal / expenditure rows
        finalRows.add(expenditureSubtotalRow);

        FundSummaryResponse.AccountBalance ab = new FundSummaryResponse.AccountBalance();
        ab.setOpeningBalance(openingBalanceTotal);
        ab.setClosingBalance(closingBalanceTotal);

        // append account balance row as final summary row (also returned via accountBalance)
        java.util.Map<String, Object> accountBalanceRow = new java.util.LinkedHashMap<>();
        accountBalanceRow.put("subjectCode", "");
        accountBalanceRow.put("subjectName", "账户余额");
        accountBalanceRow.put("openingBalance", ab.getOpeningBalance());
        accountBalanceRow.put("closingBalance", ab.getClosingBalance());
        // set per-month account balance = total income - total expenditure
        BigDecimal quarterIncomeTotal = BigDecimal.ZERO;
        BigDecimal quarterExpenditureTotal = BigDecimal.ZERO;
        for (Integer m : quarterMonths) {
            BigDecimal inc = incomeSubtotal.getOrDefault("m" + m + "_income", BigDecimal.ZERO);
            BigDecimal exp = expenditureSubtotal.getOrDefault("m" + m + "_expenditure", BigDecimal.ZERO);
            accountBalanceRow.put("m" + m, inc.subtract(exp));
            quarterIncomeTotal = quarterIncomeTotal.add(inc);
            quarterExpenditureTotal = quarterExpenditureTotal.add(exp);
        }
        accountBalanceRow.put("quarterIncome", quarterIncomeTotal);
        accountBalanceRow.put("quarterExpenditure", quarterExpenditureTotal);
        accountBalanceRow.put("quarterTotal", quarterIncomeTotal.subtract(quarterExpenditureTotal));
        // 账户余额行的年度合计 = 所有科目的年度合计之和
        accountBalanceRow.put("yearTotal", accountYearTotal);
        finalRows.add(accountBalanceRow);

        response.setRows(finalRows);
        response.setIncomeSubtotal(incomeSubtotal);
        response.setExpenditureSubtotal(expenditureSubtotal);
        response.setAccountBalance(ab);

        return response;
    }

    /**
     * 计算单个账户的汇总数据
     */
    private FundSummaryResponse.AccountSummaryInfo calculateAccountSummary(FundAccount account, FundPeriod period, FundAccountInitialBalance accountInitialBalance) {
        FundSummaryResponse.AccountSummaryInfo summary = new FundSummaryResponse.AccountSummaryInfo();
        summary.setAccountId(account.getAccountId());
        summary.setAccountCode(account.getAccountCode());
        summary.setAccountName(account.getAccountName());

        // 1. 使用传入的期初余额数据
        FundSummaryResponse.InitialBalanceInfo initialBalance = new FundSummaryResponse.InitialBalanceInfo();
        if (accountInitialBalance != null) {
            initialBalance.setAmount(accountInitialBalance.getInitialBalance());
            initialBalance.setDirection(accountInitialBalance.getBalanceDirection());
        } else {
            initialBalance.setAmount(BigDecimal.ZERO);
            initialBalance.setDirection("收入");
        }
        summary.setInitialBalance(initialBalance);

        // 2. 计算本期合计
        QueryWrapper<FundTransaction> periodTransactionQuery = new QueryWrapper<>();
        periodTransactionQuery.eq("账户编号", account.getAccountId());
        periodTransactionQuery.ge("交易日期", period.getStartDate());
        periodTransactionQuery.le("交易日期", period.getEndDate());
        List<FundTransaction> periodTransactions = transactionMapper.selectList(periodTransactionQuery);

        BigDecimal periodIncome = periodTransactions.stream()
                .filter(tx -> "INCOME".equals(tx.getTransactionType()))
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal periodExpenditure = periodTransactions.stream()
                .filter(tx -> "EXPENDITURE".equals(tx.getTransactionType()))
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal periodFinalBalance = initialBalance.getAmount().add(periodIncome).subtract(periodExpenditure);
        String periodFinalDirection = periodFinalBalance.compareTo(BigDecimal.ZERO) >= 0 ? "收入" : "支出";

        FundSummaryResponse.PeriodTotalInfo periodTotal = new FundSummaryResponse.PeriodTotalInfo();
        periodTotal.setIncome(periodIncome);
        periodTotal.setExpenditure(periodExpenditure);
        periodTotal.setDirection(periodFinalDirection);
        periodTotal.setBalance(periodFinalBalance.abs());
        summary.setPeriodTotal(periodTotal);

        // 3. 计算本年累计（全年累计到12月31日）
        LocalDate yearStart = LocalDate.of(period.getYear(), 1, 1);
        LocalDate yearEnd = LocalDate.of(period.getYear(), 12, 31);
        QueryWrapper<FundTransaction> yearTransactionQuery = new QueryWrapper<>();
        yearTransactionQuery.eq("账户编号", account.getAccountId());
        yearTransactionQuery.ge("交易日期", yearStart);
        yearTransactionQuery.le("交易日期", yearEnd);
        List<FundTransaction> yearTransactions = transactionMapper.selectList(yearTransactionQuery);

        BigDecimal yearIncome = yearTransactions.stream()
                .filter(tx -> "INCOME".equals(tx.getTransactionType()))
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal yearExpenditure = yearTransactions.stream()
                .filter(tx -> "EXPENDITURE".equals(tx.getTransactionType()))
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算年初期初余额（如果有第一个账期的期初余额）
        BigDecimal yearInitialBalance = BigDecimal.ZERO;
        QueryWrapper<FundPeriod> firstPeriodQuery = new QueryWrapper<>();
        firstPeriodQuery.eq("年份", period.getYear());
        firstPeriodQuery.eq("月份", 1);
        firstPeriodQuery.eq("组织编号", period.getOrganizationId()); // 添加组织ID过滤
        FundPeriod firstPeriod = fundPeriodMapper.selectOne(firstPeriodQuery);
        if (firstPeriod != null) {
            FundAccountInitialBalance firstPeriodBalance = initialBalanceMapper.selectByAccountAndPeriod(
                    account.getAccountId(), firstPeriod.getPeriodId());
            if (firstPeriodBalance != null) {
                yearInitialBalance = firstPeriodBalance.getInitialBalance();
            }
        }

        BigDecimal yearFinalBalance = yearInitialBalance.add(yearIncome).subtract(yearExpenditure);
        String yearFinalDirection = yearFinalBalance.compareTo(BigDecimal.ZERO) >= 0 ? "收入" : "支出";

        FundSummaryResponse.YearTotalInfo yearTotal = new FundSummaryResponse.YearTotalInfo();
        yearTotal.setIncome(yearIncome);
        yearTotal.setExpenditure(yearExpenditure);
        yearTotal.setDirection(yearFinalDirection);
        yearTotal.setBalance(yearFinalBalance.abs());
        summary.setYearTotal(yearTotal);

        return summary;
    }

    /**
     * 解析账户ID列表（JSON格式）
     */
    private List<Long> parseAccountIds(String accountIdsJson) {
        if (StrUtil.isBlank(accountIdsJson)) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(accountIdsJson, Long.class);
        } catch (Exception e) {
            // 如果JSON解析失败，尝试简单解析
            List<Long> accountIds = new ArrayList<>();
            String cleaned = accountIdsJson.trim().replaceAll("[\\[\\]\\s]", "");
            if (!cleaned.isEmpty()) {
                String[] ids = cleaned.split(",");
                for (String id : ids) {
                    try {
                        accountIds.add(Long.parseLong(id.trim()));
                    } catch (NumberFormatException ex) {
                        // 忽略无效的ID
                    }
                }
            }
            return accountIds;
        }
    }



    /**
     * 获取上一个账期（按组织过滤）
     */
    private FundPeriod getPreviousPeriod(FundPeriod currentPeriod) {
        int prevYear = currentPeriod.getYear();
        int prevMonth = currentPeriod.getMonth() - 1;
        if (prevMonth < 1) {
            prevYear -= 1;
            prevMonth = 12;
        }
        QueryWrapper<FundPeriod> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("年份", prevYear);
        queryWrapper.eq("月份", prevMonth);
        queryWrapper.eq("组织编号", currentPeriod.getOrganizationId());
        List<FundPeriod> periods = fundPeriodMapper.selectList(queryWrapper);
        if (periods.isEmpty()) return null;
        return periods.get(0);
    }

    /**
     * 获取相关的账期ID列表（从当前期向前查找，用于批量查询期初余额）
     */
    private List<Long> getRelevantPeriodIds(Long organizationId, FundPeriod currentPeriod) {
        List<Long> periodIds = new ArrayList<>();

        // 从当前季度开始向前查找最多20个账期（约5年）
        FundPeriod cursor = currentPeriod;
        int guard = 20;

        while (cursor != null && guard-- > 0) {
            periodIds.add(cursor.getPeriodId());
            cursor = getPreviousPeriod(cursor);
        }

        return periodIds;
    }

    /**
     * 向前查找最近的科目级期初余额（从当前 period 向前逐期查找），找不到返回 null
     */
    private BigDecimal findNearestPreviousSubjectInitial(Long subjectId, Long organizationId, FundPeriod period) {
        // 首先定位本季度的第一个月份（例如：1月、4月、7月、10月）
        int selMonth = period.getMonth();
        int quarter = (selMonth - 1) / 3 + 1;
        int startMonth = (quarter - 1) * 3 + 1;

        // 先尝试查找本年本季度起始月份对应的账期
        QueryWrapper<FundPeriod> startPeriodQuery = new QueryWrapper<>();
        startPeriodQuery.eq("年份", period.getYear());
        startPeriodQuery.eq("月份", startMonth);
        startPeriodQuery.eq("组织编号", period.getOrganizationId());
        FundPeriod startPeriod = fundPeriodMapper.selectOne(startPeriodQuery);

        // 如果找不到本季度首月对应的账期，从该首月之前开始向前查找最近的账期（不要以当前 period 为起点）
        FundPeriod cursor = startPeriod;
        if (cursor == null) {
            cursor = getPreviousPeriodByYearMonth(period.getYear(), startMonth, period.getOrganizationId());
        }

        while (cursor != null) {
            com.erp.entity.finance.FundSubjectInitialBalance sib = subjectInitialBalanceMapper.selectOne(
                    new QueryWrapper<com.erp.entity.finance.FundSubjectInitialBalance>()
                            .eq("组织编号", organizationId)
                            .eq("账期编号", cursor.getPeriodId())
                            .eq("科目编号", subjectId)
                            .eq("账户编号", (Object) null)
            );
            if (sib != null) {
                return sib.getInitialBalance() == null ? BigDecimal.ZERO : sib.getInitialBalance();
            }
            cursor = getPreviousPeriod(cursor);
        }
        return null;
    }

    /**
     * 从指定的 year 和 month 开始，向前查找同组织的第一个存在的账期（不包含指定 year/month 本身），找不到返回 null
     */
    private FundPeriod getPreviousPeriodByYearMonth(int year, int month, Long organizationId) {
        int y = year;
        int m = month - 1; // 从指定月的前一个月开始查找
        // 防护：避免无限循环，限定向前最多查 240 个月（20 年）
        int guard = 240;
        while (guard-- > 0) {
            if (m < 1) {
                y -= 1;
                m = 12;
            }
            QueryWrapper<FundPeriod> q = new QueryWrapper<>();
            q.eq("年份", y);
            q.eq("月份", m);
            q.eq("组织编号", organizationId);
            FundPeriod p = fundPeriodMapper.selectOne(q);
            if (p != null) return p;
            m -= 1;
        }
        return null;
    }
}

