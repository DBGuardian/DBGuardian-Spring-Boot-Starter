package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 资金组织更新请求
 */
@Data
@ApiModel("资金组织更新请求")
public class FundOrganizationUpdateRequest {

    @ApiModelProperty(value = "组织名称", example = "主账户组织")
    @Size(max = 100, message = "组织名称不能超过100个字符")
    private String organizationName;

    @ApiModelProperty(value = "组织描述", example = "包含所有主要银行账户")
    @Size(max = 500, message = "组织描述不能超过500个字符")
    private String description;

    @ApiModelProperty(value = "是否启用", example = "true")
    private Boolean enabled;
}
