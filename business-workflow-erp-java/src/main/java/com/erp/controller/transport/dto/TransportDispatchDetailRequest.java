package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运输单创建/更新请求
 */
@Data
@ApiModel("运输单创建/更新请求")
public class TransportDispatchDetailRequest {

    @ApiModelProperty("运输单号，更新时必填")
    private String dispatchCode;

    @ApiModelProperty("收运通知单号，创建必填")
    private String noticeCode;

    @ApiModelProperty("合同号，可为空")
    private String contractCode;

    @ApiModelProperty("合同待补标记")
    private Boolean contractPending;

    @ApiModelProperty("承运单位")
    private String carrierName;

    @ApiModelProperty("营运证件号")
    private String operationLicenseNo;

    @ApiModelProperty("承运单位地址")
    private String carrierAddress;

    @ApiModelProperty("承运单位电话")
    private String carrierPhone;

    @ApiModelProperty("驾驶员姓名")
    private String driverName;

    @ApiModelProperty("驾驶员电话")
    private String driverPhone;

    @ApiModelProperty("押运员")
    private String escortName;

    @ApiModelProperty("押运员电话")
    private String escortPhone;

    @ApiModelProperty("运输工具")
    private String transportTool;

    @ApiModelProperty("车辆编号")
    private Integer vehicleId;

    @ApiModelProperty("车辆号牌")
    private String plateNo;

    @ApiModelProperty("运输起点")
    private String startPoint;

    @ApiModelProperty("运输终点")
    private String endPoint;

    @ApiModelProperty("派车时间 yyyy-MM-dd HH:mm:ss")
    private String dispatchAt;

    @ApiModelProperty("实际起运时间 yyyy-MM-dd HH:mm:ss")
    private String departAt;

    @ApiModelProperty("实际到达时间 yyyy-MM-dd HH:mm:ss")
    private String arriveAt;

    @ApiModelProperty("计划转移数量(吨)")
    private Double planQuantityTon;

    @ApiModelProperty("运输距离（公里）")
    private BigDecimal transportDistance;

    @ApiModelProperty("调度备注")
    private String dispatcherRemark;

    @ApiModelProperty("状态：待运输/运输中/已到达/已完成/已取消")
    private String status;
}


