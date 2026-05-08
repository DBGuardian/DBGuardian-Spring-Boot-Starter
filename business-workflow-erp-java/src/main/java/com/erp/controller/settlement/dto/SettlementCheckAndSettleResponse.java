package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检查并结账响应DTO
 */
@Data
@ApiModel("检查并结账响应")
public class SettlementCheckAndSettleResponse {

    @ApiModelProperty(value = "组织ID", example = "1")
    private Long organizationId;

    @ApiModelProperty(value = "检查结果列表")
    private List<CheckResult> checkResults;


    /**
     * 检查结果
     */
    @Data
    @ApiModel("检查结果")
    public static class CheckResult {
        @ApiModelProperty(value = "账期ID", example = "3")
        private Long periodId;

        @ApiModelProperty(value = "账期编码", example = "202303")
        private String periodCode;

        @ApiModelProperty(value = "账户ID", example = "1")
        private Long accountId;

        @ApiModelProperty(value = "账户名称", example = "库存现金")
        private String accountName;

        @ApiModelProperty(value = "期初余额", example = "244604.00")
        private BigDecimal initialBalance;

        @ApiModelProperty(value = "期末余额", example = "210642.00")
        private BigDecimal finalBalance;

        @ApiModelProperty(value = "银行对账余额", example = "210642.00")
        private BigDecimal bankBalance;
    }
}

