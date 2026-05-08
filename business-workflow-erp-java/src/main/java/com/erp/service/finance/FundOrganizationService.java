package com.erp.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.controller.finance.dto.*;
import com.erp.entity.finance.FundOrganization;

import java.util.List;

/**
 * 资金组织服务接口
 *
 * 提供组织创建、账户关联管理等功能。
 */
public interface FundOrganizationService extends IService<FundOrganization> {

    /**
     * 创建资金组织
     *
     * @param request 创建请求
     * @return 创建后的资金组织实体
     */
    FundOrganization createOrganization(FundOrganizationCreateRequest request);

    /**
     * 分页查询资金组织列表
     *
     * @param page    分页参数
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<FundOrganizationListItemResponse> getOrganizationPage(Page<?> page, FundOrganizationPageRequest request);

    /**
     * 获取组织详情
     *
     * @param organizationId 组织ID
     * @return 组织详情
     */
    FundOrganizationListItemResponse getOrganizationDetail(Long organizationId);

    /**
     * 更新资金组织
     *
     * @param organizationId 组织ID
     * @param request        更新请求
     * @return 更新后的资金组织实体
     */
    FundOrganization updateOrganization(Long organizationId, FundOrganizationUpdateRequest request);

    /**
     * 搜索资金组织选项
     *
     * @param keyword 搜索关键词，支持组织名称模糊搜索
     * @param limit   返回结果数量限制
     * @return 组织选项列表
     */
    List<OrganizationTreeNode> searchOrganizations(String keyword, Integer limit);

    /**
     * 删除资金组织
     *
     * @param organizationId 组织ID
     */
    void deleteOrganization(Long organizationId);

    /**
     * 获取组织下的账户ID列表
     *
     * @param organizationId 组织ID
     * @return 账户ID列表
     */
    List<Long> getOrganizationAccountIds(Long organizationId);

    /**
     * 为组织添加账户
     *
     * @param organizationId 组织ID
     * @param accountId      账户ID
     */
    void addAccountToOrganization(Long organizationId, Long accountId);

    /**
     * 从组织移除账户
     *
     * @param organizationId 组织ID
     * @param accountId      账户ID
     */
    void removeAccountFromOrganization(Long organizationId, Long accountId);

    /**
     * 批量更新组织账户关联关系
     *
     * @param organizationId 组织ID
     * @param accountIds     账户ID列表
     */
    void updateOrganizationAccounts(Long organizationId, List<Long> accountIds);

    /**
     * 获取账户所属的组织列表
     *
     * @param accountId 账户ID
     * @return 组织ID列表
     */
    List<Long> getAccountOrganizationIds(Long accountId);

    /**
     * 分页查询组织账户列表
     *
     * @param page          分页参数
     * @param organizationId 组织ID
     * @param request       查询条件
     * @return 分页结果
     */
    IPage<FundAccountListItemResponse> getOrganizationAccountsPage(Page<FundAccountListItemResponse> page, Long organizationId, FundAccountPageRequest request);

    /**
     * 获取组织树形结构
     *
     * @return 树形节点列表
     */
    List<com.erp.controller.finance.dto.OrganizationTreeNode> getOrganizationTree();
}

