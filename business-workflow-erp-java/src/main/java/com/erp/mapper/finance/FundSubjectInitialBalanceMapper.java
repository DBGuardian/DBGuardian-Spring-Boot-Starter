package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.finance.FundSubjectInitialBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FundSubjectInitialBalanceMapper extends BaseMapper<FundSubjectInitialBalance> {

    /**
     * 批量查询科目期初余额（查找指定账期及之前最近的期初余额）
     *
     * @param organizationId 组织ID
     * @param periodIds 账期ID列表（按时间倒序）
     * @param subjectIds 科目ID列表
     * @return 科目期初余额列表
     */
    List<FundSubjectInitialBalance> selectBatchNearestSubjectInitialBalances(
            @Param("organizationId") Long organizationId,
            @Param("periodIds") List<Long> periodIds,
            @Param("subjectIds") List<Long> subjectIds);
}

