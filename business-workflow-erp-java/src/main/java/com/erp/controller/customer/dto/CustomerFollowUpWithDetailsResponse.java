package com.erp.controller.customer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 带明细的跟进记录响应
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("带明细的跟进记录响应")
public class CustomerFollowUpWithDetailsResponse extends CustomerFollowUpResponse {

    @ApiModelProperty("跟进明细列表")
    private List<CustomerFollowUpDetailResponse> details;
}
