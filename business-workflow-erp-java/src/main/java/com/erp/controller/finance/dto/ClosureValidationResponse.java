package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 业务闭环校验响应DTO
 *
 * @author ERP System
 * @date 2025-02-05
 */
@Data
@ApiModel(description = "业务闭环校验响应")
public class ClosureValidationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 校验ID
     */
    @ApiModelProperty(value = "校验ID", example = "VALIDATION_1738723200000")
    private String validationId;

    /**
     * 校验的总项目数
     */
    @ApiModelProperty(value = "校验的总项目数", example = "150")
    private Integer totalChecked;

    /**
     * 发现的问题数量
     */
    @ApiModelProperty(value = "发现的问题数量", example = "5")
    private Integer issuesFound;

    /**
     * 自动解决的问题数量
     */
    @ApiModelProperty(value = "自动解决的问题数量", example = "2")
    private Integer resolvedIssues;

    /**
     * 校验执行时间（毫秒）
     */
    @ApiModelProperty(value = "校验执行时间（毫秒）", example = "1250")
    private Long executionTime;

    /**
     * 发现的问题列表
     */
    @ApiModelProperty(value = "发现的问题列表")
    private List<ClosureIssueDTO> issues;

    /**
     * 校验项目详情
     */
    @ApiModelProperty(value = "校验项目详情")
    private List<ValidationCheckItemDTO> checkItems;

    /**
     * 整体校验状态
     */
    @ApiModelProperty(value = "整体校验状态", example = "PASS")
    private String overallStatus;

    /**
     * 通过的校验数
     */
    @ApiModelProperty(value = "通过的校验数", example = "145")
    private Integer passedChecks;

    /**
     * 失败的校验数
     */
    @ApiModelProperty(value = "失败的校验数", example = "5")
    private Integer failedChecks;

    /**
     * 警告的校验数
     */
    @ApiModelProperty(value = "警告的校验数", example = "0")
    private Integer warningChecks;

    /**
     * 执行时间
     */
    @ApiModelProperty(value = "执行时间", example = "2025-02-05 14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private String executedAt;

    /**
     * 执行用户
     */
    @ApiModelProperty(value = "执行用户", example = "admin")
    private String executedBy;
}
