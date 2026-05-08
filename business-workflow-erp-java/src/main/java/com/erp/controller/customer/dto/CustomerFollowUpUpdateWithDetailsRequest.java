package com.erp.controller.customer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 客户跟进记录更新请求（包含明细差分修改）
 */
@Data
@ApiModel("客户跟进记录更新请求（包含明细）")
public class CustomerFollowUpUpdateWithDetailsRequest {

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

    @ApiModelProperty("新增的明细列表")
    @Valid
    private List<CustomerFollowUpDetailCreateRequest> addedDetails;

    @ApiModelProperty("更新的明细列表")
    @Valid
    private List<CustomerFollowUpDetailUpdateRequest> updatedDetails;

    @ApiModelProperty("删除的明细ID列表")
    private List<Integer> deletedDetailIds;
}
