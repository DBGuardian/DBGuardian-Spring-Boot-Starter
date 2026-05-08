package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 运输单分页查询请求
 */
@Data
@ApiModel("运输单分页查询请求")
public class TransportDispatchPageRequest {

    @ApiModelProperty(value = "当前页码，从1开始", example = "1")
    @Min(value = 1, message = "当前页码必须大于等于1")
    private long current = 1;

    @ApiModelProperty(value = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量必须大于等于1")
    private long size = 10;

    @ApiModelProperty("运输单号（模糊查询）")
    private String dispatchCode;

    @ApiModelProperty("收运通知单号（模糊查询）")
    private String noticeCode;

    @ApiModelProperty("合同号（模糊查询）")
    private String contractCode;

    @ApiModelProperty("承运单位名称（模糊查询）")
    private String carrierName;

    @ApiModelProperty("驾驶员姓名（模糊查询）")
    private String driverName;

    @ApiModelProperty("车辆号牌（模糊查询）")
    private String plateNo;

    @ApiModelProperty("状态：待运输/运输中/已到达/已完成/已取消")
    private String status;

    @ApiModelProperty("是否锁定")
    private Boolean locked;

    @ApiModelProperty("调度员编码")
    private Integer dispatcherId;

    @ApiModelProperty("创建时间开始（格式：YYYY-MM-DD HH:mm:ss）")
    private String startTime;

    @ApiModelProperty("创建时间结束（格式：YYYY-MM-DD HH:mm:ss）")
    private String endTime;

    @ApiModelProperty("排序字段（默认：创建时间）")
    private String sortField;

    @ApiModelProperty("排序方向：asc/desc（默认：desc）")
    private String sortOrder;

    @ApiModelProperty("创建人过滤（员工编码），viewScope=SELF时前端传入当前用户ID，后端以此过滤数据范围；后端会与权限配置做二次校验")
    private Integer creatorFilter;

    @ApiModelProperty("数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断")
    private String viewScope;
}

