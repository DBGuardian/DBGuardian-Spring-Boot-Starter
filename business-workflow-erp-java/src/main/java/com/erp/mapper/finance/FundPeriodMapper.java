package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.finance.dto.FundPeriodListItemResponse;
import com.erp.controller.finance.dto.FundPeriodPageRequest;
import com.erp.entity.finance.FundPeriod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 账期 Mapper
 *
 * 对应表：FUND_PERIOD
 * 账期管理已重新设计，后续将重新实现相关功能
 */
@Mapper
public interface FundPeriodMapper extends BaseMapper<FundPeriod> {

    // 账期管理相关方法将重新实现
    // 保留基础Mapper供后续扩展

    /**
     * 分页查询账期列表
     *
     * @param page 分页对象
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<FundPeriodListItemResponse> selectFundPeriodPage(Page<FundPeriodListItemResponse> page, @Param("query") FundPeriodPageRequest request);

    /**
     * 查询账期年份列表
     *
     * @return 年份列表
     */
    java.util.List<Integer> selectFundPeriodYears();

}


