package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 总磅单分页查询请求
 */
@Data
@ApiModel("总磅单分页查询请求")
public class WeighingSlipPageRequest {

    @ApiModelProperty(value = "当前页码", example = "1")
    @Min(value = 1, message = "当前页码必须大于0")
    private Long current = 1L;

    @ApiModelProperty(value = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量必须大于0")
    private Long size = 10L;

    @ApiModelProperty("关键字（总磅单号/序号/车号，模糊查询）")
    private String keyword;

    @ApiModelProperty("总磅单号（模糊查询）")
    private String weighingSlipNo;

    @ApiModelProperty("序号（模糊查询）")
    private String sequence;

    @ApiModelProperty("车号（模糊查询）")
    private String plateNo;

    @ApiModelProperty("状态：待细分/已细分")
    private String status;

    @ApiModelProperty("开始日期（格式：YYYY-MM-DD）")
    private String startDate;

    @ApiModelProperty("结束日期（格式：YYYY-MM-DD）")
    private String endDate;

    @ApiModelProperty("排序字段（默认：创建时间）")
    private String orderBy;

    @ApiModelProperty("排序方向：asc/desc（默认：desc）")
    private String orderDirection;

    @ApiModelProperty("数据范围过滤：SELF-仅查看自己创建的数据，ALL-查看全部数据")
    private String viewScope;
}

