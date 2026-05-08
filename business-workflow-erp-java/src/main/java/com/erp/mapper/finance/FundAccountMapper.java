package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.finance.dto.FundAccountListItemResponse;
import com.erp.controller.finance.dto.FundAccountPageRequest;
import com.erp.entity.finance.FundAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资金账户 Mapper
 *
 * 对应表：FUND_ACCOUNT
 */
@Mapper
public interface FundAccountMapper extends BaseMapper<FundAccount> {

    /**
     * 分页查询资金账户列表（关联创建人/更新人）
     *
     * @param page    分页参数
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<FundAccountListItemResponse> selectFundAccountPage(Page<?> page,
                                                             @Param("query") FundAccountPageRequest request);

    /**
     * 根据组织ID查询账户ID列表
     *
     * @param organizationId 组织ID
     * @return 账户ID列表
     */
    List<Long> selectAccountIdsByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * 分页查询组织下的账户
     *
     * @param page          分页参数
     * @param organizationId 组织ID
     * @param request       查询条件
     * @return 分页结果
     */
    IPage<FundAccountListItemResponse> selectAccountsByOrganizationIdPage(Page<?> page,
                                                                          @Param("organizationId") Long organizationId,
                                                                          @Param("query") FundAccountPageRequest request);
}


