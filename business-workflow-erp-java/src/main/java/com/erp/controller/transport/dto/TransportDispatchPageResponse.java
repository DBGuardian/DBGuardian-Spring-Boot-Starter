package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 运输单分页响应
 */
@Data
@ApiModel("运输单分页响应")
public class TransportDispatchPageResponse {

    @ApiModelProperty("运输单编号")
    private Integer dispatchId;

    @ApiModelProperty("运输单号")
    private String dispatchCode;

    @ApiModelProperty("收运通知单号")
    private String noticeCode;

    @ApiModelProperty("合同号")
    private String contractCode;

    @ApiModelProperty("承运单位名称")
    private String carrierName;

    @ApiModelProperty("承运单位联系电话")
    private String carrierPhone;

    @ApiModelProperty("驾驶员姓名")
    private String driverName;

    @ApiModelProperty("驾驶员联系电话")
    private String driverPhone;

    @ApiModelProperty("车辆号牌")
    private String plateNo;

    @ApiModelProperty("调度员编码")
    private Integer dispatcherId;

    @ApiModelProperty("创建人编码（操作员ID，用于前端权限判断）")
    private Integer creatorId;

    @ApiModelProperty("营运证件号")
    private String operationLicenseNo;

    @ApiModelProperty("承运单位地址")
    private String carrierAddress;

    @ApiModelProperty("运输工具")
    private String transportTool;

    @ApiModelProperty("车辆编号")
    private Integer vehicleId;

    @ApiModelProperty("调度备注")
    private String dispatcherRemark;

    @ApiModelProperty("运输起点")
    private String startPoint;

    @ApiModelProperty("运输终点")
    private String endPoint;

    @ApiModelProperty("派车时间")
    private String dispatchAt;

    @ApiModelProperty("实际起运时间")
    private String departAt;

    @ApiModelProperty("实际到达时间")
    private String arriveAt;

    @ApiModelProperty("计划转移数量(吨)")
    private Double planQuantityTon;

    @ApiModelProperty("状态：待运输/运输中/已到达/已完成/已取消")
    private String status;

    @ApiModelProperty("是否锁定")
    private Boolean locked;

    @ApiModelProperty("锁定原因")
    private String lockReason;

    @ApiModelProperty("调度员姓名")
    private String dispatcherName;

    @ApiModelProperty("锁定时间")
    private String lockTime;

    @ApiModelProperty("锁定人编码")
    private Integer lockUserId;

    @ApiModelProperty("创建时间")
    private String createdAt;

    @ApiModelProperty("更新时间")
    private String updatedAt;

    @ApiModelProperty("总榜单编号，已关联则显示")
    private String weighingSlipCode;
}

