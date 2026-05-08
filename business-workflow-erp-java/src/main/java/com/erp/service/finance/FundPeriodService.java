package com.erp.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.controller.finance.dto.FundPeriodInitResponse;
import com.erp.controller.finance.dto.FundPeriodListItemResponse;
import com.erp.controller.finance.dto.FundPeriodPageRequest;
import com.erp.entity.finance.FundPeriod;

/**
 * 账期服务接口
 *
 * 账期管理已重新设计，后续将重新实现相关功能
 */
public interface FundPeriodService extends IService<FundPeriod> {

    /**
     * 初始化账期
     *
     * @param organizationId 组织ID
     * @param year 年份
     * @param overwrite 是否覆盖已存在的账期
     * @return 初始化结果
     */
    FundPeriodInitResponse initFundPeriods(Long organizationId, Integer year, Boolean overwrite);

    /**
     * 分页查询账期列表
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    IPage<FundPeriodListItemResponse> getFundPeriodPage(FundPeriodPageRequest request);

    /**
     * 查询账期年份列表
     *
     * @return 年份列表
     */
    java.util.List<Integer> getFundPeriodYears();

    /**
     * 结账
     *
     * @param periodId 账期编号
     */
    void settlePeriod(Long periodId);

    /**
     * 取消结账（反结账）
     *
     * @param periodId 账期编号
     */
    void reverseSettlePeriod(Long periodId);

}