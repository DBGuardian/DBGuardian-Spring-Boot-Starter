package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.finance.dto.FundOrganizationListItemResponse;
import com.erp.controller.finance.dto.FundOrganizationPageRequest;
import com.erp.controller.finance.dto.OrganizationTreeNode;
import java.util.List;
import com.erp.entity.finance.FundOrganization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资金组织 Mapper
 *
 * 对应表：fund_organization
 *
 * 提供组织的基本CRUD操作和分页查询功能
 */
@Mapper
public interface FundOrganizationMapper extends BaseMapper<FundOrganization> {

    /**
     * 分页查询资金组织列表（关联创建人/更新人）
     *
     * @param page    分页参数
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<FundOrganizationListItemResponse> selectFundOrganizationPage(Page<?> page,
                                                                       @Param("query") FundOrganizationPageRequest request);

    /**
     * 根据组织ID查询组织详情（包含账户数量统计）
     *
     * @param organizationId 组织ID
     * @return 组织详情
     */
    FundOrganizationListItemResponse selectFundOrganizationDetail(@Param("organizationId") Long organizationId);

    /**
     * 查询所有组织（用于构建树形结构，包含账户数量）
     *
     * @return 组织列表（包含账户数量）
     */
    List<FundOrganizationListItemResponse> selectAllOrganizations();

    /**
     * 根据组织ID查询关联的账户列表（用于构建树形结构）
     *
     * @param organizationId 组织ID
     * @return 账户列表
     */
    List<com.erp.controller.finance.dto.FundAccountListItemResponse> selectAccountsByOrganizationId(@Param("organizationId") Long organizationId);

}
