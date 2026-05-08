package com.erp.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.common.util.SecurityUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.erp.controller.finance.dto.BatchImportReceiptRequest;
import com.erp.controller.finance.dto.BatchImportReceiptResponse;
import com.erp.controller.finance.dto.FundTransactionCreateRequest;
import com.erp.controller.settlement.dto.SettlementAssociateRequest;
import com.erp.controller.settlement.dto.SettlementUpdateDTO;
import com.erp.entity.common.File;
import com.erp.entity.finance.FundAccount;
import com.erp.entity.finance.FundPeriod;
import com.erp.entity.finance.FundTransaction;
import com.erp.entity.settlement.SettlementFundTransactionRel;
import com.erp.entity.settlement.Settlement;
import com.erp.mapper.finance.FundAccountMapper;
import com.erp.mapper.finance.FundPeriodMapper;
import com.erp.mapper.finance.FundTransactionMapper;
import com.erp.service.finance.FundTransactionService;
import com.erp.service.settlement.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import com.erp.service.system.ILogRecordService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 资金流水服务实现类
 */
@Service
@Slf4j
public class FundTransactionServiceImpl implements FundTransactionService {

    @Autowired
    private FundTransactionMapper transactionMapper;

    @Autowired
    private FundAccountMapper accountMapper;

    @Autowired
    private FundPeriodMapper periodMapper;
    @Autowired
    private com.erp.mapper.finance.SettlementFundTransactionRelMapper settlementFundTransactionRelMapper;
    @Autowired
    private com.erp.mapper.finance.SettlementInvoiceRelMapper settlementInvoiceRelMapper;
    @Autowired
    private com.erp.mapper.finance.SettlementMapper settlementMapper;
    @Autowired
    private ILogRecordService logRecordService;
    @Autowired
    private com.erp.mapper.finance.FundTransactionMapper fundTransactionMapper;
    @Autowired
    private SettlementService settlementService;

    @Autowired
    private com.erp.service.common.FileService fileService;

