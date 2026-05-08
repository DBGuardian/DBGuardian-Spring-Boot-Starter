package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("业务员详情响应")
public class SalespersonDetailResponse extends SalespersonPageResponse {

    @ApiModelProperty("关联客户编码")
    private Integer customerId;

    @ApiModelProperty("更新时间")
    private String updateTime;
}
