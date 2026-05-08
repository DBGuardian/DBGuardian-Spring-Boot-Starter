package com.erp.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.controller.finance.dto.FundAccountListItemResponse;
import com.erp.controller.finance.dto.FundAccountPageRequest;
import com.erp.entity.finance.FundAccount;

import java.util.List;

/**
 * 资金账户服务接口
 *
 * 提供账户创建、基础校验等功能。
 */
public interface FundAccountService extends IService<FundAccount> {

    /**
     * 创建资金账户
     *
     * @param accountName           账户名称
     * @param accountType           账户类型：BANK、PETTY_CASH、CASH
     * @param accountBankAccount    账户银行账号
     * @param accountBankInstitution 账户银行/机构
     * @param remark                备注
     * @return 创建后的资金账户实体
     */
    FundAccount createAccount(String accountName, String accountType, String accountBankAccount, String accountBankInstitution, String remark, Long organizationId);

    /**
     * 分页查询资金账户列表
     *
     * @param page    分页参数
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<FundAccountListItemResponse> getAccountPage(Page<?> page, FundAccountPageRequest request);

    /**
     * 更新资金账户
     *
     * @param accountId             账户ID
     * @param accountName           账户名称
     * @param accountType           账户类型：BANK、PETTY_CASH、CASH
     * @param accountBankAccount    账户银行账号
     * @param accountBankInstitution 账户银行/机构
     * @param enabled               是否启用
     * @param remark                备注
     * @return 更新后的资金账户实体
     */
    FundAccount updateAccount(Long accountId, String accountName, String accountType, String accountBankAccount, String accountBankInstitution, Boolean enabled, String remark);

    /**
     * 删除资金账户及其所有资金流水（事务）
     *
     * @param accountId 账户ID
     */
    void deleteAccountWithTransactions(Long accountId);

    /**
     * 根据组织ID获取账户列表
     *
     * @param organizationId 组织ID
     * @return 账户列表
     */
    List<FundAccountListItemResponse> getAccountsByOrganizationId(Long organizationId);

    /**
     * 分页查询组织下的账户
     *
     * @param page          分页参数
     * @param organizationId 组织ID
     * @param request       查询条件
     * @return 分页结果
     */
    IPage<FundAccountListItemResponse> getAccountsByOrganizationIdPage(Page<?> page, Long organizationId, FundAccountPageRequest request);
}


