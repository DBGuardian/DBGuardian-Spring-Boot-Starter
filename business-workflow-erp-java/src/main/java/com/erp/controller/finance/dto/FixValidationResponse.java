package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 修复校验异常响应DTO
 *
 * @author ERP System
 * @date 2025-02-05
 */
@Data
@ApiModel(description = "修复校验异常响应")
public class FixValidationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 修复ID
     */
    @ApiModelProperty(value = "修复ID", example = "FIX_1738723200000")
    private String fixId;

    /**
     * 合同ID
     */
    @ApiModelProperty(value = "合同ID", example = "1")
    private Long contractId;

    /**
     * 修复的问题类型
     */
    @ApiModelProperty(value = "修复的问题类型", example = "TIME_SEQUENCE_VIOLATION")
    private String issueType;

    /**
     * 修复状态
     */
    @ApiModelProperty(value = "修复状态", example = "SUCCESS", allowableValues = "SUCCESS,FAILED,PARTIAL")
    private String fixStatus;

    /**
     * 修复结果描述
     */
    @ApiModelProperty(value = "修复结果描述", example = "时间顺序问题已修复")
    private String message;

    /**
     * 修复时间
     */
    @ApiModelProperty(value = "修复时间", example = "2025-02-05 14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private String fixedAt;

    /**
     * 执行用户
     */
    @ApiModelProperty(value = "执行用户", example = "admin")
    private String fixedBy;

    /**
     * 校验是否仍然存在问题
     */
    @ApiModelProperty(value = "校验是否仍然存在问题", example = "false")
    private Boolean stillHasIssues;
}
