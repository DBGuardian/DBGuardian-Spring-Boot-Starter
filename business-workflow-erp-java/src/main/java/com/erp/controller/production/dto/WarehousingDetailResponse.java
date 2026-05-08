package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 入库单详情响应
 */
@Data
@ApiModel("入库单详情响应")
public class WarehousingDetailResponse {

    @ApiModelProperty("入库单编号")
    private Integer warehousingId;

    @ApiModelProperty("入库单号")
    private String warehousingNo;

    @ApiModelProperty("总磅单号")
    private String weighingSlipNo;

    @ApiModelProperty("运输单号")
    private String dispatchCode;

    @ApiModelProperty("合同号")
    private String contractCode;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty("入库时间")
    private String warehousingTime;

    @ApiModelProperty("仓管员编码")
    private Integer warehouseKeeperId;

    @ApiModelProperty("仓管员名称")
    private String warehouseKeeperName;

    @ApiModelProperty("结算时间")
    private String auditTime;

    @ApiModelProperty("结算人编码")
    private Integer auditorId;

    @ApiModelProperty("结算人名称")
    private String auditorName;

    @ApiModelProperty("入库备注")
    private String remark;

    @ApiModelProperty("状态：待结算/已结算")
    private String status;

    @ApiModelProperty("是否锁定")
    private Boolean locked;

    @ApiModelProperty("锁定时间")
    private String lockTime;

    @ApiModelProperty("锁定人编码")
    private Integer lockUserId;

    @ApiModelProperty("锁定原因")
    private String lockReason;

    @ApiModelProperty("创建时间")
    private String createTime;

    @ApiModelProperty("更新时间")
    private String updateTime;

    @ApiModelProperty("入库单危废明细列表")
    private List<WarehousingItemResponse> items;
}


