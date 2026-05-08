package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 检查并结账请求DTO
 */
@Data
@ApiModel("检查并结账请求")
public class SettlementCheckAndSettleRequest {

    @ApiModelProperty(value = "组织ID", required = true, example = "1")
    @NotNull(message = "组织ID不能为空")
    private Long organizationId;

    @ApiModelProperty(value = "银行对账余额列表")
    private List<BankBalance> bankBalances;

    /**
     * 银行对账余额
     */
    @Data
    @ApiModel("银行对账余额")
    public static class BankBalance {
        @ApiModelProperty(value = "账户ID", example = "1")
        private Long accountId;

        @ApiModelProperty(value = "银行对账余额", example = "210642.00")
        private java.math.BigDecimal bankBalance;
    }
}

