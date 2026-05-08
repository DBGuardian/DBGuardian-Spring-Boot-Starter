package com.erp.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.settlement.dto.*;
import com.erp.entity.finance.FundAccount;
import com.erp.entity.finance.FundAccountInitialBalance;
import com.erp.entity.finance.FundPeriod;
import com.erp.entity.finance.FundSettlementCheck;
import com.erp.entity.finance.FundSettlementCheckItem;
import com.erp.entity.finance.FundTransaction;
import com.erp.mapper.finance.FundAccountInitialBalanceMapper;
import com.erp.mapper.finance.FundAccountMapper;
import com.erp.mapper.finance.FundPeriodMapper;
// import com.erp.mapper.finance.FundSettlementCheckItemMapper; // 暂时不使用，因为 FUND_SETTLEMENT_CHECK_ITEM 表不存在
import com.erp.mapper.finance.FundSettlementCheckMapper;
import com.erp.mapper.finance.FundTransactionMapper;
import com.erp.service.finance.FundSettlementService;
import com.erp.service.system.ILogRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 结账服务实现类
 */
@Service
@Slf4j
public class FundSettlementServiceImpl implements FundSettlementService {

    @Autowired
    private FundPeriodMapper fundPeriodMapper;

    @Autowired
    private FundAccountMapper fundAccountMapper;

    @Autowired
    private FundAccountInitialBalanceMapper initialBalanceMapper;

    @Autowired
    private FundTransactionMapper transactionMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private com.erp.mapper.finance.FundSubjectInitialBalanceMapper subjectInitialBalanceMapper;

    @Autowired
    private FundSettlementCheckMapper settlementCheckMapper;

    // 暂时不使用检查项Mapper，因为 FUND_SETTLEMENT_CHECK_ITEM 表不存在
    // @Autowired
    // private FundSettlementCheckItemMapper checkItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementCheckAndSettleResponse checkAndSettle(SettlementCheckAndSettleRequest request) {
        log.info("开始检查并结账，organizationId={}", request.getOrganizationId());

        // 1. 查询组织的所有未结账账期
        QueryWrapper<FundPeriod> periodQuery = new QueryWrapper<>();
        periodQuery.eq("组织编号", request.getOrganizationId())
                  .eq("是否已结账", false)
                  .orderByAsc("年份", "月份");
        List<FundPeriod> unsettledPeriods = fundPeriodMapper.selectList(periodQuery);

        log.info("查询到未结账账期数量：{}", unsettledPeriods.size());
        for (FundPeriod period : unsettledPeriods) {
            log.info("账期：{} (ID={})", period.getPeriodCode(), period.getPeriodId());
        }

        if (unsettledPeriods.isEmpty()) {
            throw new RuntimeException("该组织没有未结账的账期");
        }

        // 2. 获取该组织的所有启用的账户
        QueryWrapper<FundAccount> accountQuery = new QueryWrapper<>();
        accountQuery.eq("是否启用", true)
                   .eq("组织编号", request.getOrganizationId());
        List<FundAccount> accounts = fundAccountMapper.selectList(accountQuery);

        log.info("查询到启用的账户数量：{}", accounts.size());
        for (FundAccount account : accounts) {
            log.info("账户：{} (ID={})", account.getAccountName(), account.getAccountId());
        }

            if (accounts.isEmpty()) {
                log.warn("该组织没有启用的账户，返回空结果");
                // 如果该组织没有启用的账户，返回空的检查结果（前端显示暂无数据）
                SettlementCheckAndSettleResponse emptyResponse = new SettlementCheckAndSettleResponse();
                emptyResponse.setOrganizationId(request.getOrganizationId());
                emptyResponse.setCheckResults(new ArrayList<>());
                return emptyResponse;
            }

        // 3. 获取启用的检查项（目前使用默认检查项，不依赖数据库表）
        List<FundSettlementCheckItem> enabledCheckItems = getDefaultCheckItems();

        // 4. 对每个未结账账期的每个账户进行检查
        List<SettlementCheckAndSettleResponse.CheckResult> checkResults = new ArrayList<>();

        log.info("开始检查账户余额，共{}个账期，{}个账户", unsettledPeriods.size(), accounts.size());

