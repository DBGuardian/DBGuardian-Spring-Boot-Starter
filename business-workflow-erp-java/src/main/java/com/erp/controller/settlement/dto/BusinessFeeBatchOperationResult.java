package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("批量操作结果")
public class BusinessFeeBatchOperationResult {

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
        @ApiModelProperty("业务序号")
        private Integer businessSeq;

        @ApiModelProperty("业务编号")
        private String businessCode;

        @ApiModelProperty("失败原因")
        private String reason;
    }
}
