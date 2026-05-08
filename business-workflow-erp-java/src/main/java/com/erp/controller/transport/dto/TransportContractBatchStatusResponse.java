package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 运输合同批量状态更新响应DTO
 */
@Data
@ApiModel(description = "运输合同批量状态更新响应")
public class TransportContractBatchStatusResponse {

    @ApiModelProperty(value = "成功的合同ID列表")
    private List<Integer> successIds;

    @ApiModelProperty(value = "失败的合同ID列表")
    private List<Integer> failedIds;

    @ApiModelProperty(value = "失败原因Map：合同ID -> 失败原因")
    private java.util.Map<Integer, String> failedReasons;

    @ApiModelProperty(value = "是否全部成功")
    private boolean allSuccess;

    @ApiModelProperty(value = "成功数量")
    private int successCount;

    @ApiModelProperty(value = "失败数量")
    private int failedCount;

    public List<Integer> getSuccessIds() {
        return successIds;
    }

    public void setSuccessIds(List<Integer> successIds) {
        this.successIds = successIds;
        this.successCount = successIds != null ? successIds.size() : 0;
    }

    public List<Integer> getFailedIds() {
        return failedIds;
    }

    public void setFailedIds(List<Integer> failedIds) {
        this.failedIds = failedIds;
        this.failedCount = failedIds != null ? failedIds.size() : 0;
    }

    public boolean isAllSuccess() {
        return allSuccess;
    }

    public void setAllSuccess(boolean allSuccess) {
        this.allSuccess = allSuccess;
    }
}
