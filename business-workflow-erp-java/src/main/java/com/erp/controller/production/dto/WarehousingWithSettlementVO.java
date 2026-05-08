package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 合同关联入库单响应VO（含业务链和结算状态）
 */
@Data
@ApiModel("合同关联入库单响应")
public class WarehousingWithSettlementVO {

    @ApiModelProperty("入库单编号")
    private Integer warehousingId;

    @ApiModelProperty("入库单号")
    private String warehousingCode;

    @ApiModelProperty("入库时间")
    private String warehousingTime;

    @ApiModelProperty("仓管员编码")
    private Integer warehouseKeeperId;

    @ApiModelProperty("仓管员名称")
    private String warehouseKeeperName;

    @ApiModelProperty("入库备注")
    private String remark;

    @ApiModelProperty("总磅单号")
    private String weighingSlipNo;

    @ApiModelProperty("运输单号")
    private String dispatchCode;

    @ApiModelProperty("收运通知单号")
    private String pickupNoticeCode;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty("总重量（吨）")
    private java.math.BigDecimal totalWeight;

    @ApiModelProperty("入库单状态：待结算/已结算")
    private String status;

    @ApiModelProperty("是否已结算")
    private Boolean settled;

    @ApiModelProperty("关联的结算单编号")
    private Long settlementId;

    @ApiModelProperty("关联的结算单单号")
    private String settlementCode;
}
