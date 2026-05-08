package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 会计科目创建请求 DTO
 */
@Data
@ApiModel("会计科目创建请求")
public class FundSubjectCreateRequest {

    /**
     * 科目编码
     */
    @ApiModelProperty(value = "科目编码", required = true, example = "1001")
    @NotBlank(message = "科目编码不能为空")
    @Size(max = 50, message = "科目编码不能超过50个字符")
    @JsonProperty("subjectCode")
    private String subjectCode;

    /**
     * 科目名称
     */
    @ApiModelProperty(value = "科目名称", required = true, example = "库存现金")
    @NotBlank(message = "科目名称不能为空")
    @Size(max = 100, message = "科目名称不能超过100个字符")
    @JsonProperty("subjectName")
    private String subjectName;

    /**
     * 上级科目编码
     */
    @ApiModelProperty(value = "上级科目编码", example = "1001")
    @JsonProperty("parentSubjectCode")
    private String parentSubjectCode;

    /**
     * 科目类别：流动资产、非流动资产
     */
    @ApiModelProperty(value = "科目类别：流动资产、非流动资产", required = true, example = "流动资产")
    @NotBlank(message = "科目类别不能为空")
    @JsonProperty("subjectCategory")
    private String subjectCategory;

    /**
     * 余额方向：收入、支出
     */
    @ApiModelProperty(value = "余额方向：收入、支出", required = true, example = "收入")
    @NotBlank(message = "余额方向不能为空")
    @JsonProperty("balanceDirection")
    private String balanceDirection;

    /**
     * 是否为现金科目
     */
    @ApiModelProperty(value = "是否为现金科目", example = "true")
    @JsonProperty("isCashSubject")
    private Boolean isCashSubject = false;

    /**
     * 辅助核算
     */
    @ApiModelProperty(value = "辅助核算", example = "false")
    @JsonProperty("auxiliaryAccounting")
    private Boolean auxiliaryAccounting = false;

    /**
     * 数量核算
     */
    @ApiModelProperty(value = "数量核算", example = "false")
    @JsonProperty("quantityAccounting")
    private Boolean quantityAccounting = false;

    /**
     * 外币核算
     */
    @ApiModelProperty(value = "外币核算", example = "false")
    @JsonProperty("foreignCurrencyAccounting")
    private Boolean foreignCurrencyAccounting = false;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注", example = "库存现金科目")
    @Size(max = 500, message = "备注不能超过500个字符")
    @JsonProperty("remark")
    private String remark;
}