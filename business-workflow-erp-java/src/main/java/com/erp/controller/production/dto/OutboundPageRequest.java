package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 出库单分页查询请求
 */
@Data
@ApiModel("出库单分页查询请求")
public class OutboundPageRequest {

    @ApiModelProperty("页码，默认1")
    private Integer page = 1;

    @ApiModelProperty("每页数量，默认20")
    private Integer size = 20;

    @ApiModelProperty("关键词（出库单号/合同号/客户名称/废物名称）")
    private String keyword;

    @ApiModelProperty("出库类型")
    private String outboundType;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("去向类型")
    private String destinationType;

    @ApiModelProperty("开始时间")
    private String startTime;

    @ApiModelProperty("结束时间")
    private String endTime;

    @ApiModelProperty("排序字段")
    private String orderBy;

    @ApiModelProperty("排序方向：asc/desc")
    private String orderDirection;
}
