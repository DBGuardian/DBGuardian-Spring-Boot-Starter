package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 账户组合创建响应 DTO
 */
@Data
@ApiModel("账户组合创建响应")
public class FundAccountGroupCreateResponse {

    /**
     * 组合ID
     */
    @ApiModelProperty(value = "组合ID", example = "1")
    @JsonProperty("groupId")
    private Long groupId;

    /**
     * 组合编码
     */
    @ApiModelProperty(value = "组合编码", example = "GRP-20260101-0001")
    @JsonProperty("groupCode")
    private String groupCode;
}





































































