package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 入库单分页查询请求
 */
@Data
@ApiModel("入库单分页查询请求")
public class WarehousingPageRequest {

    @ApiModelProperty(value = "当前页码", example = "1")
    @Min(value = 1, message = "当前页码必须大于0")
    private Integer page = 1;

    @ApiModelProperty(value = "每页数量", example = "20")
    @Min(value = 1, message = "每页数量必须大于0")
    private Integer size = 20;

    @ApiModelProperty("关键字（入库单号/总磅单号/收运运输单号，模糊查询）")
    private String keyword;

    @ApiModelProperty("总磅单号（模糊查询）")
    private String weighingSlipNo;

    @ApiModelProperty("运输单号（模糊查询）")
    private String dispatchCode;

    @ApiModelProperty("状态：待结算/已结算/已锁定")
    private String status;

    @ApiModelProperty("开始时间（格式：YYYY-MM-DD HH:mm:ss）")
    private String startTime;

    @ApiModelProperty("结束时间（格式：YYYY-MM-DD HH:mm:ss）")
    private String endTime;

    @ApiModelProperty("排序字段")
    private String orderBy;

    @ApiModelProperty("排序方向：asc/desc")
    private String orderDirection;

    @ApiModelProperty("数据范围过滤：SELF-仅查看自己创建的数据，ALL-查看全部数据")
    private String viewScope;

    @ApiModelProperty("是否仅查询未关联合同的入库单（独立入库单）")
    private Boolean independentOnly;
}

