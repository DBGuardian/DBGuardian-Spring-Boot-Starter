package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 修复校验异常请求DTO
 *
 * @author ERP System
 * @date 2025-02-05
 */
@Data
@ApiModel(description = "修复校验异常请求")
public class FixValidationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 合同ID
     */
    @ApiModelProperty(value = "合同ID", required = true, example = "1")
    private Long contractId;

    /**
     * 问题类型
     */
    @ApiModelProperty(value = "问题类型", required = true, example = "TIME_SEQUENCE_VIOLATION")
    private String issueType;

    /**
     * 修复数据
     */
    @ApiModelProperty(value = "修复数据", example = "{\"newDate\":\"2025-01-15 10:00:00\"}")
    private Map<String, Object> fixData;
}
