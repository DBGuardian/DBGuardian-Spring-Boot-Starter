package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 账户组合创建请求 DTO
 *
 * 对应文档《资金管理实现说明》中的"创建账户组合"接口入参
 */
@Data
@ApiModel("账户组合创建请求")
public class FundAccountGroupCreateRequest {

    /**
     * 组合名称
     */
    @ApiModelProperty(value = "组合名称", required = true, example = "主账户组合")
    @NotBlank(message = "组合名称不能为空")
    @JsonProperty("groupName")
    private String groupName;

    /**
     * 账户ID列表
     */
    @ApiModelProperty(value = "账户ID列表", required = true, example = "[1, 2, 3]")
    @NotEmpty(message = "账户ID列表不能为空")
    @JsonProperty("accountIds")
    private List<Long> accountIds;

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

