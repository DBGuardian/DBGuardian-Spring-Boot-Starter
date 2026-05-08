package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 可结算入库单VO
 */
@Data
@ApiModel("可结算入库单")
public class AvailableWarehousingVO {

    @ApiModelProperty("入库单编号")
    private Long warehousingId;

    @ApiModelProperty("入库单号")
    private String warehousingCode;

    @ApiModelProperty("入库时间")
    private LocalDateTime warehousingTime;

    @ApiModelProperty("仓管员编码")
    private Integer warehouseKeeperId;

    @ApiModelProperty("仓管员姓名")
    private String warehouseKeeperName;

    @ApiModelProperty("入库备注")
    private String remark;

    @ApiModelProperty("运输单号")
    private String dispatchCode;

    @ApiModelProperty("收运通知单号")
    private String pickupNoticeCode;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty("总重量（吨）")
    private BigDecimal totalWeight;
}

