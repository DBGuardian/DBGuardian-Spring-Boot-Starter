package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 转移联单分页查询请求
 */
@Data
@ApiModel("转移联单分页查询请求")
public class TransferManifestPageRequest {

    @ApiModelProperty(value = "当前页码", example = "1")
    @Min(value = 1, message = "当前页码必须大于0")
    private Integer page = 1;

    @ApiModelProperty(value = "每页数量", example = "20")
    @Min(value = 1, message = "每页数量必须大于0")
    private Integer size = 20;

    @ApiModelProperty("广东省联单号（模糊查询）")
    private String 广东省联单号;

    @ApiModelProperty("国家联单号（模糊查询）")
    private String 国家联单号;

    @ApiModelProperty("产生单位（模糊查询）")
    private String 产生单位;

    @ApiModelProperty("接收单位（模糊查询）")
    private String 接收单位;

    @ApiModelProperty("车牌号（模糊查询）")
    private String 车牌号;

    @ApiModelProperty("当前阶段（精确匹配：待发运/运输中/已接收/已完结）")
    private String 当前阶段;

    @ApiModelProperty("计划转移开始日期（格式：YYYY-MM-DD）")
    private String 计划转移开始;

    @ApiModelProperty("计划转移结束日期（格式：YYYY-MM-DD）")
    private String 计划转移结束;

    @ApiModelProperty("排序字段")
    private String sortField;

    @ApiModelProperty("排序方向：asc/desc")
    private String sortOrder;
}
