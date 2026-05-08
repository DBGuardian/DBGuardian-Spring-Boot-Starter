package com.erp.service.finance.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.FundAccountListItemResponse;
import com.erp.controller.finance.dto.FundAccountPageRequest;
import com.erp.entity.finance.FundAccount;
import com.erp.entity.finance.FundTransaction;
import com.erp.mapper.finance.FundAccountMapper;
import com.erp.mapper.finance.FundTransactionMapper;
import com.erp.service.finance.FundAccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 资金账户服务实现
 */
@Slf4j
@Service
public class FundAccountServiceImpl extends ServiceImpl<FundAccountMapper, FundAccount> implements FundAccountService {

    @Autowired
    private FundTransactionMapper fundTransactionMapper;
    @Autowired
    private com.erp.mapper.finance.FundSettlementCheckMapper fundSettlementCheckMapper;

    // SIMPLE 模式：账户管理页面不区分数据范围，Service 层不做 viewScope/operateScope 校验。
    // 操作权限已由 Controller 层 @RequireActionPermission 注解统一控制。

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundAccount createAccount(String accountName, String accountType, String accountBankAccount, String accountBankInstitution, String remark, Long organizationId) {
        // 基本校验
        if (StrUtil.isBlank(accountName)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "账户名称不能为空");
        }
        if (StrUtil.isBlank(accountType)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "账户类型不能为空");
        }
        if (!"BANK".equals(accountType) && !"PETTY_CASH".equals(accountType) && !"CASH".equals(accountType)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "账户类型不合法，应为：BANK、PETTY_CASH 或 CASH");
        }

        // 生成账户编码：ACC-YYYYMMDD-XXXXX
        String accountCode = generateAccountCode();
        Integer currentUserId = SecurityUtil.getCurrentUserId();

        FundAccount entity = new FundAccount();
        // 设置所属组织（如果提供）
        if (organizationId != null) {
            entity.setOrganizationId(organizationId);
        }
        entity.setAccountCode(accountCode);
        entity.setAccountName(accountName.trim());
        entity.setAccountType(accountType);
        entity.setAccountBankAccount(StrUtil.trimToNull(accountBankAccount));
        entity.setAccountBankInstitution(StrUtil.trimToNull(accountBankInstitution));
        entity.setEnabled(Boolean.TRUE);
        entity.setRemark(StrUtil.trimToNull(remark));

        // 显式设置创建时间，避免数据库 NOT NULL 且无默认值时插入失败
        // 创建时不设置更新时间，仅在更新时设置
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        // 创建时仅填充创建人编码，更新人编码在后续更新操作时再维护
        if (currentUserId != null) {
            entity.setCreateUserId(currentUserId);
        }

        // version 字段使用数据库默认值 0
        this.save(entity);

        log.info("创建资金账户成功：accountId={}, accountCode={}, accountName={}",
                entity.getAccountId(), entity.getAccountCode(), entity.getAccountName());

        return entity;
    }

    /**
     * 生成资金账户编码
     * <p>
     * 规则：ACC-YYYYMMDD-XXXXX
     */
    private String generateAccountCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ACC-" + dateStr + "-";

        LambdaQueryWrapper<FundAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(FundAccount::getAccountCode, prefix)
                .orderByDesc(FundAccount::getAccountCode)
                .last("LIMIT 1");

        FundAccount maxAccount = this.getOne(wrapper);
        int sequence = 1;
        if (maxAccount != null && StrUtil.isNotBlank(maxAccount.getAccountCode())) {
            String maxCode = maxAccount.getAccountCode();
            if (maxCode.length() > prefix.length()) {
                String sequenceStr = maxCode.substring(prefix.length());
                try {
                    sequence = Integer.parseInt(sequenceStr) + 1;
                } catch (NumberFormatException e) {
                    log.warn("解析资金账户编码序号失败：maxCode={}, prefix={}", maxCode, prefix, e);
                    sequence = 1;
                }
            }
        }

        String accountCode = prefix + String.format("%05d", sequence);
        log.debug("生成资金账户编码：{}", accountCode);
        return accountCode;
    }

    @Override
    public IPage<FundAccountListItemResponse> getAccountPage(Page<?> page, FundAccountPageRequest request) {
        // SIMPLE 模式：不区分数据范围，直接查全部
        return this.baseMapper.selectFundAccountPage(page, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundAccount updateAccount(Long accountId, String accountName, String accountType, String accountBankAccount, String accountBankInstitution, Boolean enabled, String remark) {
        if (accountId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "账户ID不能为空");
        }
        if (StrUtil.isBlank(accountName)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "账户名称不能为空");
        }
        if (StrUtil.isBlank(accountType)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "账户类型不能为空");
        }
        if (!"BANK".equals(accountType) && !"PETTY_CASH".equals(accountType) && !"CASH".equals(accountType)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "账户类型不合法，应为：BANK、PETTY_CASH 或 CASH");
        }

        FundAccount entity = this.getById(accountId);
        if (entity == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "资金账户不存在");
        }
        // SIMPLE 模式：不做 operateScope 校验，操作权限已由 @RequireActionPermission 注解控制

        entity.setAccountName(accountName.trim());
        entity.setAccountType(accountType);
        entity.setAccountBankAccount(StrUtil.isBlank(accountBankAccount) ? "" : accountBankAccount.trim());
        entity.setAccountBankInstitution(StrUtil.isBlank(accountBankInstitution) ? "" : accountBankInstitution.trim());
        if (enabled != null) {
            entity.setEnabled(enabled);
        }
        entity.setRemark(StrUtil.isBlank(remark) ? "" : remark.trim());

        // 仅更新更新时间和更新人编码，不修改创建时间和创建人编码
        LocalDateTime now = LocalDateTime.now();
        entity.setUpdateTime(now);
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId != null) {
            entity.setUpdateUserId(currentUserId);
        }

        boolean success = this.updateById(entity);
        if (!success) {
            throw new BusinessException(ResultCodeEnum.ERROR.getCode(), "更新资金账户失败：记录已被其他用户修改");
        }

        log.info("更新资金账户成功：accountId={}, accountCode={}, accountName={}",
                entity.getAccountId(), entity.getAccountCode(), entity.getAccountName());

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccountWithTransactions(Long accountId) {
        if (accountId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "账户ID不能为空");
        }

        FundAccount account = this.getById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "资金账户不存在");
        }
        // SIMPLE 模式：不做 operateScope 校验，操作权限已由 @RequireActionPermission 注解控制

        // 删除该账户下所有资金流水
        LambdaQueryWrapper<FundTransaction> txWrapper = new LambdaQueryWrapper<>();
        txWrapper.eq(FundTransaction::getAccountId, accountId);
        int deletedTx = fundTransactionMapper.delete(txWrapper);
        log.info("删除资金账户[{}]的资金流水条数：{}", accountId, deletedTx);

        // 删除该账户在结账检查表中的记录（避免外键约束）
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.erp.entity.finance.FundSettlementCheck> checkWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        checkWrapper.eq(com.erp.entity.finance.FundSettlementCheck::getAccountId, accountId);
        int deletedChecks = fundSettlementCheckMapper.delete(checkWrapper);
        log.info("删除资金账户[{}]在结账检查表中的记录数：{}", accountId, deletedChecks);

        // 注意：账户现在直接通过organizationId字段从属于组织，无需删除关联记录
        log.info("资金账户[{}]已通过organizationId字段从属于组织，无需删除关联记录", accountId);

        // 删除账户本身
        boolean removed = this.removeById(accountId);
        if (!removed) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除资金账户失败");
        }

        log.info("删除资金账户成功：accountId={}, accountCode={}", account.getAccountId(), account.getAccountCode());
    }

    @Override
    public List<FundAccountListItemResponse> getAccountsByOrganizationId(Long organizationId) {
        if (organizationId == null) {
            return new java.util.ArrayList<>();
        }

        // 查询组织关联的账户ID列表
        List<Long> accountIds = this.baseMapper.selectAccountIdsByOrganizationId(organizationId);
        if (accountIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // 查询账户详情
        LambdaQueryWrapper<FundAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(FundAccount::getAccountId, accountIds);
        List<FundAccount> accounts = this.list(queryWrapper);

        // 转换为响应对象
        return accounts.stream()
                .map(this::convertToListItemResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public IPage<FundAccountListItemResponse> getAccountsByOrganizationIdPage(Page<?> page, Long organizationId, FundAccountPageRequest request) {
        if (organizationId == null) {
            return new Page<>();
        }

        log.debug("分页查询组织账户，组织ID：{}，查询条件：{}", organizationId, request);
        return this.baseMapper.selectAccountsByOrganizationIdPage(page, organizationId, request);
    }

    /**
     * 将FundAccount实体转换为FundAccountListItemResponse
     */
    private FundAccountListItemResponse convertToListItemResponse(FundAccount account) {
        FundAccountListItemResponse response = new FundAccountListItemResponse();
        response.setAccountId(account.getAccountId());
        response.setAccountCode(account.getAccountCode());
        response.setAccountName(account.getAccountName());
        response.setAccountType(account.getAccountType());
        response.setAccountBankAccount(account.getAccountBankAccount());
        response.setAccountBankInstitution(account.getAccountBankInstitution());
        response.setEnabled(account.getEnabled());
        response.setRemark(account.getRemark());
        response.setCreateTime(account.getCreateTime());
        response.setUpdateTime(account.getUpdateTime());
        // TODO: 设置创建人和更新人姓名，需要关联用户表查询
        return response;
    }
}


