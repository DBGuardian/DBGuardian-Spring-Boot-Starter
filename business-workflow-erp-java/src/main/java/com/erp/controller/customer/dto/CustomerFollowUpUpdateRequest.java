package com.erp.controller.customer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 客户跟进更新请求
 */
@Data
@ApiModel("客户跟进更新请求")
public class CustomerFollowUpUpdateRequest {

    @ApiModelProperty(value = "跟进记录编号", required = true)
    @NotNull(message = "跟进记录编号不能为空")
    private Integer followUpId;

    @ApiModelProperty(value = "客户编码", required = true)
    @NotNull(message = "客户编码不能为空")
    private Integer customerId;

    @ApiModelProperty("联系人姓名")
    private String contactName;

    @ApiModelProperty("联系人电话")
    private String contactPhone;

    @ApiModelProperty("备注")
    private String remark;
}
