package com.erp.service.finance.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.*;
import com.erp.entity.finance.FundAccount;
import com.erp.entity.finance.FundOrganization;
import com.erp.service.finance.FundAccountService;
import com.erp.mapper.finance.FundOrganizationMapper;
import com.erp.service.finance.FundOrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资金组织服务实现
 */
@Slf4j
@Service
public class FundOrganizationServiceImpl extends ServiceImpl<FundOrganizationMapper, FundOrganization> implements FundOrganizationService {

    @Autowired
    private FundAccountService fundAccountService;

    // SIMPLE 模式：账户管理页面不区分数据范围，Service 层不做 viewScope/operateScope 校验。
    // 操作权限已由 Controller 层 @RequireActionPermission 注解统一控制。

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundOrganization createOrganization(FundOrganizationCreateRequest request) {
        log.info("创建资金组织，组织名称：{}", request.getOrganizationName());

        // 基本校验
        if (StrUtil.isBlank(request.getOrganizationName())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "组织名称不能为空");
        }

        // 检查组织名称是否重复
        LambdaQueryWrapper<FundOrganization> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundOrganization::getOrganizationName, request.getOrganizationName());
        if (this.count(queryWrapper) > 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "组织名称已存在");
        }

        // 生成组织编码：ORG-YYYYMMDD-XXXXX
        String organizationCode = generateOrganizationCode();

        // 创建组织实体
        FundOrganization organization = new FundOrganization();
        BeanUtils.copyProperties(request, organization);
        organization.setOrganizationCode(organizationCode);
        organization.setEnabled(true); // 默认启用
        organization.setCreateUserId(SecurityUtil.getCurrentUserId().longValue());
        organization.setUpdateUserId(SecurityUtil.getCurrentUserId().longValue());

        // 保存组织
        if (!this.save(organization)) {
            throw new BusinessException(ResultCodeEnum.ERROR.getCode(), "创建资金组织失败");
        }

        log.info("创建资金组织成功，组织ID：{}，组织编码：{}", organization.getOrganizationId(), organization.getOrganizationCode());
        return organization;
    }

    @Override
    public IPage<FundOrganizationListItemResponse> getOrganizationPage(Page<?> page, FundOrganizationPageRequest request) {
        log.debug("分页查询资金组织，查询条件：{}", request);
        // SIMPLE 模式：不区分数据范围，直接查全部
        return this.baseMapper.selectFundOrganizationPage(page, request);
    }

    @Override
    public FundOrganizationListItemResponse getOrganizationDetail(Long organizationId) {
        log.debug("查询资金组织详情，组织ID：{}", organizationId);

        if (organizationId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "组织ID不能为空");
        }

        FundOrganizationListItemResponse detail = this.baseMapper.selectFundOrganizationDetail(organizationId);
        if (detail == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "资金组织不存在");
        }

        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundOrganization updateOrganization(Long organizationId, FundOrganizationUpdateRequest request) {
        log.info("更新资金组织，组织ID：{}，更新内容：{}", organizationId, request);

        if (organizationId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "组织ID不能为空");
        }

        // 检查组织是否存在
        FundOrganization existingOrg = this.getById(organizationId);
        if (existingOrg == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "资金组织不存在");
        }

        // 操作范围校验：SIMPLE 模式不做 operateScope 校验，操作权限已由 @RequireActionPermission 注解控制

        // 检查组织名称是否重复（排除自身）
        if (StrUtil.isNotBlank(request.getOrganizationName())) {
            LambdaQueryWrapper<FundOrganization> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FundOrganization::getOrganizationName, request.getOrganizationName())
                       .ne(FundOrganization::getOrganizationId, organizationId);
            if (this.count(queryWrapper) > 0) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "组织名称已存在");
            }
        }

        // 更新组织信息
        BeanUtils.copyProperties(request, existingOrg);
        existingOrg.setUpdateUserId(SecurityUtil.getCurrentUserId().longValue());

        if (!this.updateById(existingOrg)) {
            throw new BusinessException(ResultCodeEnum.ERROR.getCode(), "更新资金组织失败");
        }

        log.info("更新资金组织成功，组织ID：{}", organizationId);
        return existingOrg;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrganization(Long organizationId) {
        log.info("删除资金组织，组织ID：{}", organizationId);

        if (organizationId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "组织ID不能为空");
        }

        // 检查组织是否存在
        FundOrganization organization = this.getById(organizationId);
        if (organization == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "资金组织不存在");
        }

        // 操作范围校验：SIMPLE 模式不做 operateScope 校验，操作权限已由 @RequireActionPermission 注解控制

        // 检查组织下是否有账户关联
        List<Long> accountIds = this.getOrganizationAccountIds(organizationId);
        if (!CollectionUtils.isEmpty(accountIds)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                "该组织下还有" + accountIds.size() + "个账户关联，无法删除。请先移除所有账户关联。");
        }

        // 删除组织（级联删除会自动处理关联表）
        if (!this.removeById(organizationId)) {
            throw new BusinessException(ResultCodeEnum.ERROR.getCode(), "删除资金组织失败");
        }

        log.info("删除资金组织成功，组织ID：{}", organizationId);
    }

    @Override
    public List<Long> getOrganizationAccountIds(Long organizationId) {
        if (organizationId == null) {
            return new ArrayList<>();
        }

        // 直接从fund_account表查询该组织下的所有账户ID
        LambdaQueryWrapper<FundAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundAccount::getOrganizationId, organizationId)
                   .select(FundAccount::getAccountId);
        List<FundAccount> accounts = fundAccountService.list(queryWrapper);
        return accounts.stream()
                      .map(FundAccount::getAccountId)
                      .collect(Collectors.toList());
    }

    // 注意：现在账户直接通过organizationId字段从属于组织，不再需要关联操作
    // addAccountToOrganization和removeAccountFromOrganization方法已不再适用

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAccountToOrganization(Long organizationId, Long accountId) {
        throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
            "该方法已废弃，现在账户直接从属于组织，请使用updateAccountOrganization方法");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAccountFromOrganization(Long organizationId, Long accountId) {
        throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
            "该方法已废弃，现在账户直接从属于组织，无法移除关联");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IPage<FundAccountListItemResponse> getOrganizationAccountsPage(Page<FundAccountListItemResponse> page, Long organizationId, FundAccountPageRequest request) {
        log.info("分页查询组织账户，organizationId={}, request={}", organizationId, request);

        if (organizationId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "组织ID不能为空");
        }

        // 创建用于查询的Page对象
        Page<FundAccount> queryPage = new Page<>(page.getCurrent(), page.getSize());

        // 构建查询条件
        LambdaQueryWrapper<FundAccount> wrapper = new LambdaQueryWrapper<>();

        // 直接查询条件：通过organizationId字段关联
        wrapper.eq(FundAccount::getOrganizationId, organizationId);

        // 搜索条件
        if (StringUtils.isNotBlank(request.getAccountName())) {
            wrapper.like(FundAccount::getAccountName, request.getAccountName());
        }
        if (StringUtils.isNotBlank(request.getAccountType())) {
            wrapper.eq(FundAccount::getAccountType, request.getAccountType());
        }
        if (request.getIsEnabled() != null) {
            wrapper.eq(FundAccount::getEnabled, request.getIsEnabled());
        }

        // 排序
        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();
        if (StringUtils.isNotBlank(sortField)) {
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "accountCode":
                    wrapper.orderBy(true, isAsc, FundAccount::getAccountCode);
                    break;
                case "accountName":
                    wrapper.orderBy(true, isAsc, FundAccount::getAccountName);
                    break;
                case "createTime":
                    wrapper.orderBy(true, isAsc, FundAccount::getCreateTime);
                    break;
                case "updateTime":
                    wrapper.orderBy(true, isAsc, FundAccount::getUpdateTime);
                    break;
                default:
                    wrapper.orderByDesc(FundAccount::getCreateTime);
            }
        } else {
            wrapper.orderByDesc(FundAccount::getCreateTime);
        }

        // 执行分页查询
        IPage<FundAccount> accountPage = this.fundAccountService.page(queryPage, wrapper);

        // 转换为响应对象
        List<FundAccountListItemResponse> responseList = accountPage.getRecords().stream()
            .map(this::convertToAccountResponse)
            .collect(Collectors.toList());

        // 构建返回的分页结果
        Page<FundAccountListItemResponse> responsePage = new Page<FundAccountListItemResponse>(
            accountPage.getCurrent(),
            accountPage.getSize(),
            accountPage.getTotal()
        );
        responsePage.setPages(accountPage.getPages());
        responsePage.setRecords(responseList);

        return responsePage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrganizationAccounts(Long organizationId, List<Long> accountIds) {
        log.info("批量更新组织账户关联，组织ID：{}，账户IDs：{}", organizationId, accountIds);

        if (organizationId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "组织ID不能为空");
        }

        // 检查组织是否存在
        if (this.getById(organizationId) == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "资金组织不存在");
        }

        // 批量更新账户的organizationId字段
        if (!CollectionUtils.isEmpty(accountIds)) {
            // 将这些账户的organizationId设置为指定组织
            List<Long> distinctAccountIds = accountIds.stream().distinct().collect(Collectors.toList());

            // 先将这些账户从其他组织移除（设置为null）
            LambdaQueryWrapper<FundAccount> clearWrapper = new LambdaQueryWrapper<>();
            clearWrapper.in(FundAccount::getAccountId, distinctAccountIds);
            FundAccount clearAccount = new FundAccount();
            clearAccount.setOrganizationId(null);
            clearAccount.setUpdateTime(LocalDateTime.now());
            clearAccount.setUpdateUserId(SecurityUtil.getCurrentUserId());
            fundAccountService.update(clearAccount, clearWrapper);

            // 然后将这些账户设置为新组织
            FundAccount updateAccount = new FundAccount();
            updateAccount.setOrganizationId(organizationId);
            updateAccount.setUpdateTime(LocalDateTime.now());
            updateAccount.setUpdateUserId(SecurityUtil.getCurrentUserId());
            fundAccountService.update(updateAccount, clearWrapper);
        }

        log.info("批量更新组织账户关联成功，组织ID：{}，关联账户数量：{}", organizationId,
                CollectionUtils.isEmpty(accountIds) ? 0 : accountIds.size());
    }

    @Override
    public List<Long> getAccountOrganizationIds(Long accountId) {
        if (accountId == null) {
            return new ArrayList<>();
        }

        // 直接从fund_account表查询账户所属的组织
        FundAccount account = fundAccountService.getById(accountId);
        if (account != null && account.getOrganizationId() != null) {
            return Arrays.asList(account.getOrganizationId());
        }

        return new ArrayList<>();
    }

    /**
     * 生成组织编码：ORG-YYYYMMDD-XXXXX
     */
    private String generateOrganizationCode() {
        // 获取当前日期
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 查询今日已创建的组织数量
        LambdaQueryWrapper<FundOrganization> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(FundOrganization::getOrganizationCode, "ORG-" + dateStr + "-");
        long count = this.count(queryWrapper);

        // 生成5位序号（从00001开始）
        String sequence = String.format("%05d", count + 1);

        return "ORG-" + dateStr + "-" + sequence;
    }

    @Override
    public List<OrganizationTreeNode> getOrganizationTree() {
        log.info("开始构建组织树形结构");

        List<OrganizationTreeNode> treeNodes = new ArrayList<>();

        try {
            // 1. 查询所有组织（包含账户数量）
            List<FundOrganizationListItemResponse> organizations = this.baseMapper.selectAllOrganizations();
            log.debug("查询到 {} 个组织", organizations.size());

            // 2. 为每个组织构建树形节点
            for (FundOrganizationListItemResponse org : organizations) {
                log.debug("处理组织: {} (ID: {}), 账户数量: {}", org.getOrganizationName(), org.getOrganizationId(), org.getAccountCount());

                OrganizationTreeNode orgNode = new OrganizationTreeNode();
                orgNode.setId(String.valueOf(org.getOrganizationId()));
                orgNode.setType("organization");
                orgNode.setLabel(org.getOrganizationName());
                orgNode.setData(org);

                // 3. 查询该组织下的账户
                List<FundAccountListItemResponse> accounts = this.baseMapper.selectAccountsByOrganizationId(org.getOrganizationId());
                log.debug("组织 {} 下有 {} 个账户", org.getOrganizationName(), accounts.size());

                List<OrganizationTreeNode> accountNodes = new ArrayList<>();
                for (FundAccountListItemResponse account : accounts) {
                    OrganizationTreeNode accountNode = new OrganizationTreeNode();
                    accountNode.setId("account_" + account.getAccountId());
                    accountNode.setType("account");
                    accountNode.setLabel(account.getAccountName());
                    accountNode.setParentId(String.valueOf(org.getOrganizationId()));
                    accountNode.setData(account);
                    accountNodes.add(accountNode);
                }

                orgNode.setChildren(accountNodes);
                treeNodes.add(orgNode);
            }

            log.info("组织树形结构构建完成，共 {} 个节点", treeNodes.size());
        } catch (Exception e) {
            log.error("构建组织树形结构失败", e);
            throw new BusinessException(ResultCodeEnum.ERROR.getCode(), "构建组织树形结构失败：" + e.getMessage());
        }

        return treeNodes;
    }

    /**
     * 将 FundAccount 转换为 FundAccountListItemResponse
     */
    private FundAccountListItemResponse convertToAccountResponse(FundAccount account) {
        FundAccountListItemResponse response = new FundAccountListItemResponse();
        BeanUtils.copyProperties(account, response);

        // 设置创建人和更新人姓名（如果需要的话）
        if (account.getCreateUserId() != null) {
            // 这里可以根据用户ID查询用户名，暂时保持为空
            response.setCreateUserName("");
        }
        if (account.getUpdateUserId() != null) {
            response.setUpdateUserName("");
        }

        return response;
    }

    @Override
    public List<OrganizationTreeNode> searchOrganizations(String keyword, Integer limit) {
        log.info("搜索资金组织选项，keyword={}, limit={}", keyword, limit);

        // 构建查询条件
        LambdaQueryWrapper<FundOrganization> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundOrganization::getEnabled, 1); // 只查询启用的组织

        // 如果有搜索关键词，进行模糊搜索
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.like(FundOrganization::getOrganizationName, keyword.trim());
        }

        // 按创建时间倒序排序，取前limit条记录
        queryWrapper.orderByDesc(FundOrganization::getCreateTime)
                   .last("LIMIT " + limit);

        List<FundOrganization> organizations = this.list(queryWrapper);

        // 转换为OrganizationTreeNode格式
        return organizations.stream().map(org -> {
            OrganizationTreeNode node = new OrganizationTreeNode();
            node.setId(String.valueOf(org.getOrganizationId()));
            node.setType("organization");
            node.setLabel(org.getOrganizationName());
            node.setParentId(null); // 组织表不支持层级结构，设为null
            node.setData(org); // 将完整的组织信息作为data
            // 不包含children，因为这是搜索结果
            node.setChildren(null);
            return node;
        }).collect(Collectors.toList());
    }

}
