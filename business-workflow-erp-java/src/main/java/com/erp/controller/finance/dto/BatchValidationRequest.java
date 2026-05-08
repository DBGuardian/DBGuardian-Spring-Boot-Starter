package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量业务闭环校验请求DTO
 *
 * @author ERP System
 * @date 2025-02-05
 */
@Data
@ApiModel(description = "批量业务闭环校验请求")
public class BatchValidationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 合同ID列表
     */
    @ApiModelProperty(value = "合同ID列表", required = true, example = "[1, 2, 3]")
    private List<Long> contractIds;

    /**
     * 指定要执行的校验项目，不传则执行所有校验
     */
    @ApiModelProperty(value = "校验项目列表", example = "[\"TIME_SEQUENCE\", \"AMOUNT_CONSISTENCY\"]")
    private List<String> checkTypes;
}
