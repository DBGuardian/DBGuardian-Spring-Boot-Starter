package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 会计科目批量更新请求 DTO
 */
@Data
@ApiModel("会计科目批量更新请求")
public class FundSubjectBatchUpdateRequest {

    /**
     * 科目ID列表
     */
    @ApiModelProperty(value = "科目ID列表", required = true)
    @NotEmpty(message = "科目ID列表不能为空")
    @JsonProperty("subjectIds")
    private List<Long> subjectIds;

    /**
     * 是否启用
     */
    @ApiModelProperty(value = "是否启用", required = true, example = "true")
    @NotNull(message = "状态值不能为空")
    @JsonProperty("enabled")
    private Boolean enabled;
}