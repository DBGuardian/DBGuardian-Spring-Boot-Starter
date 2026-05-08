package com.erp.service.finance.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.FundAccountGroupDetailResponse;
import com.erp.controller.finance.dto.FundAccountGroupListItemResponse;
import com.erp.controller.finance.dto.FundAccountGroupPageRequest;
import com.erp.entity.finance.FundAccount;
import com.erp.entity.finance.FundAccountGroup;
import com.erp.mapper.finance.FundAccountGroupMapper;
import com.erp.mapper.finance.FundAccountMapper;
import com.erp.service.finance.FundAccountGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 账户组合服务实现
 */
@Slf4j
@Service
public class FundAccountGroupServiceImpl extends ServiceImpl<FundAccountGroupMapper, FundAccountGroup> implements FundAccountGroupService {

    @Autowired
    private FundAccountMapper fundAccountMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundAccountGroup createGroup(String groupName, List<Long> accountIds, Boolean enabled, String remark) {
        // 基本校验
        if (StrUtil.isBlank(groupName)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "组合名称不能为空");
        }
        if (accountIds == null || accountIds.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "账户ID列表不能为空");
        }

        // 校验账户是否存在
        validateAccountIds(accountIds);

        // 生成组合编码：GRP-YYYYMMDD-XXXXX
        String groupCode = generateGroupCode();
        Integer currentUserId = SecurityUtil.getCurrentUserId();

        FundAccountGroup entity = new FundAccountGroup();
        entity.setGroupCode(groupCode);
        entity.setGroupName(groupName.trim());
        entity.setAccountIdsJson(JSONUtil.toJsonStr(accountIds));
        entity.setEnabled(enabled != null ? enabled : Boolean.TRUE); // 默认启用
        entity.setRemark(StrUtil.trimToNull(remark));

        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        if (currentUserId != null) {
            entity.setCreateUserId(currentUserId);
        }

        this.save(entity);

        log.info("创建账户组合成功：groupId={}, groupCode={}, groupName={}",
                entity.getGroupId(), entity.getGroupCode(), entity.getGroupName());

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundAccountGroup updateGroup(Long groupId, String groupName, List<Long> accountIds, Boolean enabled, String remark) {
        if (groupId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "组合ID不能为空");
        }
        if (StrUtil.isBlank(groupName)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "组合名称不能为空");
        }
        if (accountIds == null || accountIds.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "账户ID列表不能为空");
        }

        FundAccountGroup entity = this.getById(groupId);
        if (entity == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "账户组合不存在");
        }

        // 校验账户是否存在
        validateAccountIds(accountIds);

        Integer currentUserId = SecurityUtil.getCurrentUserId();

        entity.setGroupName(groupName.trim());
        entity.setAccountIdsJson(JSONUtil.toJsonStr(accountIds));
        if (enabled != null) {
            entity.setEnabled(enabled);
        }
        entity.setRemark(StrUtil.trimToNull(remark));
        entity.setUpdateTime(LocalDateTime.now());
        if (currentUserId != null) {
            entity.setUpdateUserId(currentUserId);
        }

        boolean success = this.updateById(entity);
        if (!success) {
            throw new BusinessException(ResultCodeEnum.ERROR.getCode(), "更新账户组合失败：记录已被其他用户修改");
        }

        log.info("更新账户组合成功：groupId={}, groupCode={}, groupName={}",
                entity.getGroupId(), entity.getGroupCode(), entity.getGroupName());

        return entity;
    }

    @Override
    public List<FundAccountGroupListItemResponse> getGroupList() {
        List<FundAccountGroup> groups = this.list();
        return groups.stream().map(this::convertToListItem).collect(Collectors.toList());
    }

    @Override
    public IPage<FundAccountGroupListItemResponse> getGroupPage(Page<?> page, FundAccountGroupPageRequest request) {
        // 调用Mapper分页查询
        IPage<FundAccountGroupListItemResponse> resultPage = this.baseMapper.selectFundAccountGroupPage(page, request);
        
        // 对结果进行处理，填充accountIds和accountNames
        List<FundAccountGroupListItemResponse> records = resultPage.getRecords();
        for (FundAccountGroupListItemResponse record : records) {
            // 根据groupId查询完整的组合信息，获取账户ID列表
            FundAccountGroup group = this.getById(record.getGroupId());
            if (group != null) {
                List<Long> accountIds = parseAccountIds(group.getAccountIdsJson());
                record.setAccountIds(accountIds);
                
                // 查询账户名称列表
                List<String> accountNames = getAccountNames(accountIds);
                record.setAccountNames(accountNames);
            }
        }
        
        return resultPage;
    }

    @Override
    public FundAccountGroupDetailResponse getGroupDetail(Long groupId) {
        FundAccountGroup group = this.getById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "账户组合不存在");
        }
        return convertToDetail(group);
    }

    /**
     * 生成账户组合编码
     * 规则：GRP-YYYYMMDD-XXXXX
     */
    private String generateGroupCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GRP-" + dateStr + "-";

        LambdaQueryWrapper<FundAccountGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(FundAccountGroup::getGroupCode, prefix)
                .orderByDesc(FundAccountGroup::getGroupCode)
                .last("LIMIT 1");

        FundAccountGroup maxGroup = this.getOne(wrapper);
        int sequence = 1;
        if (maxGroup != null && StrUtil.isNotBlank(maxGroup.getGroupCode())) {
            String maxCode = maxGroup.getGroupCode();
            if (maxCode.length() > prefix.length()) {
                String sequenceStr = maxCode.substring(prefix.length());
                try {
                    sequence = Integer.parseInt(sequenceStr) + 1;
                } catch (NumberFormatException e) {
                    log.warn("解析账户组合编码序号失败：maxCode={}, prefix={}", maxCode, prefix, e);
                    sequence = 1;
                }
            }
        }

        String groupCode = prefix + String.format("%05d", sequence);
        log.debug("生成账户组合编码：{}", groupCode);
        return groupCode;
    }

    /**
     * 校验账户ID列表是否有效
     */
    private void validateAccountIds(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return;
        }

        List<FundAccount> accounts = fundAccountMapper.selectBatchIds(accountIds);
        if (accounts.size() != accountIds.size()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "部分账户ID不存在");
        }

        // 检查是否有停用的账户
        List<String> disabledAccountNames = accounts.stream()
                .filter(acc -> !Boolean.TRUE.equals(acc.getEnabled()))
                .map(FundAccount::getAccountName)
                .collect(Collectors.toList());

        if (!disabledAccountNames.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    "以下账户已停用，不能添加到组合中：" + String.join("、", disabledAccountNames));
        }
    }

    /**
     * 转换为列表项响应
     */
    private FundAccountGroupListItemResponse convertToListItem(FundAccountGroup group) {
        FundAccountGroupListItemResponse response = new FundAccountGroupListItemResponse();
        response.setGroupId(group.getGroupId());
        response.setGroupCode(group.getGroupCode());
        response.setGroupName(group.getGroupName());
        response.setEnabled(group.getEnabled());
        response.setRemark(group.getRemark());

        // 解析账户ID列表
        List<Long> accountIds = parseAccountIds(group.getAccountIdsJson());
        response.setAccountIds(accountIds);

        // 查询账户名称列表
        List<String> accountNames = getAccountNames(accountIds);
        response.setAccountNames(accountNames);

        // 设置时间
        if (group.getCreateTime() != null) {
            response.setCreateTime(group.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (group.getUpdateTime() != null) {
            response.setUpdateTime(group.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        // TODO: 设置创建人/更新人姓名（需要关联用户表）

        return response;
    }

    /**
     * 转换为详情响应
     */
    private FundAccountGroupDetailResponse convertToDetail(FundAccountGroup group) {
        FundAccountGroupDetailResponse response = new FundAccountGroupDetailResponse();
        response.setGroupId(group.getGroupId());
        response.setGroupCode(group.getGroupCode());
        response.setGroupName(group.getGroupName());
        response.setEnabled(group.getEnabled());
        response.setRemark(group.getRemark());

        // 解析账户ID列表
        List<Long> accountIds = parseAccountIds(group.getAccountIdsJson());
        response.setAccountIds(accountIds);

        // 查询账户名称列表
        List<String> accountNames = getAccountNames(accountIds);
        response.setAccountNames(accountNames);

        return response;
    }

    /**
     * 解析账户ID列表JSON
     */
    private List<Long> parseAccountIds(String accountIdsJson) {
        if (StrUtil.isBlank(accountIdsJson)) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(accountIdsJson, Long.class);
        } catch (Exception e) {
            log.error("解析账户ID列表JSON失败：accountIdsJson={}", accountIdsJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取账户名称列表
     */
    private List<String> getAccountNames(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<FundAccount> accounts = fundAccountMapper.selectBatchIds(accountIds);
        return accounts.stream()
                .map(FundAccount::getAccountName)
                .collect(Collectors.toList());
    }
}