        for (FundPeriod period : unsettledPeriods) {
            log.info("检查账期：{} (ID={})", period.getPeriodCode(), period.getPeriodId());

            // 收集账户ID
            List<Long> accountIds = accounts.stream()
                    .map(FundAccount::getAccountId)
                    .collect(Collectors.toList());

            // 1) 一次性查询本期所有账户的期初余额
            QueryWrapper<FundAccountInitialBalance> ibQuery = new QueryWrapper<>();
            ibQuery.eq("账期编号", period.getPeriodId());
            ibQuery.in("账户编号", accountIds);
            List<FundAccountInitialBalance> initialBalances = initialBalanceMapper.selectList(ibQuery);
            Map<Long, BigDecimal> accountIdToInitial = initialBalances.stream()
                    .collect(Collectors.toMap(FundAccountInitialBalance::getAccountId,
                            ib -> ib.getInitialBalance() == null ? BigDecimal.ZERO : ib.getInitialBalance()));

            // 2) 一次性查询本期所有账户的流水（按账户分组）并在内存中聚合收入/支出
            QueryWrapper<FundTransaction> txQuery = new QueryWrapper<>();
            txQuery.in("账户编号", accountIds)
                    .ge("交易日期", period.getStartDate())
                    .le("交易日期", period.getEndDate());
            List<FundTransaction> transactions = transactionMapper.selectList(txQuery);

            Map<Long, BigDecimal> accountIncomeMap = new HashMap<>();
            Map<Long, BigDecimal> accountExpenditureMap = new HashMap<>();

            for (FundTransaction tx : transactions) {
                Long accId = tx.getAccountId();
                BigDecimal amt = tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount();
                if ("INCOME".equals(tx.getTransactionType())) {
                    accountIncomeMap.put(accId, accountIncomeMap.getOrDefault(accId, BigDecimal.ZERO).add(amt));
                } else if ("EXPENDITURE".equals(tx.getTransactionType())) {
                    accountExpenditureMap.put(accId, accountExpenditureMap.getOrDefault(accId, BigDecimal.ZERO).add(amt));
                }
            }

            // 3) 逐账户构建检查结果（避免为每个账户单独查询）
            for (FundAccount account : accounts) {
                Long accId = account.getAccountId();
                log.info("检查账户：{} (ID={})", account.getAccountName(), accId);

                java.math.BigDecimal bankBalance = getBankBalanceForAccount(accId, request.getBankBalances());

                BigDecimal initialBalance = accountIdToInitial.getOrDefault(accId, BigDecimal.ZERO);
                BigDecimal periodIncome = accountIncomeMap.getOrDefault(accId, BigDecimal.ZERO);
                BigDecimal periodExpenditure = accountExpenditureMap.getOrDefault(accId, BigDecimal.ZERO);

                SettlementCheckAndSettleResponse.CheckResult checkResult = new SettlementCheckAndSettleResponse.CheckResult();
                checkResult.setAccountId(accId);
                checkResult.setAccountName(account.getAccountName());
                checkResult.setInitialBalance(initialBalance);
                checkResult.setFinalBalance(initialBalance.add(periodIncome).subtract(periodExpenditure));
                checkResult.setBankBalance(bankBalance);

                // 填充账期信息并记录
                checkResult.setPeriodId(period.getPeriodId());
                checkResult.setPeriodCode(period.getPeriodCode());
                checkResults.add(checkResult);

                log.info("检查结果 - 期初余额：{}，期末余额：{}，银行余额：{}",
                        checkResult.getInitialBalance(), checkResult.getFinalBalance(),
                        checkResult.getBankBalance());

                saveCheckRecord(period.getPeriodId(), accId, checkResult);
            }
        }

        log.info("检查完成，共生成{}个检查结果", checkResults.size());

        // 构建响应
        SettlementCheckAndSettleResponse response = new SettlementCheckAndSettleResponse();
        response.setOrganizationId(request.getOrganizationId());
        response.setCheckResults(checkResults);

