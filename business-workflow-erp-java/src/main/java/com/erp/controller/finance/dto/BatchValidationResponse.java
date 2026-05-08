package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 批量业务闭环校验响应DTO
 *
 * @author ERP System
 * @date 2025-02-05
 */
@Data
@ApiModel(description = "批量业务闭环校验响应")
public class BatchValidationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 批量校验ID
     */
    @ApiModelProperty(value = "批量校验ID", example = "BATCH_VALIDATION_1738723200000")
    private String batchValidationId;

    /**
     * 请求的合同数量
     */
    @ApiModelProperty(value = "请求的合同数量", example = "3")
    private Integer requestedContracts;

    /**
     * 成功校验的合同数量
     */
    @ApiModelProperty(value = "成功校验的合同数量", example = "3")
    private Integer validatedContracts;

    /**
     * 总共发现的问题数量
     */
    @ApiModelProperty(value = "总共发现的问题数量", example = "5")
    private Integer totalIssuesFound;

    /**
     * 校验执行时间（毫秒）
     */
    @ApiModelProperty(value = "校验执行时间（毫秒）", example = "1500")
    private Long executionTime;

    /**
     * 各合同的校验结果
     */
    @ApiModelProperty(value = "各合同的校验结果")
    private Map<Long, ContractValidationResult> contractResults;

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

    /**
     * 合同校验结果
     */
    @Data
    @ApiModel(description = "单个合同校验结果")
    public static class ContractValidationResult implements Serializable {
        private String contractCode;
        private String contractName;
        private Integer issuesFound;
        private List<ClosureIssueDTO> issues;
        private String status; // SUCCESS, FAILED, ERROR
        private String message;
    }
}
