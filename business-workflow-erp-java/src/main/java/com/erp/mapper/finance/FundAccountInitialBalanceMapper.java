package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.finance.FundAccountInitialBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 账户期初余额 Mapper
 */
@Mapper
public interface FundAccountInitialBalanceMapper extends BaseMapper<FundAccountInitialBalance> {

    /**
     * 根据账户ID和账期ID查询期初余额
     *
     * @param accountId 账户ID
     * @param periodId  账期ID
     * @return 期初余额记录
     */
    FundAccountInitialBalance selectByAccountAndPeriod(@Param("accountId") Long accountId,
                                                        @Param("periodId") Long periodId);

    /**
     * 批量查询账户期初余额（查找指定账期及之前最近的期初余额）
     *
     * @param organizationId 组织ID
     * @param periodIds 账期ID列表（按时间倒序）
     * @param accountIds 账户ID列表
     * @return 账户期初余额列表
     */
    List<FundAccountInitialBalance> selectBatchNearestAccountInitialBalances(
            @Param("organizationId") Long organizationId,
            @Param("periodIds") List<Long> periodIds,
            @Param("accountIds") List<Long> accountIds);
}


