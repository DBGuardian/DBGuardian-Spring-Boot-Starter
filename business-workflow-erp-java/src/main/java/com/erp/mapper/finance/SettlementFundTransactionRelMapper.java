package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.settlement.SettlementFundTransactionRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SettlementFundTransactionRelMapper extends BaseMapper<SettlementFundTransactionRel> {

    List<SettlementFundTransactionRel> selectBySettlementId(@Param("settlementId") Long settlementId);
}

