package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 总磅单列表响应
 */
@Data
@ApiModel("总磅单列表响应")
public class WeighingSlipListResponse {

    @ApiModelProperty("统计信息")
    private List<WeighingSlipStat> stats;

    @ApiModelProperty("总磅单记录列表")
    private List<WeighingSlipPageResponse> records;

    @ApiModelProperty("总数")
    private Long total;

    @ApiModelProperty("当前页码")
    private Long current;

    @ApiModelProperty("每页数量")
    private Long size;
}

