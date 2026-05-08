package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 收运通知单列表响应（包含统计信息）
 */
@Data
@ApiModel("收运通知单列表响应")
public class TransportApplyListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("统计数据")
    private List<TransportStat> stats;

    @ApiModelProperty("收运通知单列表")
    private List<TransportApplyPageResponse> records;

    @ApiModelProperty("总数")
    private Long total;

    @ApiModelProperty("当前页")
    private Long current;

    @ApiModelProperty("每页大小")
    private Long size;
}

