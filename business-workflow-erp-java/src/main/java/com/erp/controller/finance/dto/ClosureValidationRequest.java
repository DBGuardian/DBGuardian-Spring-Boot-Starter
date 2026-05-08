package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 业务闭环校验请求DTO
 *
 * @author ERP System
 * @date 2025-02-05
 */
@Data
@ApiModel(description = "业务闭环校验请求")
public class ClosureValidationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 合同ID（全量校验时可为空）
     */
    @ApiModelProperty(value = "合同ID", example = "1")
    private Long contractId;

    /**
     * 校验类型：FULL-全量校验，PARTIAL-部分校验
     */
    @ApiModelProperty(value = "校验类型", example = "FULL", allowableValues = "FULL,PARTIAL")
    private String validateType;

    /**
     * 指定要执行的校验项目，不传则执行所有校验
     */
    @ApiModelProperty(value = "校验项目列表", example = "[\"TIME_SEQUENCE\", \"AMOUNT_CONSISTENCY\"]")
    private List<String> checkItems;
}
