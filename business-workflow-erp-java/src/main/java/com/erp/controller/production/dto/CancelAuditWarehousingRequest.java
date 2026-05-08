package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 取消审核入库单请求参数
 */
@Data
@ApiModel(description = "取消审核入库单请求参数")
public class CancelAuditWarehousingRequest {

    /**
     * 入库单号
     */
    @NotBlank(message = "入库单号不能为空")
    @ApiModelProperty(value = "入库单号", required = true, example = "RKD-20240101-0001")
    private String warehousingNo;

    /**
     * 取消审核原因（可选）
     */
    @ApiModelProperty(value = "取消审核原因", example = "需要重新审核")
    private String reason;
}

