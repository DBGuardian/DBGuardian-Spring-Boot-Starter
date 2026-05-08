package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 更新组织账户关联关系请求
 */
@Data
@ApiModel("更新组织账户关联关系请求")
public class UpdateOrganizationAccountsRequest {

    @ApiModelProperty(value = "账户ID列表", required = true, example = "[1, 2, 3]")
    @NotNull(message = "账户ID列表不能为空")
    private List<Long> accountIds;
}