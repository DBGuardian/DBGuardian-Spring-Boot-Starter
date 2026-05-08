package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 会计科目更新请求 DTO
 */
@Data
@ApiModel("会计科目更新请求")
public class FundSubjectUpdateRequest extends FundSubjectCreateRequest {

    /**
     * 科目编号
     */
    @ApiModelProperty(value = "科目编号", required = true, example = "1")
    @NotNull(message = "科目编号不能为空")
    @JsonProperty("subjectId")
    private Long subjectId;

    /**
     * 是否启用
     */
    @ApiModelProperty(value = "是否启用", example = "true")
    @JsonProperty("enabled")
    private Boolean enabled;

    /**
     * 版本号（乐观锁）
     */
    @ApiModelProperty(value = "版本号（乐观锁）", required = true, example = "0")
    @NotNull(message = "版本号不能为空")
    @JsonProperty("version")
    private Integer version;
}