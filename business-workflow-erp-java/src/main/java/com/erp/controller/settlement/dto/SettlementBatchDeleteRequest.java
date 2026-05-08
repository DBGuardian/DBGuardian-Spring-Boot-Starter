package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量删除结算单请求DTO
 *
 * @author ERP System
 * @date 2025-01-31
 */
@Data
@ApiModel("批量删除结算单请求")
public class SettlementBatchDeleteRequest {

    /**
     * 结算单ID列表
     */
    @NotEmpty(message = "请选择要删除的结算单")
    @ApiModelProperty("结算单编号列表")
    private List<Long> settlementIds;
}
