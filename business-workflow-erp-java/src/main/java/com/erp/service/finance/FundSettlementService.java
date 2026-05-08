package com.erp.service.finance;

import com.erp.controller.settlement.dto.SettlementCheckAndSettleRequest;
import com.erp.controller.settlement.dto.SettlementCheckAndSettleResponse;
import com.erp.controller.settlement.dto.SettlementCheckItemResponse;
import com.erp.controller.settlement.dto.SettlementCheckItemUpdateRequest;
import com.erp.controller.settlement.dto.SettlementRequest;
import com.erp.controller.settlement.dto.SettlementResponse;
import com.erp.controller.settlement.dto.SettlementReverseRequest;
import com.erp.controller.settlement.dto.SettlementReverseResponse;

/**
 * 结账服务接口
 */
public interface FundSettlementService {

    /**
     * 检查并结账（只实现检查部分，不实现结账逻辑）
     * 
     * @param request 检查并结账请求
     * @return 检查结果
     */
    SettlementCheckAndSettleResponse checkAndSettle(SettlementCheckAndSettleRequest request);

    /**
     * 单个账期结账
     *
     * @param request 结账请求
     * @return 结账结果
     */
    SettlementResponse settlePeriod(SettlementRequest request);

    /**
     * 单个账期反结账
     *
     * @param request 反结账请求
     * @return 反结账结果
     */
    SettlementReverseResponse reverseSettlementPeriod(SettlementReverseRequest request);

    /**
     * 获取检查项设置
     * 
     * @return 检查项设置列表
     */
    SettlementCheckItemResponse getCheckItems();

    /**
     * 更新检查项设置
     * 
     * @param request 更新请求
     */
    void updateCheckItems(SettlementCheckItemUpdateRequest request);
}

