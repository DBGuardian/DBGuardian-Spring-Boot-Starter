package com.erp.mapper.report;

import com.erp.controller.report.dto.FundBalanceTrendResponse.AccountInitialDTO;
import com.erp.controller.report.dto.FundBalanceTrendResponse.DayFlowDTO;
import com.erp.controller.report.dto.FundBalanceTrendResponse.TransactionFlowDTO;
import com.erp.controller.report.dto.FundBalanceTrendResponse.YearlyBalanceDTO;
import com.erp.entity.finance.FundAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资金余额变动趋势 Mapper
 *
 * 功能描述：查询启用账户列表、期初余额及区间流水
 */
@Mapper
public interface FundBalanceTrendMapper {

    /**
     * 查询所有启用的资金账户（可按 accountIds 过滤）
     *
     * @param accountIds 账户ID过滤列表，为空则查询所有启用账户
     * @return 启用账户列表
     */
    List<FundAccount> selectEnabledAccounts(@Param("accountIds") List<Long> accountIds);

    /**
     * 查询账户期初余额
     *
     * @param accountIds 账户ID列表
     * @param startDate  查询开始日期（YYYY-MM-DD）
     * @return 每个账户的期初余额（含账期开始日期）
     */
    List<AccountInitialDTO> selectInitialBalances(
            @Param("accountIds") List<Long> accountIds,
            @Param("startDate") String startDate,
            @Param("useLatestClosedPeriodFallback") boolean useLatestClosedPeriodFallback);

    /**
     * 查询区间内各账户每日资金流水汇总（分组到日期 + 账户）
     *
     * @param accountIds 账户ID列表
     * @param startDate  开始日期（包含）
     * @param endDate    结束日期（包含）
     * @return 各账户每日收支汇总列表
     */
    List<DayFlowDTO> selectDayFlows(
            @Param("accountIds") List<Long> accountIds,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 查询区间结束日对应的最新余额快照。
     * 优先取结束日期所在月份的期初余额，再叠加当月截至结束日期的流水净额。
     *
     * @param accountIds 账户ID列表
     * @param endDate    截止日期（YYYY-MM-DD）
     * @return 各账户截至 endDate 的余额
     */
    List<AccountInitialDTO> selectEndDateBalances(
            @Param("accountIds") List<Long> accountIds,
            @Param("endDate") String endDate,
            @Param("useLatestClosedPeriodFallback") boolean useLatestClosedPeriodFallback);

    /**
     * 按年查询各账户余额：取每年最后一个已结账账期的期初余额
     *
     * @param accountIds 账户ID列表
     * @param startYear  起始年份（含）
     * @param endYear    结束年份（含）
     * @return 每条记录包含 accountId、initialBalance、year
     */
    List<YearlyBalanceDTO> selectYearlyClosedBalances(
            @Param("accountIds") List<Long> accountIds,
            @Param("startYear") int startYear,
            @Param("endYear") int endYear);
}
