package com.erp.service.finance;

import com.erp.controller.finance.dto.AvailableWarehousingVO;
import com.erp.controller.finance.dto.TransportRecordDTO;
import com.erp.controller.finance.dto.WarehousingWasteDetailVO;
import com.erp.controller.finance.dto.WarehousingWasteDetailWithContractRequest;
import com.erp.controller.finance.dto.WarehousingWasteDetailWithContractResponse;

import java.util.List;

/**
 * 财务管理服务接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
public interface FinanceService {
    // TODO: 定义财务管理相关方法

    /**
     * 根据合同号获取可结算的入库单列表
     * 业务链：合同 → 收运通知单 → 运输单 → 入库单（未被其他结算单引用）
     *
     * @param contractCode 合同号
     * @return 可结算入库单列表
     */
    List<AvailableWarehousingVO> getAvailableWarehousingByContract(String contractCode);

    /**
     * 根据入库单号列表获取入库单危废明细
     *
     * @param warehousingCodes 入库单号列表
     * @return 入库单危废明细列表
     */
    List<WarehousingWasteDetailVO> getWarehousingWasteDetailsByCodes(List<String> warehousingCodes);

    /**
     * 根据入库单号列表获取入库单详情
     *
     * @param warehousingCodes 入库单号列表
     * @return 入库单详情列表
     */
    List<AvailableWarehousingVO> getWarehousingDetailsByCodes(List<String> warehousingCodes);

    /**
     * 根据合同号获取可结算的运输记录
     * 业务链：合同 → 收运通知单 → 运输单（未被其他结算单引用）
     *
     * @param contractCode 合同号
     * @return 运输记录列表
     */
    List<TransportRecordDTO> getTransportRecordsByContract(String contractCode);

    /**
     * 获取入库危废明细（含合同匹配信息）
     * 后端统一处理入库数据与合同数据的匹配逻辑
     * 关联校验：contractId + contractNo + 入库单编号（双层锁定：入库单号+入库单编号必须同时匹配）
     *
     * @param warehousingList 入库单信息列表（双层锁定）
     * @param contractId 合同编号（自增主键）
     * @param contractNo 合同号（业务可见编号）
     * @return 匹配后的完整数据
     */
    WarehousingWasteDetailWithContractResponse getWarehousingWasteDetailsWithContract(
            List<WarehousingWasteDetailWithContractRequest.WarehousingItemDTO> warehousingList,
            Integer contractId,
            String contractNo);
}











































