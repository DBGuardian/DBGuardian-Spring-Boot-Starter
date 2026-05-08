package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 会计科目选项 DTO（用于下拉选择）
 */
@Data
@ApiModel("会计科目选项")
public class FundSubjectOption {

    /**
     * 科目编号
     */
    @ApiModelProperty(value = "科目编号", example = "1")
    @JsonProperty("subjectId")
    private Long subjectId;

    /**
     * 科目编码
     */
    @ApiModelProperty(value = "科目编码", example = "1001")
    @JsonProperty("subjectCode")
    private String subjectCode;

    /**
     * 科目名称
     */
    @ApiModelProperty(value = "科目名称", example = "库存现金")
    @JsonProperty("subjectName")
    private String subjectName;

    /**
     * 上级科目编码
     */
    @ApiModelProperty(value = "上级科目编码", example = "1001")
    @JsonProperty("parentSubjectCode")
    private String parentSubjectCode;

    /**
     * 科目级次
     */
    @ApiModelProperty(value = "科目级次", example = "1")
    @JsonProperty("subjectLevel")
    private Integer subjectLevel;

    /**
     * 全科目名称
     */
    @ApiModelProperty(value = "全科目名称", example = "库存现金")
    @JsonProperty("fullSubjectName")
    private String fullSubjectName;

    /**
     * 科目类别
     */
    @ApiModelProperty(value = "科目类别", example = "流动资产")
    @JsonProperty("subjectCategory")
    private String subjectCategory;

    /**
     * 显示文本（编码 + 名称）
     */
    @ApiModelProperty(value = "显示文本", example = "1001 - 库存现金")
    @JsonProperty("label")
    private String label;
}