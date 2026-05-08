package com.erp.service.finance.impl;

import com.erp.controller.finance.dto.AvailableWarehousingVO;
import com.erp.controller.finance.dto.SettledQuantityDTO;
import com.erp.controller.finance.dto.TransportRecordDTO;
import com.erp.controller.finance.dto.WarehousingWasteDetailVO;
import com.erp.controller.finance.dto.WarehousingWasteDetailWithContractRequest;
import com.erp.controller.finance.dto.WarehousingWasteDetailWithContractResponse;
import com.erp.controller.finance.dto.WarehousingWasteDetailWithContractVO;
import com.erp.mapper.finance.FinanceMapper;
import com.erp.service.finance.FinanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务管理服务实现类
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Service
public class FinanceServiceImpl implements FinanceService {

    @Autowired
    private FinanceMapper financeMapper;

    @Override
    public List<AvailableWarehousingVO> getAvailableWarehousingByContract(String contractCode) {
        if (contractCode == null || contractCode.trim().isEmpty()) {
            throw new IllegalArgumentException("合同号不能为空");
        }

        return financeMapper.selectAvailableWarehousingByContract(contractCode.trim());
    }

    @Override
    public List<WarehousingWasteDetailVO> getWarehousingWasteDetailsByCodes(List<String> warehousingCodes) {
        if (CollectionUtils.isEmpty(warehousingCodes)) {
            throw new IllegalArgumentException("入库单号列表不能为空");
        }

        return financeMapper.selectWarehousingWasteDetailsByCodes(warehousingCodes);
    }

    @Override
    public List<AvailableWarehousingVO> getWarehousingDetailsByCodes(List<String> warehousingCodes) {
        if (CollectionUtils.isEmpty(warehousingCodes)) {
            throw new IllegalArgumentException("入库单号列表不能为空");
        }

        log.info("根据入库单号列表获取入库单详情：warehousingCodes={}", warehousingCodes);
        return financeMapper.selectWarehousingDetailsByCodes(warehousingCodes);
    }

    @Override
    public List<TransportRecordDTO> getTransportRecordsByContract(String contractCode) {
        if (contractCode == null || contractCode.trim().isEmpty()) {
            throw new IllegalArgumentException("合同号不能为空");
        }

        log.info("根据合同号获取运输记录：contractCode={}", contractCode);
        return financeMapper.selectTransportRecordsByContract(contractCode.trim());
    }

    @Override
    public WarehousingWasteDetailWithContractResponse getWarehousingWasteDetailsWithContract(
            List<WarehousingWasteDetailWithContractRequest.WarehousingItemDTO> warehousingList,
            Integer contractId,
            String contractNo) {

        if (CollectionUtils.isEmpty(warehousingList)) {
            throw new IllegalArgumentException("入库单列表不能为空");
        }
        if (contractId == null || !StringUtils.hasText(contractNo)) {
            throw new IllegalArgumentException("合同编号和合同号不能为空");
        }

        log.info("获取入库危废明细，warehousingList={}, contractId={}, contractNo={}",
                warehousingList, contractId, contractNo);

        // 1. 查询入库危废明细（带合同匹配信息，双层锁定：入库单号+入库单编号必须同时匹配）
        // 注意：使用 LEFT JOIN 兜底匹配，即使危废条目编号为空也会返回（通过废物代码匹配）
        List<WarehousingWasteDetailWithContractVO> allDetails =
                financeMapper.selectWarehousingWasteDetailsWithContract(warehousingList, contractId, contractNo);

        log.info("查询到的入库危废明细数量={}", allDetails.size());

        // 2. 分离匹配成功和未匹配的数据
        List<WarehousingWasteDetailWithContractVO> matchedDetails = new java.util.ArrayList<>();
        List<WarehousingWasteDetailWithContractVO> unmatchedDetails = new java.util.ArrayList<>();

        for (WarehousingWasteDetailWithContractVO detail : allDetails) {
            // contractItemId 为空表示没有匹配到合同（双重兜底匹配都失败）
            if (detail.getContractItemId() == null) {
                // 设置未匹配状态
                detail.setMatched(false);
                detail.setMatchMessage("未能匹配到合同危废条目（危废条目编号和废物代码均无法匹配）");
                detail.setContractItemId(0); // 设置默认值避免前端解析问题
                unmatchedDetails.add(detail);
            } else {
                matchedDetails.add(detail);
            }
        }

        log.info("匹配成功的入库危废明细数量={}，未匹配数量={}", matchedDetails.size(), unmatchedDetails.size());

        // 3. 对匹配成功的记录，处理已结算量
        for (WarehousingWasteDetailWithContractVO detail : matchedDetails) {
            if ("总价包干".equals(detail.getQuotationMode())) {
                // 查询已结算量
                SettledQuantityDTO settledQty =
                        financeMapper.selectSettledQuantityByContractAndWasteCode(
                                contractId,
                                contractNo,
                                detail.getWasteCode()
                        );
                if (settledQty != null) {
                    detail.setSettledBasicQuantity(settledQty.getSettledBasicQuantity());
                    detail.setSettledAuxiliaryQuantity(settledQty.getSettledAuxiliaryQuantity());
                } else {
                    detail.setSettledBasicQuantity(BigDecimal.ZERO);
                    detail.setSettledAuxiliaryQuantity(BigDecimal.ZERO);
                }
            } else {
                // 按量结算模式，已结算量设为0
                detail.setSettledBasicQuantity(BigDecimal.ZERO);
                detail.setSettledAuxiliaryQuantity(BigDecimal.ZERO);
            }

            // 设置匹配状态
            detail.setMatched(true);
            detail.setMatchMessage("匹配成功");
        }

        // 4. 构建响应（只返回匹配成功的数据）
        WarehousingWasteDetailWithContractResponse response = new WarehousingWasteDetailWithContractResponse();
        response.setData(matchedDetails);
        response.setMatchedCount(matchedDetails.size());
        response.setUnmatchedCount(unmatchedDetails.size());

        return response;
    }
}