        return response;
    }

    /**
     * 获取下一个账期
     */
    private FundPeriod getNextPeriod(FundPeriod currentPeriod) {
        int nextYear = currentPeriod.getYear();
        int nextPeriod = currentPeriod.getMonth() + 1;

        // 如果超过12月，则进入下一年
        if (nextPeriod > 12) {
            nextYear += 1;
            nextPeriod = 1;
        }

        // 查询下一个账期（必须属于同一组织）
        QueryWrapper<FundPeriod> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("年份", nextYear);
        queryWrapper.eq("月份", nextPeriod);
        queryWrapper.eq("组织编号", currentPeriod.getOrganizationId());
        List<FundPeriod> periods = fundPeriodMapper.selectList(queryWrapper);

        if (periods.isEmpty()) {
            // 如果下一个账期不存在，返回null（不自动创建，由账期管理模块处理）
            return null;
        }

        return periods.get(0);
    }

    /**
     * 获取上一个账期
     */
    private FundPeriod getPreviousPeriod(FundPeriod currentPeriod) {
        int prevYear = currentPeriod.getYear();
        int prevPeriod = currentPeriod.getMonth() - 1;

        // 如果小于1月，则回到上一年的12月
        if (prevPeriod < 1) {
            prevYear -= 1;
            prevPeriod = 12;
        }

        // 查询上一个账期
        QueryWrapper<FundPeriod> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("年份", prevYear);
        queryWrapper.eq("月份", prevPeriod);
        List<FundPeriod> periods = fundPeriodMapper.selectList(queryWrapper);

        if (periods.isEmpty()) {
            // 如果上一个账期不存在，返回null
            return null;
        }

        return periods.get(0);
    }

    /**
     * 计算上一账期的期末余额作为当前账期的期初余额
     */
    private BigDecimal calculatePreviousPeriodFinalBalance(FundAccount account, FundPeriod previousPeriod) {
        if (previousPeriod == null) {
            return BigDecimal.ZERO; // 如果没有上一账期，说明是第一个账期，期初余额为0
        }

        // 1. 获取上一账期的期初余额
        FundAccountInitialBalance prevInitialBalanceRecord = initialBalanceMapper.selectByAccountAndPeriod(
                account.getAccountId(), previousPeriod.getPeriodId());

        BigDecimal prevInitialBalance = BigDecimal.ZERO;
        if (prevInitialBalanceRecord != null) {
            prevInitialBalance = prevInitialBalanceRecord.getInitialBalance();
        }

        // 2. 计算上一账期的收入合计
        QueryWrapper<FundTransaction> incomeQuery = new QueryWrapper<>();
        incomeQuery.eq("账户编号", account.getAccountId());
        incomeQuery.eq("交易类型", "INCOME");
        incomeQuery.ge("交易日期", previousPeriod.getStartDate());
        incomeQuery.le("交易日期", previousPeriod.getEndDate());
        List<FundTransaction> incomeTransactions = transactionMapper.selectList(incomeQuery);

        BigDecimal prevPeriodIncome = incomeTransactions.stream()
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 计算上一账期的支出合计
        QueryWrapper<FundTransaction> expenditureQuery = new QueryWrapper<>();
        expenditureQuery.eq("账户编号", account.getAccountId());
        expenditureQuery.eq("交易类型", "EXPENDITURE");
        expenditureQuery.ge("交易日期", previousPeriod.getStartDate());
        expenditureQuery.le("交易日期", previousPeriod.getEndDate());
        List<FundTransaction> expenditureTransactions = transactionMapper.selectList(expenditureQuery);

        BigDecimal prevPeriodExpenditure = expenditureTransactions.stream()
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. 计算上一账期的期末余额
        return prevInitialBalance.add(prevPeriodIncome).subtract(prevPeriodExpenditure);
    }

    /**
     * 检查单个账户（不依赖银行对账余额，直接显示系统计算的余额）
     */
    private SettlementCheckAndSettleResponse.CheckResult checkAccount(
            FundAccount account,
            FundPeriod period,
            List<FundSettlementCheckItem> checkItems,
            java.math.BigDecimal bankBalance) {

        SettlementCheckAndSettleResponse.CheckResult result = new SettlementCheckAndSettleResponse.CheckResult();
        result.setAccountId(account.getAccountId());
        result.setAccountName(account.getAccountName());

        // 1. 计算期初余额
        FundAccountInitialBalance initialBalanceRecord = initialBalanceMapper.selectByAccountAndPeriod(
                account.getAccountId(), period.getPeriodId());

        BigDecimal initialBalance = BigDecimal.ZERO;
        if (initialBalanceRecord != null) {
            // 如果有期初余额记录，直接使用
            initialBalance = initialBalanceRecord.getInitialBalance();
        } else {
            // 如果没有期初余额记录，尝试从上一账期的期末余额计算
            FundPeriod previousPeriod = getPreviousPeriod(period);
            initialBalance = calculatePreviousPeriodFinalBalance(account, previousPeriod);
        }
        result.setInitialBalance(initialBalance);

        // 2. 计算本期收入合计
        QueryWrapper<FundTransaction> incomeQuery = new QueryWrapper<>();
        incomeQuery.eq("账户编号", account.getAccountId());
        incomeQuery.eq("交易类型", "INCOME");
        incomeQuery.ge("交易日期", period.getStartDate());
        incomeQuery.le("交易日期", period.getEndDate());
        List<FundTransaction> incomeTransactions = transactionMapper.selectList(incomeQuery);

        BigDecimal periodIncome = incomeTransactions.stream()
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 计算本期支出合计
        QueryWrapper<FundTransaction> expenditureQuery = new QueryWrapper<>();
        expenditureQuery.eq("账户编号", account.getAccountId());
        expenditureQuery.eq("交易类型", "EXPENDITURE");
        expenditureQuery.ge("交易日期", period.getStartDate());
        expenditureQuery.le("交易日期", period.getEndDate());
        List<FundTransaction> expenditureTransactions = transactionMapper.selectList(expenditureQuery);

        BigDecimal periodExpenditure = expenditureTransactions.stream()
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. 计算期末余额
        BigDecimal finalBalance = initialBalance.add(periodIncome).subtract(periodExpenditure);
        result.setFinalBalance(finalBalance);

        // 5. 设置银行对账余额
        result.setBankBalance(bankBalance);

        return result;
    }

    /**
     * 保存检查记录
     */
    private void saveCheckRecord(Long periodId, Long accountId, SettlementCheckAndSettleResponse.CheckResult checkResult) {
        FundSettlementCheck checkRecord = new FundSettlementCheck();
        checkRecord.setPeriodId(periodId);
        checkRecord.setAccountId(accountId);
        checkRecord.setInitialBalance(checkResult.getInitialBalance());
        checkRecord.setFinalBalance(checkResult.getFinalBalance());
        checkRecord.setBankBalance(null); // 不保存银行对账余额
        checkRecord.setCheckTime(LocalDateTime.now());
        // checkRecord.setCheckUserId(); // 从当前登录用户获取

        settlementCheckMapper.insert(checkRecord);
    }

    /**
     * 从银行对账余额列表中获取指定账户的余额
     */
    private java.math.BigDecimal getBankBalanceForAccount(Long accountId, List<SettlementCheckAndSettleRequest.BankBalance> bankBalances) {
        if (bankBalances == null || bankBalances.isEmpty()) {
            return null;
        }

        return bankBalances.stream()
                .filter(balance -> accountId.equals(balance.getAccountId()))
                .map(SettlementCheckAndSettleRequest.BankBalance::getBankBalance)
                .findFirst()
                .orElse(null);
    }

    /**
     * 批量结账指定的账期
     */
    private boolean settlePeriods(List<FundPeriod> periods, List<FundAccount> accounts) {
        try {
            LocalDateTime settlementTime = LocalDateTime.now();
            Integer currentUserId = SecurityUtil.getCurrentUserId();
            Long settlementUserId = currentUserId != null ? currentUserId.longValue() : null;

            for (FundPeriod period : periods) {
                // 计算每个账户在该账期的期末余额
                Map<Long, java.math.BigDecimal> accountFinalBalances = new HashMap<>();
                Map<Long, String> accountBalanceDirections = new HashMap<>();

                for (FundAccount account : accounts) {
                    SettlementCheckAndSettleResponse.CheckResult checkResult = checkAccount(
                            account, period, getDefaultCheckItems(), null);
                    // 填充账期信息
                    checkResult.setPeriodId(period.getPeriodId());
                    checkResult.setPeriodCode(period.getPeriodCode());

                    // 保存期末余额和余额方向
                    accountFinalBalances.put(account.getAccountId(), checkResult.getFinalBalance());

                    // 确定余额方向（根据期末余额的正负）
                    String balanceDirection = checkResult.getFinalBalance().compareTo(java.math.BigDecimal.ZERO) >= 0
                            ? "收入" : "支出";
                    accountBalanceDirections.put(account.getAccountId(), balanceDirection);
                }

                // 更新账期为已结账状态
                period.setIsSettled(true);
                period.setSettlementTime(settlementTime);
                period.setSettlementUserId(settlementUserId);
                period.setUpdateTime(settlementTime);
                int rows = fundPeriodMapper.updateById(period);
                if (rows == 0) {
                    log.warn("更新账期状态失败（乐观锁冲突），periodId={}", period.getPeriodId());
                }

                // 获取下一个账期
                FundPeriod nextPeriod = getNextPeriod(period);

                // 如果下一个账期存在，为每个账户创建下一个账期的期初余额
                if (nextPeriod != null) {
                    for (FundAccount account : accounts) {
                        java.math.BigDecimal finalBalance = accountFinalBalances.get(account.getAccountId());
                        String balanceDirection = accountBalanceDirections.get(account.getAccountId());

                        // 检查是否已存在期初余额记录
                        FundAccountInitialBalance existingBalance = initialBalanceMapper.selectByAccountAndPeriod(
                                account.getAccountId(), nextPeriod.getPeriodId());

                        if (existingBalance == null) {
                            // 创建新的期初余额记录
                            FundAccountInitialBalance initialBalance = new FundAccountInitialBalance();
                            initialBalance.setAccountId(account.getAccountId());
                            initialBalance.setPeriodId(nextPeriod.getPeriodId());
                            initialBalance.setInitialBalance(finalBalance);
                            initialBalance.setBalanceDirection(balanceDirection);
                            initialBalance.setBankBalance(null);
                            initialBalance.setCreateTime(settlementTime);
                            initialBalance.setUpdateTime(settlementTime);
                            initialBalanceMapper.insert(initialBalance);
                        } else {
                            // 更新已存在的期初余额记录
                            existingBalance.setInitialBalance(finalBalance);
                            existingBalance.setBalanceDirection(balanceDirection);
                            existingBalance.setUpdateTime(settlementTime);
                            rows = initialBalanceMapper.updateById(existingBalance);
                            if (rows == 0) {
                                log.warn("更新账户期初余额失败（乐观锁冲突），accountId={}, periodId={}",
                                    account.getAccountId(), nextPeriod.getPeriodId());
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("批量结账失败", e);
            return false;
        }
    }

    /**
     * 获取默认检查项（如果数据库中没有）
     */
    private List<FundSettlementCheckItem> getDefaultCheckItems() {
        List<FundSettlementCheckItem> defaultItems = new ArrayList<>();

        FundSettlementCheckItem item1 = new FundSettlementCheckItem();
        item1.setCheckItemCode("CHECK_INITIAL_BALANCE");
        item1.setCheckItemName("检查期初余额");
        item1.setEnabled(true);
        item1.setDescription("检查期初余额是否与系统计算一致");
        item1.setSortOrder(1);
        defaultItems.add(item1);

        FundSettlementCheckItem item2 = new FundSettlementCheckItem();
        item2.setCheckItemCode("CHECK_FINAL_BALANCE");
        item2.setCheckItemName("检查期末余额");
        item2.setEnabled(true);
        item2.setDescription("检查期末余额是否与银行对账余额一致");
        item2.setSortOrder(2);
        defaultItems.add(item2);

        FundSettlementCheckItem item3 = new FundSettlementCheckItem();
        item3.setCheckItemCode("CHECK_BANK_BALANCE");
        item3.setCheckItemName("检查银行对账余额");
        item3.setEnabled(true);
        item3.setDescription("检查是否提供了银行对账余额");
        item3.setSortOrder(3);
        defaultItems.add(item3);

        return defaultItems;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementResponse settlePeriod(SettlementRequest request) {
        // 1. 查询账期信息（使用 periodId 字段）
        FundPeriod period = fundPeriodMapper.selectById(request.getPeriodId());
        if (period == null) {
            throw new RuntimeException("账期不存在，ID：" + request.getPeriodId());
        }

        if (Boolean.TRUE.equals(period.getIsSettled())) {
            throw new RuntimeException("该账期已结账，不能再次结账");
        }

        // 2. 获取该账期所属组织的所有启用的账户
        QueryWrapper<FundAccount> accountQuery = new QueryWrapper<>();
        accountQuery.eq("是否启用", true)
                   .eq("组织编号", period.getOrganizationId());
        List<FundAccount> accounts = fundAccountMapper.selectList(accountQuery);

        if (accounts.isEmpty()) {
            throw new RuntimeException("没有启用的账户");
        }

        // 3. 获取启用的检查项
        List<FundSettlementCheckItem> enabledCheckItems = getDefaultCheckItems();

        // 4. 对每个账户进行检查并计算期末余额
        Map<Long, BigDecimal> accountFinalBalances = new HashMap<>();
        Map<Long, String> accountBalanceDirections = new HashMap<>();

        for (FundAccount account : accounts) {
            SettlementCheckAndSettleResponse.CheckResult checkResult = checkAccount(
                    account, period, enabledCheckItems, null);

            // 保存期末余额和余额方向
            accountFinalBalances.put(account.getAccountId(), checkResult.getFinalBalance());

            // 确定余额方向（根据期末余额的正负）
            String balanceDirection = checkResult.getFinalBalance().compareTo(BigDecimal.ZERO) >= 0
                    ? "收入" : "支出";
            accountBalanceDirections.put(account.getAccountId(), balanceDirection);
        }

        // 5. 更新账期为已结账状态
        period.setIsSettled(true);
        period.setSettlementTime(LocalDateTime.now());
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId != null) {
            period.setSettlementUserId(currentUserId.longValue());
        }
        period.setUpdateTime(LocalDateTime.now());
        int rows = fundPeriodMapper.updateById(period);
        if (rows == 0) {
            log.warn("更新账期状态失败（乐观锁冲突），periodId={}", period.getPeriodId());
        }

        // 6. 获取下一个账期
        FundPeriod nextPeriod = getNextPeriod(period);

        // 7. 如果下一个账期存在，为每个账户创建下一个账期的期初余额
        if (nextPeriod != null) {
            for (FundAccount account : accounts) {
                BigDecimal finalBalance = accountFinalBalances.get(account.getAccountId());
                String balanceDirection = accountBalanceDirections.get(account.getAccountId());

                // 检查是否已存在期初余额记录
                FundAccountInitialBalance existingBalance = initialBalanceMapper.selectByAccountAndPeriod(
                        account.getAccountId(), nextPeriod.getPeriodId());

                if (existingBalance == null) {
                    // 创建新的期初余额记录
                    FundAccountInitialBalance initialBalance = new FundAccountInitialBalance();
                    initialBalance.setAccountId(account.getAccountId());
                    initialBalance.setPeriodId(nextPeriod.getPeriodId());
                    initialBalance.setInitialBalance(finalBalance);
                    initialBalance.setBalanceDirection(balanceDirection);
                    initialBalance.setBankBalance(null);
                    initialBalance.setCreateTime(LocalDateTime.now());
                    initialBalance.setUpdateTime(LocalDateTime.now());
                    initialBalanceMapper.insert(initialBalance);
                } else {
                    // 更新已存在的期初余额记录
                    existingBalance.setInitialBalance(finalBalance);
                    existingBalance.setBalanceDirection(balanceDirection);
                    existingBalance.setUpdateTime(LocalDateTime.now());
                    rows = initialBalanceMapper.updateById(existingBalance);
                    if (rows == 0) {
                        log.warn("更新账户期初余额失败（乐观锁冲突），accountId={}, periodId={}",
                            account.getAccountId(), nextPeriod.getPeriodId());
                    }
                }
            }
        }

            // --- 额外：为下一个账期写入科目级期初余额（FUND_SUBJECT_INITIAL_BALANCE） ---
            try {
                // 查询本期到期日之前的所有流水（按科目聚合），用于计算每个科目的累计净额（含历史）
                QueryWrapper<FundTransaction> subjTxQuery = new QueryWrapper<>();
                subjTxQuery.in("账户编号", accounts.stream().map(FundAccount::getAccountId).collect(Collectors.toList()));
                subjTxQuery.le("交易日期", period.getEndDate());
                subjTxQuery.eq("是否内部往来", false);
                List<FundTransaction> subjTxs = transactionMapper.selectList(subjTxQuery);

                java.util.Map<Long, java.math.BigDecimal> subjectNetMap = new java.util.HashMap<>();
                for (FundTransaction tx : subjTxs) {
                    Long sid = tx.getSubjectId();
                    if (sid == null) continue;
                    BigDecimal amt = tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount();
                    BigDecimal prev = subjectNetMap.getOrDefault(sid, BigDecimal.ZERO);
                    if ("INCOME".equals(tx.getTransactionType())) {
                        prev = prev.add(amt);
                    } else {
                        prev = prev.subtract(amt);
                    }
                    subjectNetMap.put(sid, prev);
                }

                // Upsert per subject into FUND_SUBJECT_INITIAL_BALANCE for nextPeriod
                LocalDateTime now = LocalDateTime.now();
                for (java.util.Map.Entry<Long, java.math.BigDecimal> e : subjectNetMap.entrySet()) {
                    Long subjectId = e.getKey();
                    BigDecimal finalNet = e.getValue();
                    if (finalNet == null) continue;
                    String dir = finalNet.compareTo(BigDecimal.ZERO) >= 0 ? "收入" : "支出";

                    // check existing
                    com.erp.entity.finance.FundSubjectInitialBalance exist = subjectInitialBalanceMapper.selectOne(
                            new QueryWrapper<com.erp.entity.finance.FundSubjectInitialBalance>()
                                    .eq("组织编号", period.getOrganizationId())
                                    .eq("账期编号", nextPeriod.getPeriodId())
                                    .eq("科目编号", subjectId)
                                    .eq("账户编号", (Object) null)
                    );
                    if (exist == null) {
                        com.erp.entity.finance.FundSubjectInitialBalance sib = new com.erp.entity.finance.FundSubjectInitialBalance();
                        sib.setOrganizationId(period.getOrganizationId());
                        sib.setPeriodId(nextPeriod.getPeriodId());
                        sib.setSubjectId(subjectId);
                        sib.setAccountId(null);
                        sib.setInitialBalance(finalNet);
                        sib.setBalanceDirection(dir);
                        sib.setCreateTime(now);
                        sib.setUpdateTime(now);
                        subjectInitialBalanceMapper.insert(sib);
                    } else {
                        exist.setInitialBalance(finalNet);
                        exist.setBalanceDirection(dir);
                        exist.setUpdateTime(now);
                        rows = subjectInitialBalanceMapper.updateById(exist);
                        if (rows == 0) {
                            log.warn("更新科目期初余额失败（乐观锁冲突），subjectId={}, periodId={}",
                                subjectId, nextPeriod.getPeriodId());
                        }
                    }
                }
            } catch (Exception ex) {
                log.error("写入科目级期初余额失败", ex);
            }

        // 8. 构建响应
        SettlementResponse response = new SettlementResponse();
        response.setOrganizationId(period.getPeriodId()); // 返回实际的periodId
        response.setSettlementSuccess(true);

        // 记录数据变更日志（结账：状态变更 未结账 -> 已结账）
        try {
            if (logRecordService != null) {
                FundPeriod oldPeriod = new FundPeriod();
                oldPeriod.setIsSettled(false);
                FundPeriod newPeriod = new FundPeriod();
                newPeriod.setIsSettled(true);
                newPeriod.setSettlementTime(LocalDateTime.now());
                logRecordService.recordDataChangeLog("结账管理", "FUND_PERIOD", String.valueOf(period.getPeriodId()),
                        "结账", String.format("账期结账：账期ID=%s，状态=已结账", period.getPeriodId()),
                        oldPeriod, newPeriod, SecurityUtil.getCurrentUserId(), null, true, null);
            }
        } catch (Exception logEx) {
            log.warn("记录账期结账数据变更日志失败，periodId={}", period.getPeriodId(), logEx);
        }

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementReverseResponse reverseSettlementPeriod(SettlementReverseRequest request) {
        // 1. 查询账期信息（使用 periodId 字段）
        FundPeriod period = fundPeriodMapper.selectById(request.getPeriodId());
        if (period == null) {
            throw new RuntimeException("账期不存在，ID：" + request.getPeriodId());
        }

        // 2. 检查账期是否已结账
        if (!Boolean.TRUE.equals(period.getIsSettled())) {
            throw new RuntimeException("该账期未结账，无需反结账");
        }

        // 3. 检查是否有后续账期已结账（如果有，可能需要提示或阻止反结账）
        // 这里我们允许反结账，但会在日志中记录警告
        FundPeriod nextPeriod = getNextPeriod(period);
        if (nextPeriod != null && Boolean.TRUE.equals(nextPeriod.getIsSettled())) {
            // 如果下一个账期已结账，仍然允许反结账，但需要用户确认（前端已处理）
            // 这里只记录日志
        }

        // 4. 更新账期为未结账状态
        period.setIsSettled(false);
        period.setSettlementTime(null);
        period.setSettlementUserId(null);
        period.setUpdateTime(LocalDateTime.now());
        int rows = fundPeriodMapper.updateById(period);
        if (rows == 0) {
            log.warn("更新账期状态失败（乐观锁冲突），periodId={}", period.getPeriodId());
        }

        // 5. 删除下一个账期的期初余额记录（如果存在）
        // 因为反结账后，下一个账期的期初余额应该重新计算
        if (nextPeriod != null) {
            // 获取所有启用的账户
            QueryWrapper<FundAccount> accountQuery = new QueryWrapper<>();
            accountQuery.eq("是否启用", true);
            List<FundAccount> accounts = fundAccountMapper.selectList(accountQuery);

            for (FundAccount account : accounts) {
                // 删除该账户在下一个账期的期初余额记录
                QueryWrapper<FundAccountInitialBalance> balanceQuery = new QueryWrapper<>();
                balanceQuery.eq("账户编号", account.getAccountId())
                           .eq("账期编号", nextPeriod.getPeriodId());
                initialBalanceMapper.delete(balanceQuery);
            }
            // 同时删除下一个账期的科目级期初余额（组织级别），以避免反结账后科目级期初残留
            try {
                QueryWrapper<com.erp.entity.finance.FundSubjectInitialBalance> subjDelQuery = new QueryWrapper<>();
                subjDelQuery.eq("组织编号", period.getOrganizationId())
                        .eq("账期编号", nextPeriod.getPeriodId());
                subjectInitialBalanceMapper.delete(subjDelQuery);
            } catch (Exception ex) {
                log.warn("删除下一个账期的科目级期初余额失败，org={}, period={}", period.getOrganizationId(), nextPeriod.getPeriodId(), ex);
            }
        }

        // 6. 构建响应
        SettlementReverseResponse response = new SettlementReverseResponse();
        response.setOrganizationId(period.getPeriodId()); // 返回实际的periodId
        response.setReverseSuccess(true);

        // 记录数据变更日志（反结账：状态变更 已结账 -> 未结账）
        try {
            if (logRecordService != null) {
                FundPeriod oldPeriod = new FundPeriod();
                oldPeriod.setIsSettled(true);
                FundPeriod newPeriod = new FundPeriod();
                newPeriod.setIsSettled(false);
                logRecordService.recordDataChangeLog("结账管理", "FUND_PERIOD", String.valueOf(period.getPeriodId()),
                        "反结账", String.format("账期反结账：账期ID=%s，状态=未结账", period.getPeriodId()),
                        oldPeriod, newPeriod, SecurityUtil.getCurrentUserId(), null, true, null);
            }
        } catch (Exception logEx) {
            log.warn("记录账期反结账数据变更日志失败，periodId={}", period.getPeriodId(), logEx);
        }

        return response;
    }

    @Override
    public SettlementCheckItemResponse getCheckItems() {
        // 目前使用默认检查项，不依赖数据库表
        // TODO: 如果将来需要持久化检查项设置，可以创建 FUND_SETTLEMENT_CHECK_ITEM 表
        List<FundSettlementCheckItem> items = getDefaultCheckItems();

        SettlementCheckItemResponse response = new SettlementCheckItemResponse();
        List<SettlementCheckItemResponse.CheckItem> checkItems = new ArrayList<>();
        
        // 为每个默认检查项分配一个虚拟ID（从1开始）
        long checkItemId = 1;
        for (FundSettlementCheckItem item : items) {
            SettlementCheckItemResponse.CheckItem dto = new SettlementCheckItemResponse.CheckItem();
            dto.setCheckItemId(checkItemId++);
            dto.setCheckItemName(item.getCheckItemName());
            dto.setCheckItemCode(item.getCheckItemCode());
            dto.setEnabled(item.getEnabled());
            dto.setDescription(item.getDescription());
            dto.setSortOrder(item.getSortOrder());
            checkItems.add(dto);
        }

        response.setCheckItems(checkItems);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCheckItems(SettlementCheckItemUpdateRequest request) {
        // 目前检查项设置不支持持久化，只返回成功
        // TODO: 如果将来需要持久化检查项设置，可以创建 FUND_SETTLEMENT_CHECK_ITEM 表
        // 并在这里实现更新逻辑
        if (request.getCheckItems() == null || request.getCheckItems().isEmpty()) {
            return;
        }
        
        // 暂时不做任何操作，因为检查项是默认的，不支持修改
        // 如果需要支持修改，需要创建数据库表并实现持久化逻辑
    }
}

