package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 业务闭环问题DTO
 *
 * @author ERP System
 * @date 2025-02-05
 */
@Data
@ApiModel(description = "业务闭环问题信息")
public class ClosureIssueDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 问题唯一标识
     */
    @ApiModelProperty(value = "问题唯一标识", example = "ISSUE_001")
    private String issueId;

    /**
     * 问题类型
     */
    @ApiModelProperty(value = "问题类型", example = "TIME_SEQUENCE_VIOLATION")
    private String issueType;

    /**
     * 风险等级
     */
    @ApiModelProperty(value = "风险等级", example = "HIGH")
    private String riskLevel;

    /**
     * 严重程度
     */
    @ApiModelProperty(value = "严重程度", example = "HIGH")
    private String severity;

    /**
     * 关联业务类型
     */
    @ApiModelProperty(value = "关联业务类型", example = "CONTRACT")
    private String relatedEntityType;

    /**
     * 关联业务ID
     */
    @ApiModelProperty(value = "关联业务ID", example = "1001")
    private Long relatedEntityId;

    /**
     * 关联业务编码
     */
    @ApiModelProperty(value = "关联业务编码", example = "HT20250101001")
    private String relatedEntityCode;

    /**
     * 关联业务名称
     */
    @ApiModelProperty(value = "关联业务名称", example = "危险废物处置合同")
    private String relatedEntityName;

    /**
     * 问题标题
     */
    @ApiModelProperty(value = "问题标题", example = "合同签订时间晚于执行时间")
    private String issueTitle;

    /**
     * 问题描述
     */
    @ApiModelProperty(value = "问题描述", example = "合同HT20250101001的签订日期为2025-01-15，但实际执行开始时间为2025-01-10")
    private String issueDescription;

    /**
     * 问题详情数据
     */
    @ApiModelProperty(value = "问题详情数据", example = "{\"contractSigningTime\":\"2025-01-15 09:00:00\",\"executionStartTime\":\"2025-01-10 14:30:00\"}")
    private Map<String, Object> issueDetails;

    /**
     * 建议操作
     */
    @ApiModelProperty(value = "建议操作", example = "请确认合同签订时间是否正确，或调整执行记录的时间")
    private String suggestedAction;

    /**
     * 操作类型
     */
    @ApiModelProperty(value = "操作类型", example = "REVIEW_REQUIRED")
    private String actionType;

    /**
     * 发现时间
     */
    @ApiModelProperty(value = "发现时间", example = "2025-01-20 10:15:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private String detectedAt;

    /**
     * 最后校验时间
     */
    @ApiModelProperty(value = "最后校验时间", example = "2025-01-20 10:15:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private String lastValidatedAt;
}
