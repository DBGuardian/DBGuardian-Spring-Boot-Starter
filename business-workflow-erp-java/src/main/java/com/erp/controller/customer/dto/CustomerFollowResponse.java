package com.erp.controller.customer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户跟进记录响应
 */
@Data
@ApiModel("客户跟进记录响应")
public class CustomerFollowResponse {

    @ApiModelProperty("跟进记录ID")
    private Integer followId;

    @ApiModelProperty("跟进时间")
    private LocalDateTime followTime;

    @ApiModelProperty("跟进方式")
    private String followMethod;

    @ApiModelProperty("跟进内容")
    private String followContent;

    @ApiModelProperty("下一步计划")
    private String nextPlan;

    @ApiModelProperty("跟进人编码")
    private Integer followerId;

    @ApiModelProperty("跟进人姓名")
    private String followerName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}


