package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 资金组织创建请求
 */
@Data
@ApiModel("资金组织创建请求")
public class FundOrganizationCreateRequest {

    @ApiModelProperty(value = "组织名称", required = true, example = "主账户组织")
    @NotBlank(message = "组织名称不能为空")
    @Size(max = 100, message = "组织名称不能超过100个字符")
    private String organizationName;

    @ApiModelProperty(value = "组织描述", example = "包含所有主要银行账户")
    @Size(max = 500, message = "组织描述不能超过500个字符")
    private String description;
}
