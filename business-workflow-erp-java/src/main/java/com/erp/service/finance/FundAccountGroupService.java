package com.erp.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.controller.finance.dto.FundAccountGroupDetailResponse;
import com.erp.controller.finance.dto.FundAccountGroupListItemResponse;
import com.erp.controller.finance.dto.FundAccountGroupPageRequest;
import com.erp.entity.finance.FundAccountGroup;

import java.util.List;

/**
 * 账户组合服务接口
 */
public interface FundAccountGroupService extends IService<FundAccountGroup> {

    /**
     * 创建账户组合
     *
     * @param groupName  组合名称
     * @param accountIds 账户ID列表
     * @param enabled    是否启用
     * @param remark    备注
     * @return 创建后的账户组合实体
     */
    FundAccountGroup createGroup(String groupName, List<Long> accountIds, Boolean enabled, String remark);

    /**
     * 更新账户组合
     *
     * @param groupId   组合ID
     * @param groupName 组合名称
     * @param accountIds 账户ID列表
     * @param enabled   是否启用
     * @param remark    备注
     * @return 更新后的账户组合实体
     */
    FundAccountGroup updateGroup(Long groupId, String groupName, List<Long> accountIds, Boolean enabled, String remark);

    /**
     * 查询所有账户组合列表
     *
     * @return 账户组合列表
     */
    List<FundAccountGroupListItemResponse> getGroupList();

    /**
     * 分页查询账户组合列表
     *
     * @param page    分页参数
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<FundAccountGroupListItemResponse> getGroupPage(Page<?> page, FundAccountGroupPageRequest request);

    /**
     * 查询账户组合详情
     *
     * @param groupId 组合ID
     * @return 账户组合详情
     */
    FundAccountGroupDetailResponse getGroupDetail(Long groupId);
}

