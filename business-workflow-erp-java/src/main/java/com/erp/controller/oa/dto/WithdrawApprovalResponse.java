package com.erp.controller.oa.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 撤回审核响应
 */
@Data
@ApiModel(description = "撤回审核响应")
public class WithdrawApprovalResponse {

    @ApiModelProperty(value = "是否成功")
    private boolean success;
}
