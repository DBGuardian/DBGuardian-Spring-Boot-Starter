package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 委外处理合同操作响应（用于删除等操作）
 */
@Data
@ApiModel(description = "委外处理合同操作响应")
public class OutsourceProcessingContractOperationResponse {

    @ApiModelProperty(value = "总数量", example = "10")
    private Integer totalCount;

    @ApiModelProperty(value = "成功数量", example = "9")
    private Integer successCount;

    @ApiModelProperty(value = "失败数量", example = "1")
    private Integer failCount;

    @ApiModelProperty(value = "是否全部成功", example = "false")
    private Boolean allSuccess;

    @ApiModelProperty(value = "操作耗时（毫秒）", example = "150")
    private Long duration;
}
