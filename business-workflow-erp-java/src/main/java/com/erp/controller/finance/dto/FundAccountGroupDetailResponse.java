package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 账户组合详情响应 DTO
 */
@Data
@ApiModel("账户组合详情响应")
public class FundAccountGroupDetailResponse {

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

    /**
     * 组合名称
     */
    @ApiModelProperty(value = "组合名称", example = "主账户组合")
    @JsonProperty("groupName")
    private String groupName;

    /**
     * 账户ID列表
     */
    @ApiModelProperty(value = "账户ID列表", example = "[1, 2, 3]")
    @JsonProperty("accountIds")
    private List<Long> accountIds;

    /**
     * 账户名称列表
     */
    @ApiModelProperty(value = "账户名称列表", example = "[\"工商银行\", \"建设银行\", \"农业银行\"]")
    @JsonProperty("accountNames")
    private List<String> accountNames;

    /**
     * 是否启用
     */
    @ApiModelProperty(value = "是否启用", example = "true")
    @JsonProperty("enabled")
    private Boolean enabled;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注", example = "用于汇总表查看")
    @JsonProperty("remark")
    private String remark;
}





































