    private static final DateTimeFormatter CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundTransaction createTransaction(FundTransactionCreateRequest request) {
        // 1. 校验账户是否存在
        FundAccount account = accountMapper.selectById(request.getAccountId());
        if (account == null) {
            throw new RuntimeException("账户不存在，ID：" + request.getAccountId());
        }
        if (!Boolean.TRUE.equals(account.getEnabled())) {
            throw new RuntimeException("账户已停用，无法创建流水");
        }

        // 2. 校验账期是否存在且未结账
        FundPeriod period = periodMapper.selectById(request.getPeriodId());
        if (period == null) {
            throw new RuntimeException("账期不存在，ID：" + request.getPeriodId());
        }
        if (Boolean.TRUE.equals(period.getIsSettled())) {
            throw new RuntimeException("该账期已结账，无法创建流水");
        }

        // 3. 校验交易日期是否在账期范围内
        if (request.getTransactionDate().isBefore(period.getStartDate()) ||
            request.getTransactionDate().isAfter(period.getEndDate())) {
            throw new RuntimeException(String.format("交易日期必须在账期范围内（%s 至 %s）",
                    period.getStartDate(), period.getEndDate()));
        }

        // 4. 校验内部往来
        if (Boolean.TRUE.equals(request.getInternalTransfer())) {
            if (request.getRelatedAccountId() == null) {
                throw new RuntimeException("内部往来必须指定关联账户");
            }
            if (request.getRelatedAccountId().equals(request.getAccountId())) {
                throw new RuntimeException("关联账户不能与当前账户相同");
            }
            FundAccount relatedAccount = accountMapper.selectById(request.getRelatedAccountId());
            if (relatedAccount == null) {
                throw new RuntimeException("关联账户不存在，ID：" + request.getRelatedAccountId());
            }
        }

        // 5. 生成流水编码：LS-YYYYMMDD-4位序号
        String dateStr = request.getTransactionDate().format(CODE_FORMATTER);
        String prefix = "LS-" + dateStr + "-";
        
        // 查询当天最大序号
        QueryWrapper<FundTransaction> codeQuery = new QueryWrapper<>();
        codeQuery.likeRight("流水编码", prefix);
        codeQuery.orderByDesc("流水编码");
        codeQuery.last("LIMIT 1");
        FundTransaction lastTransaction = transactionMapper.selectOne(codeQuery);
        
        int sequence = 1;
        if (lastTransaction != null && lastTransaction.getTransactionCode() != null) {
            String lastCode = lastTransaction.getTransactionCode();
            String lastSequenceStr = lastCode.substring(lastCode.lastIndexOf("-") + 1);
            try {
                sequence = Integer.parseInt(lastSequenceStr) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }
        
        String transactionCode = prefix + String.format("%04d", sequence);

        // 6. 创建流水记录
        FundTransaction transaction = new FundTransaction();
        FundTransaction relatedTransaction = null; // 内部往来关联流水
        transaction.setAccountId(request.getAccountId());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setAmount(request.getAmount());
        transaction.setCounterpartyAccount(request.getCounterpartyAccount());
        transaction.setCounterpartyName(request.getCounterpartyName());
        transaction.setCounterpartyBank(request.getCounterpartyBank());
        transaction.setPurpose(request.getPurpose());
        transaction.setSummary(request.getSummary());
        transaction.setSubjectId(request.getSubjectId());
        transaction.setInternalTransfer(Boolean.TRUE.equals(request.getInternalTransfer()));
        transaction.setRelatedAccountId(request.getRelatedAccountId());
        transaction.setRelatedTransactionId(request.getRelatedTransactionId());
        transaction.setReceiptNo(request.getReceiptNo());
        transaction.setRemark(request.getRemark());
        transaction.setTimestamp(request.getTimestamp());
        transaction.setTransactionCode(transactionCode);
        transaction.setCreateTime(LocalDateTime.now());
        transaction.setUpdateTime(LocalDateTime.now());

        // 7. 保存
        transactionMapper.insert(transaction);

        // 8. 如果是内部往来，创建关联流水
        if (Boolean.TRUE.equals(request.getInternalTransfer()) && request.getRelatedAccountId() != null) {
            // 获取关联账户信息
            FundAccount relatedAccount = accountMapper.selectById(request.getRelatedAccountId());
            if (relatedAccount == null) {
                throw new RuntimeException("关联账户不存在");
            }

            // 获取关联账期（根据关联账户的组织ID和交易日期的年月）
            int year = request.getTransactionDate().getYear();
            int month = request.getTransactionDate().getMonthValue();

            QueryWrapper<FundPeriod> relatedPeriodQuery = new QueryWrapper<>();
            relatedPeriodQuery.eq("组织编号", relatedAccount.getOrganizationId());
            relatedPeriodQuery.eq("年份", year);
            relatedPeriodQuery.eq("月份", month);
            FundPeriod relatedPeriod = periodMapper.selectOne(relatedPeriodQuery);

            if (relatedPeriod == null) {
                throw new RuntimeException("关联账户的账期不存在");
            }
            if (Boolean.TRUE.equals(relatedPeriod.getIsSettled())) {
                throw new RuntimeException("关联账户的账期已结账，无法创建流水");
            }

            // 生成关联流水编码
            String relatedCode = "LS-" + dateStr + "-" + String.format("%04d", sequence + 1);
            
            // 创建关联流水（交易类型相反）
            relatedTransaction = new FundTransaction();
            relatedTransaction.setAccountId(request.getRelatedAccountId());
            relatedTransaction.setTransactionDate(request.getTransactionDate());
            relatedTransaction.setTransactionType(
                    "INCOME".equals(request.getTransactionType()) ? "EXPENDITURE" : "INCOME");
            relatedTransaction.setAmount(request.getAmount());
            relatedTransaction.setCounterpartyAccount(request.getCounterpartyAccount());
            relatedTransaction.setCounterpartyName(request.getCounterpartyName());
            relatedTransaction.setCounterpartyBank(request.getCounterpartyBank());
            relatedTransaction.setPurpose(request.getPurpose());
            relatedTransaction.setSummary(request.getSummary());
            relatedTransaction.setSubjectId(request.getSubjectId());
            relatedTransaction.setInternalTransfer(true);
            relatedTransaction.setRelatedAccountId(request.getAccountId());
            relatedTransaction.setRelatedTransactionId(transaction.getTransactionId());
            relatedTransaction.setRemark(request.getRemark());
            relatedTransaction.setTimestamp(request.getTimestamp());
            relatedTransaction.setTransactionCode(relatedCode);
            relatedTransaction.setCreateTime(LocalDateTime.now());
            relatedTransaction.setUpdateTime(LocalDateTime.now());
            
            transactionMapper.insert(relatedTransaction);
            
            // 更新原流水的关联流水编号
            transaction.setRelatedTransactionId(relatedTransaction.getTransactionId());
            int rows = transactionMapper.updateById(transaction);
            if (rows == 0) {
                log.warn("更新原流水关联编号失败（乐观锁冲突），transactionId={}", transaction.getTransactionId());
            }
        }

        // 记录数据变更日志
        try {
            Integer currentUserId = SecurityUtil.getCurrentUserId();
            logRecordService.recordDataChangeLog("资金管理", "FUND_TRANSACTION",
                    String.valueOf(transaction.getTransactionId()),
                    "新增",
                    "创建资金流水：流水编码=" + transaction.getTransactionCode() + "，金额=" + transaction.getAmount(),
                    null, transaction, currentUserId, null, true, null);
            // 如果是内部往来，也记录关联流水
            if (relatedTransaction != null) {
                logRecordService.recordDataChangeLog("资金管理", "FUND_TRANSACTION",
                        String.valueOf(relatedTransaction.getTransactionId()),
                        "新增",
                        "创建资金流水（内部往来）：流水编码=" + relatedTransaction.getTransactionCode() + "，金额=" + relatedTransaction.getAmount(),
                        null, relatedTransaction, currentUserId, null, true, null);
            }
        } catch (Exception logEx) {
            log.warn("记录资金流水创建数据变更日志失败", logEx);
        }

        return transaction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void attachReceiptFile(Long transactionId, Integer fileId, String fileName) {
        try {
            FundTransaction tx = transactionMapper.selectById(transactionId);
            if (tx == null) {
                throw new RuntimeException("资金流水不存在，ID：" + transactionId);
            }
            // 设置回单文件编号（数据库字段：回单文件编号）和文件名（回单编号暂不自动写入）
            // 使用 MyBatis-Plus 的 updateById 进行更新
            tx.setReceiptFile(fileId);
            // 不覆盖已存在的回单编号，fileName 可用于前端显示
            int rows = transactionMapper.updateById(tx);
            if (rows == 0) {
                log.warn("关联回单文件失败（乐观锁冲突），transactionId={}", transactionId);
            }
        } catch (Exception e) {
            log.error("关联回单文件失败，transactionId={}, fileId={}", transactionId, fileId, e);
            throw e;
        }
    }

    @Override
    public FundTransaction getTransactionById(Long transactionId) {
        return transactionMapper.selectById(transactionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundTransaction updateTransaction(Long transactionId, FundTransactionCreateRequest request) {
        // 1. 查询流水是否存在
        FundTransaction transaction = transactionMapper.selectById(transactionId);
        if (transaction == null) {
            throw new RuntimeException("流水不存在，ID：" + transactionId);
        }

        // 保存旧数据用于数据变更日志
        FundTransaction oldDetail = transactionMapper.selectById(transactionId);

        // 2. 校验账期是否已结账
        FundPeriod period = periodMapper.selectById(request.getPeriodId());
        if (period == null) {
            throw new RuntimeException("账期不存在，ID：" + request.getPeriodId());
        }
        if (Boolean.TRUE.equals(period.getIsSettled())) {
            throw new RuntimeException("该账期已结账，无法修改流水");
        }

        // 3. 校验账户是否存在且启用
        FundAccount account = accountMapper.selectById(request.getAccountId());
        if (account == null) {
            throw new RuntimeException("账户不存在，ID：" + request.getAccountId());
        }
        if (!Boolean.TRUE.equals(account.getEnabled())) {
            throw new RuntimeException("账户已停用，无法修改流水");
        }

        // 4. 校验交易日期是否在账期范围内
        if (request.getTransactionDate().isBefore(period.getStartDate()) ||
            request.getTransactionDate().isAfter(period.getEndDate())) {
            throw new RuntimeException(String.format("交易日期必须在账期范围内（%s 至 %s）",
                    period.getStartDate(), period.getEndDate()));
        }

        // 5. 校验内部往来
        if (Boolean.TRUE.equals(request.getInternalTransfer())) {
            if (request.getRelatedAccountId() == null) {
                throw new RuntimeException("内部往来必须指定关联账户");
            }
            if (request.getRelatedAccountId().equals(request.getAccountId())) {
                throw new RuntimeException("关联账户不能与当前账户相同");
            }
            FundAccount relatedAccount = accountMapper.selectById(request.getRelatedAccountId());
            if (relatedAccount == null) {
                throw new RuntimeException("关联账户不存在，ID：" + request.getRelatedAccountId());
            }
        }

        // 6. 更新流水记录
        transaction.setAccountId(request.getAccountId());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setAmount(request.getAmount());
        transaction.setCounterpartyAccount(request.getCounterpartyAccount());
        transaction.setCounterpartyName(request.getCounterpartyName());
        transaction.setCounterpartyBank(request.getCounterpartyBank());
        transaction.setPurpose(request.getPurpose());
        transaction.setSummary(request.getSummary());
        transaction.setSubjectId(request.getSubjectId());
        transaction.setInternalTransfer(Boolean.TRUE.equals(request.getInternalTransfer()));
        transaction.setRelatedAccountId(request.getRelatedAccountId());
        transaction.setRelatedTransactionId(request.getRelatedTransactionId());
        transaction.setReceiptNo(request.getReceiptNo());
        transaction.setRemark(request.getRemark());
        transaction.setTimestamp(request.getTimestamp());
        transaction.setUpdateTime(LocalDateTime.now());

        int rows = transactionMapper.updateById(transaction);
        if (rows == 0) {
            log.warn("更新资金流水失败（乐观锁冲突），transactionId={}", transactionId);
        }

        // 记录数据变更日志（不影响主流程）
        try {
            FundTransaction newDetail = transactionMapper.selectById(transactionId);
            logRecordService.recordDataChangeLog("资金流水", "FUND_TRANSACTION", String.valueOf(transactionId),
                    "更新", "更新资金流水：" + (newDetail != null ? newDetail.getTransactionCode() : transactionId),
                    oldDetail, newDetail, com.erp.common.util.SecurityUtil.getCurrentUserId(), null, true, null);
        } catch (Exception logEx) {
            log.warn("记录资金流水更新数据变更日志失败", logEx);
        }

        // 7. 如果是内部往来，更新关联流水
        if (Boolean.TRUE.equals(request.getInternalTransfer()) && request.getRelatedAccountId() != null) {
            if (transaction.getRelatedTransactionId() != null) {
                FundTransaction relatedTransaction = transactionMapper.selectById(transaction.getRelatedTransactionId());
                if (relatedTransaction != null) {
                    // 获取关联账户信息
                    FundAccount relatedAccount = accountMapper.selectById(request.getRelatedAccountId());
                    if (relatedAccount == null) {
                        throw new RuntimeException("关联账户不存在");
                    }

                    // 获取关联账期（根据关联账户的组织ID和交易日期的年月）
                    int year = request.getTransactionDate().getYear();
                    int month = request.getTransactionDate().getMonthValue();

                    QueryWrapper<FundPeriod> relatedPeriodQuery = new QueryWrapper<>();
                    relatedPeriodQuery.eq("组织编号", relatedAccount.getOrganizationId());
                    relatedPeriodQuery.eq("年份", year);
                    relatedPeriodQuery.eq("月份", month);
                    FundPeriod relatedPeriod = periodMapper.selectOne(relatedPeriodQuery);
                    
                    if (relatedPeriod != null && !Boolean.TRUE.equals(relatedPeriod.getIsSettled())) {
                        // 更新关联流水（交易类型相反）
                        relatedTransaction.setAccountId(request.getRelatedAccountId());
                        relatedTransaction.setTransactionDate(request.getTransactionDate());
                        relatedTransaction.setTransactionType(
                                "INCOME".equals(request.getTransactionType()) ? "EXPENDITURE" : "INCOME");
                        relatedTransaction.setAmount(request.getAmount());
                        relatedTransaction.setCounterpartyAccount(request.getCounterpartyAccount());
                        relatedTransaction.setCounterpartyName(request.getCounterpartyName());
                        relatedTransaction.setCounterpartyBank(request.getCounterpartyBank());
                        relatedTransaction.setPurpose(request.getPurpose());
                        relatedTransaction.setSummary(request.getSummary());
                        relatedTransaction.setInternalTransfer(true);
                        relatedTransaction.setRelatedAccountId(request.getAccountId());
                        relatedTransaction.setRelatedTransactionId(transaction.getTransactionId());
                        relatedTransaction.setRemark(request.getRemark());
                        relatedTransaction.setTimestamp(request.getTimestamp());
                        relatedTransaction.setUpdateTime(LocalDateTime.now());

                        rows = transactionMapper.updateById(relatedTransaction);
                        if (rows == 0) {
                            log.warn("更新关联流水失败（乐观锁冲突），relatedTransactionId={}",
                                relatedTransaction.getTransactionId());
                        }
                    }
                }
            }
        }

        return transaction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTransaction(Long transactionId) {
        // 1. 查询流水是否存在
        FundTransaction transaction = transactionMapper.selectById(transactionId);
        if (transaction == null) {
            throw new RuntimeException("流水不存在，ID：" + transactionId);
        }

        // 2. 查询账期是否已结账
        FundAccount account = accountMapper.selectById(transaction.getAccountId());
        if (account == null) {
            throw new RuntimeException("账户不存在，ID：" + transaction.getAccountId());
        }

        int year = transaction.getTransactionDate().getYear();
        int month = transaction.getTransactionDate().getMonthValue();

        QueryWrapper<FundPeriod> periodQuery = new QueryWrapper<>();
        periodQuery.eq("组织编号", account.getOrganizationId());
        periodQuery.eq("年份", year);
        periodQuery.eq("月份", month);
        FundPeriod period = periodMapper.selectOne(periodQuery);

        if (period != null && Boolean.TRUE.equals(period.getIsSettled())) {
            throw new RuntimeException("该账期已结账，无法删除流水");
        }

        // 3. 如果是内部往来，删除关联流水
        if (Boolean.TRUE.equals(transaction.getInternalTransfer()) && transaction.getRelatedTransactionId() != null) {
            FundTransaction relatedTransaction = transactionMapper.selectById(transaction.getRelatedTransactionId());
            if (relatedTransaction != null) {
                // 获取关联账户信息
                FundAccount relatedAccount = accountMapper.selectById(relatedTransaction.getAccountId());
                if (relatedAccount != null) {
                    // 检查关联流水的账期是否已结账
                    int relatedYear = relatedTransaction.getTransactionDate().getYear();
                    int relatedMonth = relatedTransaction.getTransactionDate().getMonthValue();

                    QueryWrapper<FundPeriod> relatedPeriodQuery = new QueryWrapper<>();
                    relatedPeriodQuery.eq("组织编号", relatedAccount.getOrganizationId());
                    relatedPeriodQuery.eq("年份", relatedYear);
                    relatedPeriodQuery.eq("月份", relatedMonth);
                    FundPeriod relatedPeriod = periodMapper.selectOne(relatedPeriodQuery);

                    if (relatedPeriod == null || !Boolean.TRUE.equals(relatedPeriod.getIsSettled())) {
                        int rows = transactionMapper.deleteById(relatedTransaction.getTransactionId());
                        if (rows == 0) {
                            log.warn("删除关联流水失败，relatedTransactionId={}",
                                relatedTransaction.getTransactionId());
                        }
                    }
                }
            }
        }

        // 4. 删除流水
        transactionMapper.deleteById(transactionId);
        // 记录删除数据变更日志（不影响主流程）
        try {
            logRecordService.recordDataChangeLog("资金流水", "FUND_TRANSACTION", String.valueOf(transactionId),
                    "删除", "删除资金流水：" + transactionId, transaction, null, com.erp.common.util.SecurityUtil.getCurrentUserId(), null, true, null);
        } catch (Exception logEx) {
            log.warn("记录资金流水删除数据变更日志失败", logEx);
        }
    }

    @Override
    public java.util.List<FundTransaction> getAccountTransactions(Long accountId, Long periodId) {
        log.info("查询账户流水记录，accountId={}, periodId={}", accountId, periodId);

        QueryWrapper<FundTransaction> query = new QueryWrapper<>();
        query.eq("账户编号", accountId);

        // 如果指定了账期，则只查询该账期的流水
        if (periodId != null) {
            // 先查询账期信息
            FundPeriod period = periodMapper.selectById(periodId);
            if (period != null) {
                query.ge("交易日期", period.getStartDate());
                query.le("交易日期", period.getEndDate());
            }
        }

        // 按交易日期倒序排列，显示最新的流水
        query.orderByDesc("交易日期");
        query.orderByDesc("创建时间");

        return transactionMapper.selectList(query);
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getAssociatedSettlements(Long transactionId) {
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SettlementFundTransactionRel> qw =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            qw.eq("资金流水编号", transactionId);

            java.util.List<SettlementFundTransactionRel> rels =
                    settlementFundTransactionRelMapper.selectList(qw);

            if (rels == null || rels.isEmpty()) {
                return new java.util.ArrayList<>();
            }

            java.util.List<Long> settlementIds = new java.util.ArrayList<>();
            for (SettlementFundTransactionRel r : rels) {
                settlementIds.add(r.getSettlementId());
            }

            java.util.List<Settlement> settlements =
                    settlementMapper.selectBatchIds(settlementIds);

            java.util.Map<Long, SettlementFundTransactionRel> relMap = new java.util.HashMap<>();
            for (SettlementFundTransactionRel r : rels) {
                relMap.put(r.getSettlementId(), r);
            }

            java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
            for (Settlement s : settlements) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("settlementId", s.getSettlementId());
                item.put("settlementCode", s.getSettlementCode());
                item.put("settlementType", s.getSettlementType());
                item.put("settlementPeriodStart", s.getSettlementPeriodStart());
                item.put("settlementPeriodEnd", s.getSettlementPeriodEnd());
                item.put("settlementAmount", s.getSettlementAmount());
                item.put("status", s.getStatus());
                SettlementFundTransactionRel rel = relMap.get(s.getSettlementId());
                item.put("relAmount", rel != null ? rel.getRelAmount() : null);
                result.add(item);
            }

            return result;
        } catch (Exception e) {
            log.error("查询已关联结算单失败，transactionId={}", transactionId, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void associateSettlements(Long transactionId, java.util.List<SettlementAssociateRequest.SettlementRelation> settlementRelations) {
        // 如果传入为空或空列表，清空当前流水的所有有效关联（支持删除所有关联）
        if (settlementRelations == null || settlementRelations.isEmpty()) {
            // 查询当前流水关联的结算单
            QueryWrapper<SettlementFundTransactionRel> delQ = new QueryWrapper<>();
            delQ.eq("资金流水编号", transactionId);
            java.util.List<SettlementFundTransactionRel> existingRelations = settlementFundTransactionRelMapper.selectList(delQ);

            // 删除关联记录
            settlementFundTransactionRelMapper.delete(delQ);

            // 对于被取消关联的结算单，将已收金额更新为 0
            for (SettlementFundTransactionRel rel : existingRelations) {
                if (rel.getSettlementId() != null) {
                    updateSettlementReceivedAmount(rel.getSettlementId(), BigDecimal.ZERO);
                }
            }
            return;
        }

        // 校验 transaction 存在
        com.erp.entity.finance.FundTransaction transaction = fundTransactionMapper.selectById(transactionId);
        if (transaction == null) {
            throw new com.erp.common.exception.BusinessException("资金流水不存在，ID：" + transactionId);
        }

        // 查询当前已关联的结算单（用于后续判断哪些是新增的）
        QueryWrapper<SettlementFundTransactionRel> currentRelQ = new QueryWrapper<>();
        currentRelQ.eq("资金流水编号", transactionId);
        java.util.List<SettlementFundTransactionRel> currentRelations = settlementFundTransactionRelMapper.selectList(currentRelQ);
        java.util.Set<Long> currentSettlementIds = currentRelations.stream()
                .map(SettlementFundTransactionRel::getSettlementId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());

        // 校验每个结算单并插入关系
        Integer currentUserId = com.erp.common.util.SecurityUtil.getCurrentUserId();
        for (SettlementAssociateRequest.SettlementRelation rel : settlementRelations) {
            Long settlementId = rel.getSettlementId();
            if (settlementId == null) continue;

            // 校验结算单存在
            Settlement settlement = settlementMapper.selectById(settlementId);
            if (settlement == null) {
                throw new com.erp.common.exception.BusinessException("结算单不存在，ID：" + settlementId);
            }

            // 检查是否已被其他流水关联（状态为有效）
            QueryWrapper<SettlementFundTransactionRel> qw = new QueryWrapper<>();
            qw.eq("结算单编号", settlementId)
              .ne("资金流水编号", transactionId);
            java.util.List<SettlementFundTransactionRel> exists = settlementFundTransactionRelMapper.selectList(qw);
            if (exists != null && !exists.isEmpty()) {
                String code = settlement.getSettlementCode() != null ? settlement.getSettlementCode() : String.valueOf(settlementId);
                throw new com.erp.common.exception.BusinessException("结算单 " + code + " 已被其他资金流水关联，不能重复关联");
            }

            // 检查是否已与当前流水存在有效关系，避免重复插入
            QueryWrapper<SettlementFundTransactionRel> existQ2 = new QueryWrapper<>();
            existQ2.eq("结算单编号", settlementId)
                   .eq("资金流水编号", transactionId);
            Long count = settlementFundTransactionRelMapper.selectCount(existQ2);
            if (count != null && count > 0L) {
                // 跳过重复
                continue;
            }

            SettlementFundTransactionRel record = new SettlementFundTransactionRel();
            record.setSettlementId(settlementId);
            record.setTransactionId(transactionId);
            record.setRelAmount(rel.getRelAmount() == null ? BigDecimal.ZERO : rel.getRelAmount());
            record.setRelTime(LocalDateTime.now());
            record.setCreateUserId(currentUserId);
            record.setStatus("有效");
            settlementFundTransactionRelMapper.insert(record);

            // 将结算单设置为已结算（使用 SettlementService 的 field 模式更新 status）
            try {
                SettlementUpdateDTO updateDTO = new SettlementUpdateDTO();
                updateDTO.setUpdateMode("field");
                java.util.Map<String, Object> fieldUpdates = new java.util.HashMap<>();
                fieldUpdates.put("status", "已结算");
                updateDTO.setFieldUpdates(fieldUpdates);
                settlementService.updateSettlement(settlementId, updateDTO);
            } catch (Exception ex) {
                log.warn("设置结算单状态为已结算失败，settlementId={}, error={}", settlementId, ex.getMessage());
            }

            // 计算发票净额并更新已收金额
            try {
                BigDecimal netAmount = calculateInvoiceNetAmount(settlementId);
                updateSettlementReceivedAmount(settlementId, netAmount);
            } catch (Exception ex) {
                log.warn("更新结算单已收金额失败，settlementId={}, error={}", settlementId, ex.getMessage());
            }
        }

        // 对所有当前关联的结算单重新计算并更新已收金额（确保即使没有新增操作也正确计算）
        for (Long settlementId : currentSettlementIds) {
            try {
                BigDecimal netAmount = calculateInvoiceNetAmount(settlementId);
                updateSettlementReceivedAmount(settlementId, netAmount);
            } catch (Exception ex) {
                log.warn("重新计算结算单已收金额失败，settlementId={}, error={}", settlementId, ex.getMessage());
            }
        }
    }

    /**
     * 更新结算单的已收金额
     * 使用 SettlementService 的 updateSettlement 接口（field 模式）
     *
     * @param settlementId 结算单ID
     * @param receivedAmount 新的已收金额
     */
    private void updateSettlementReceivedAmount(Long settlementId, BigDecimal receivedAmount) {
        try {
            SettlementUpdateDTO updateDTO = new SettlementUpdateDTO();
            updateDTO.setUpdateMode("field");
            java.util.Map<String, Object> fieldUpdates = new java.util.HashMap<>();
            fieldUpdates.put("receivedAmount", receivedAmount);
            updateDTO.setFieldUpdates(fieldUpdates);

            settlementService.updateSettlement(settlementId, updateDTO);
            log.info("结算单已收金额更新成功，settlementId={}, receivedAmount={}", settlementId, receivedAmount);
        } catch (Exception e) {
            log.error("更新结算单已收金额失败，settlementId={}, receivedAmount={}", settlementId, receivedAmount, e);
            throw new RuntimeException("更新结算单已收金额失败：" + e.getMessage());
        }
    }

    /**
     * 计算结算单已关联发票的净额（蓝字 - 红字）
     *
     * @param settlementId 结算单ID
     * @return 净额
     */
    private BigDecimal calculateInvoiceNetAmount(Long settlementId) {
        BigDecimal netAmount = settlementInvoiceRelMapper.selectNetAmountBySettlementId(settlementId);
        if (netAmount == null) {
            netAmount = BigDecimal.ZERO;
        }
        return netAmount;
    }

    @Override
    public java.util.List<FundTransaction> getTransactionsByAccountAndPeriod(Long accountId, Long periodId) {
        QueryWrapper<FundTransaction> query = new QueryWrapper<>();
        query.eq("账户编号", accountId);

        if (periodId != null) {
            FundPeriod period = periodMapper.selectById(periodId);
            if (period != null) {
                query.ge("交易日期", period.getStartDate());
                query.le("交易日期", period.getEndDate());
            }
        }

        query.orderByAsc("交易日期", "创建时间");
        return transactionMapper.selectList(query);
    }

    @Override
    public com.erp.controller.finance.dto.BatchImportReceiptResponse batchImportReceipts(
            Long accountId, Long periodId,
            java.util.List<com.erp.controller.finance.dto.BatchImportReceiptRequest.ReceiptFileItem> files,
            Integer uploaderId) {

        log.info("批量导入银行回单，accountId={}, periodId={}, fileCount={}", accountId, periodId, files.size());

        // 查询该账户和账期下的所有流水
        java.util.List<FundTransaction> transactions = getTransactionsByAccountAndPeriod(accountId, periodId);

        // 构建回单编号到流水ID的映射
        java.util.Map<String, FundTransaction> receiptNoMap = new java.util.HashMap<>();
        for (FundTransaction tx : transactions) {
            if (tx.getReceiptNo() != null && !tx.getReceiptNo().isEmpty()) {
                receiptNoMap.put(tx.getReceiptNo(), tx);
            }
        }

        java.util.List<com.erp.controller.finance.dto.BatchImportReceiptResponse.ImportSuccessItem> successList =
                new java.util.ArrayList<>();
        java.util.List<com.erp.controller.finance.dto.BatchImportReceiptResponse.ImportFailItem> failList =
                new java.util.ArrayList<>();
        int notFoundCount = 0;

        for (com.erp.controller.finance.dto.BatchImportReceiptRequest.ReceiptFileItem fileItem : files) {
            String originalFileName = fileItem.getFileName();
            String fileContent = fileItem.getFileContent();

            try {
                // 解码Base64文件内容
                byte[] fileBytes = java.util.Base64.getDecoder().decode(fileContent);

                // 生成唯一文件名（与单次上传保持一致，使用UUID）
                String fileExtension = StrUtil.isBlank(originalFileName) ? ".pdf" :
                        (originalFileName.contains(".") ? originalFileName.substring(originalFileName.lastIndexOf(".")) : ".pdf");
                String uniqueFileName = IdUtil.fastUUID() + fileExtension;

                // 使用PDFBox提取PDF文本
                String pdfText = extractTextFromPdf(fileBytes);

                // 在PDF文本中查找匹配的流水
                FundTransaction matchedTransaction = findMatchingTransaction(pdfText, receiptNoMap);

                if (matchedTransaction == null) {
                    notFoundCount++;
                    failList.add(com.erp.controller.finance.dto.BatchImportReceiptResponse.ImportFailItem.builder()
                            .fileName(originalFileName)
                            .reason("未找到匹配的流水（回单编号不匹配）")
                            .build());
                    continue;
                }

                // 使用唯一文件名上传文件（与单次上传保持一致）
                com.erp.entity.common.File fileEntity = fileService.uploadBytesAndSave(
                        fileBytes, uniqueFileName, "FUND_RECEIPT",
                        matchedTransaction.getTransactionId().intValue(),
                        uploaderId);

                // 关联回单文件到流水
                attachReceiptFile(matchedTransaction.getTransactionId(), fileEntity.getFileId(), fileEntity.getFileName());

                successList.add(com.erp.controller.finance.dto.BatchImportReceiptResponse.ImportSuccessItem.builder()
                        .fileName(originalFileName)
                        .transactionId(matchedTransaction.getTransactionId())
                        .transactionCode(matchedTransaction.getTransactionCode())
                        .fileId(fileEntity.getFileId())
                        .build());

                log.info("银行回单导入成功，originalFileName={}, savedFileName={}, transactionId={}",
                        originalFileName, uniqueFileName, matchedTransaction.getTransactionId());

            } catch (Exception e) {
                log.error("处理银行回单失败，fileName={}", originalFileName, e);
                failList.add(com.erp.controller.finance.dto.BatchImportReceiptResponse.ImportFailItem.builder()
                        .fileName(originalFileName)
                        .reason("处理失败：" + e.getMessage())
                        .build());
            }
        }

        return com.erp.controller.finance.dto.BatchImportReceiptResponse.builder()
                .totalCount(files.size())
                .successCount(successList.size())
                .failCount(failList.size())
                .notFoundCount(notFoundCount)
                .successList(successList)
                .failList(failList)
                .build();
    }

    /**
     * 从PDF文件中提取文本内容
     */
    private String extractTextFromPdf(byte[] pdfBytes) throws Exception {
        org.apache.pdfbox.pdmodel.PDDocument document = null;
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(pdfBytes);
        try {
            document = org.apache.pdfbox.pdmodel.PDDocument.load(bais);
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            return stripper.getText(document);
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 在PDF文本中查找匹配的流水记录
     */
    private FundTransaction findMatchingTransaction(String pdfText, java.util.Map<String, FundTransaction> receiptNoMap) {
        if (pdfText == null || pdfText.isEmpty() || receiptNoMap.isEmpty()) {
            return null;
        }

        // 遍历所有流水，检查PDF文本是否包含其回单编号
        for (java.util.Map.Entry<String, FundTransaction> entry : receiptNoMap.entrySet()) {
            String receiptNo = entry.getKey();
            if (receiptNo != null && !receiptNo.isEmpty()) {
                // 检查PDF文本中是否包含该回单编号
                if (pdfText.contains(receiptNo)) {
                    log.info("PDF文本匹配到回单编号：{}", receiptNo);
                    return entry.getValue();
                }
            }
        }

        return null;
    }
}

