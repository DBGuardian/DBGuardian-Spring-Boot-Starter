package com.erp.controller.settlement.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量查询危废结算单关联入库单聚合数据 - 请求DTO
 * <p>
 * 传入多个危废结算单编号，后端查询每个结算单关联的入库单危废明细，
 * 按废物类别+废物代码+废物名称合并有价类/无价类重量，返回聚合后的明细行列表。
 */
@Data
public class SettlementWasteAggregateRequest {

    /**
     * 危废结算单编号列表（支持批量，不能为空）
     */
    @NotEmpty(message = "结算单编号列表不能为空")
    private List<Long> settlementIds;
}
