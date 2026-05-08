package com.erp.controller.contract.dto;

import com.erp.entity.contract.ContractItem;
import com.erp.entity.contract.ContractWasteItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 合同条目及其危废条目明细的联合查询结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractItemWithWasteItems extends ContractItem {
    /**
     * 关联的危废条目明细列表
     */
    private List<ContractWasteItem> wasteItems;
}
