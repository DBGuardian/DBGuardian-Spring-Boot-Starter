package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同执行进度响应
 */
@Data
@ApiModel("合同执行进度响应")
public class ContractProgressResponse {

    @ApiModelProperty("合同编号")
    private Integer contractId;

    @ApiModelProperty("合同号")
    private String contractNo;

    @ApiModelProperty("整体进度百分比（0-100）")
    private Integer progressPercentage;

    @ApiModelProperty("进度步骤列表")
    private List<ProgressStep> steps;

    /**
     * 进度步骤
     */
    @Data
    @ApiModel("进度步骤")
    public static class ProgressStep {
        @ApiModelProperty("步骤名称")
        private String stepName;

        @ApiModelProperty("步骤描述")
        private String stepDescription;

        @ApiModelProperty("是否完成")
        private Boolean completed;

        @ApiModelProperty("完成时间")
        private LocalDateTime completedTime;

        @ApiModelProperty("操作人姓名")
        private String operatorName;

        @ApiModelProperty("备注")
        private String remark;
    }
}
















