package com.erp.mapper.report;

import com.erp.controller.report.dto.ContractSignTrendResponse.ContractSignCountDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 合同签订数量变动趋势 Mapper
 *
 * 功能描述：
 * - 查询各计价类型合同在时间粒度内的签订数量
 * - 统计口径：CONTRACT.签订日期（非系统创建时间）
 * - 排除合同状态为草稿和作废的合同
 * - 计价类型判断：CONTRACT → CONTRACT_ITEM.报价模式（总价包干/按量结算 → PACKAGE/UNIT/MIXED）
 */
@Mapper
public interface ContractSignTrendMapper {

    /**
     * 按时间粒度查询区间内各计价类型的合同签订数量
     *
     * 统计口径：CONTRACT.签订日期
     * 排除：合同状态 IN ('草稿', '作废') 或 签订日期 IS NULL
     * 计价类型：通过 CONTRACT_ITEM.报价模式 判断 PACKAGE / UNIT / MIXED
     *
     * dateLabel 格式：
     *   day:   YYYY-MM-DD
     *   month: YYYY-MM
     *   year:  YYYY
     *
     * @param startDate   开始日期（YYYY-MM-DD，含）
     * @param endDate     结束日期（YYYY-MM-DD，含）
     * @param granularity 时间粒度：day / month / year
     * @return 各粒度各计价类型的签订数量列表
     */
    List<ContractSignCountDTO> selectSignCountByPeriod(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("granularity") String granularity);
}
