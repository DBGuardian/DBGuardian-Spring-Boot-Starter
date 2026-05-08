package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.finance.FundSettlementCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 结账检查记录Mapper
 */
@Mapper
public interface FundSettlementCheckMapper extends BaseMapper<FundSettlementCheck> {

    /**
     * 根据账期ID查询检查记录
     */
    List<FundSettlementCheck> selectByPeriodId(@Param("periodId") Long periodId);

    /**
     * 根据账期ID和账户ID查询检查记录
     */
    FundSettlementCheck selectByPeriodIdAndAccountId(@Param("periodId") Long periodId, @Param("accountId") Long accountId);
}

