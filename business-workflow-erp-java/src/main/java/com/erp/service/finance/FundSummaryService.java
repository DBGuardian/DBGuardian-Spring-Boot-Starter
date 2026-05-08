package com.erp.service.finance;

import com.erp.controller.finance.dto.FundSummaryRequest;
import com.erp.controller.finance.dto.FundSummaryResponse;

/**
 * 汇总表服务接口
 */
public interface FundSummaryService {

    /**
     * 获取汇总表数据
     *
     * @param request 查询请求
     * @return 汇总表响应数据
     */
    FundSummaryResponse getSummary(FundSummaryRequest request);
}

