package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 会计科目分页查询请求 DTO
 */
@Data
@ApiModel("会计科目分页查询请求")
public class FundSubjectPageRequest {

    /**
     * 当前页码
     */
    @ApiModelProperty(value = "当前页码", example = "1")
    @JsonProperty("current")
    private Integer current = 1;

    /**
     * 每页大小
     */
    @ApiModelProperty(value = "每页大小", example = "10")
    @JsonProperty("size")
    private Integer size = 10;

    /**
     * 科目编码（模糊查询）
     */
    @ApiModelProperty(value = "科目编码（模糊查询）", example = "1001")
    @JsonProperty("subjectCode")
    private String subjectCode;

    /**
     * 科目名称（模糊查询）
     */
    @ApiModelProperty(value = "科目名称（模糊查询）", example = "库存现金")
    @JsonProperty("subjectName")
    private String subjectName;

    /**
     * 科目类别
     */
    @ApiModelProperty(value = "科目类别", example = "流动资产")
    @JsonProperty("subjectCategory")
    private String subjectCategory;

    /**
     * 是否启用
     */
    @ApiModelProperty(value = "是否启用", example = "true")
    @JsonProperty("enabled")
    private Boolean enabled;

    /**
     * 排序字段
     */
    @ApiModelProperty(value = "排序字段", example = "subjectCode")
    @JsonProperty("sortField")
    private String sortField;

    /**
     * 排序顺序：asc/desc
     */
    @ApiModelProperty(value = "排序顺序：asc/desc", example = "asc")
    @JsonProperty("sortOrder")
    private String sortOrder;
}