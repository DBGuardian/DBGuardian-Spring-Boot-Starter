package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作委外运输结算单结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("批量操作委外运输结算单结果")
public class OutsourceSettlementBatchOperationResult {

    @ApiModelProperty("成功数量")
    private int successCount;

    @ApiModelProperty("失败数量")
    private int failCount;

    @ApiModelProperty("失败详情")
    private List<FailureItem> failures = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel("失败项")
    public static class FailureItem {
        @ApiModelProperty("结算单编号")
        private Integer settlementId;

        @ApiModelProperty("结算单编号")
        private String settlementNo;

        @ApiModelProperty("失败原因")
        private String reason;
    }
}
