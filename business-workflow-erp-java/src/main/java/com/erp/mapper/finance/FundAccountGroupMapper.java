package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.finance.dto.FundAccountGroupListItemResponse;
import com.erp.controller.finance.dto.FundAccountGroupPageRequest;
import com.erp.entity.finance.FundAccountGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 账户组合 Mapper
 *
 * 对应表：FUND_ACCOUNT_GROUP
 */
@Mapper
public interface FundAccountGroupMapper extends BaseMapper<FundAccountGroup> {

    /**
     * 分页查询账户组合列表（关联创建人/更新人）
     *
     * @param page    分页参数
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<FundAccountGroupListItemResponse> selectFundAccountGroupPage(Page<?> page,
                                                                        @Param("query") FundAccountGroupPageRequest request);
}


