package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 批量提交审核响应
 */
@Data
@ApiModel("批量提交审核响应")
public class ContractBatchSubmitAuditResponse {

    @ApiModelProperty("成功提交的合同ID列表")
    private List<Integer> successIds;

    @ApiModelProperty("提交失败的合同ID列表")
    private List<Integer> failedIds;

    @ApiModelProperty("失败原因列表")
    private List<FailedReason> failedReasons;

    @ApiModelProperty("是否全部成功")
    private boolean allSuccess;

    @Data
    @ApiModel("失败原因")
    public static class FailedReason {
        @ApiModelProperty("合同ID")
        private Integer contractId;

        @ApiModelProperty("失败原因")
        private String reason;
    }
}
