package com.erp.controller.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户跟进记录响应
 */
@Data
@ApiModel("客户跟进记录响应")
public class CustomerFollowUpResponse {

    @ApiModelProperty("跟进记录编号")
    private Integer followUpId;

    @ApiModelProperty("客户编码")
    private Integer customerId;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty("业务员编码")
    private Integer employeeId;

    @ApiModelProperty("业务员姓名")
    private String employeeName;

    @ApiModelProperty("联系人姓名")
    private String contactName;

    @ApiModelProperty("联系人电话")
    private String contactPhone;

    @ApiModelProperty("创建人编码")
    private Integer creatorId;

    @ApiModelProperty("创建人姓名")
    private String creatorName;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
