package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.controller.finance.dto.FundSummaryTransactionDTO;
import com.erp.entity.finance.FundTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 资金流水 Mapper
 */
@Mapper
public interface FundTransactionMapper extends BaseMapper<FundTransaction> {

    /**
     * 查询汇总表所需的资金流水数据（连表查询科目信息，合并季度内、季度前、全年查询）
     *
     * @param accountIds 账户ID列表
     * @param quarterStartDate 季度开始日期
     * @param quarterEndDate 季度结束日期
     * @param yearStartDate 年初日期
     * @param yearEndDate 年末日期
     * @return 汇总流水数据
     */
    List<FundSummaryTransactionDTO> selectFundSummaryTransactions(
            @Param("accountIds") List<Long> accountIds,
            @Param("quarterStartDate") LocalDate quarterStartDate,
            @Param("quarterEndDate") LocalDate quarterEndDate,
            @Param("yearStartDate") LocalDate yearStartDate,
            @Param("yearEndDate") LocalDate yearEndDate);
}









































































