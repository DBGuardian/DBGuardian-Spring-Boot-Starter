package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 入库危废明细（含合同匹配信息）响应DTO
 */
@Data
@ApiModel("入库危废明细（含合同匹配信息）响应")
public class WarehousingWasteDetailWithContractResponse {

    @ApiModelProperty("匹配后的完整数据列表")
    private List<WarehousingWasteDetailWithContractVO> data;

    @ApiModelProperty("匹配成功数量")
    private Integer matchedCount;

    @ApiModelProperty("未匹配数量")
    private Integer unmatchedCount;
}
