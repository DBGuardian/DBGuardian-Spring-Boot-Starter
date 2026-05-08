package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 入库单列表响应
 */
@Data
@ApiModel("入库单列表响应")
public class WarehousingListResponse {

    @ApiModelProperty("统计信息")
    private List<WarehousingStat> stats;

    @ApiModelProperty("入库单记录列表")
    private List<WarehousingPageResponse> records;

    @ApiModelProperty("总数")
    private Long total;

    @ApiModelProperty("当前页码")
    private Integer current;

    @ApiModelProperty("每页数量")
    private Integer size;
}

