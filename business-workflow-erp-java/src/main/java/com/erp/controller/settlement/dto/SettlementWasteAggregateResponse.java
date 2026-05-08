package com.erp.controller.settlement.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量查询危废结算单关联入库单聚合数据 - 响应DTO
 */
@Data
public class SettlementWasteAggregateResponse {

    /**
     * 按废物类别+代码+名称聚合后的明细行列表
     */
    private List<SettlementWasteAggregateItemDTO> items;

    /**
     * 涉及的危废结算单编号列表（原样返回，便于前端对账）
     */
    private List<Long> settlementIds;
}
