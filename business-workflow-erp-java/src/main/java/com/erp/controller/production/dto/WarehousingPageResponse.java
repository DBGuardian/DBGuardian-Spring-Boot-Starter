package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 入库单列表项响应
 */
@Data
@ApiModel("入库单列表项响应")
public class WarehousingPageResponse {

    @ApiModelProperty("入库单编号")
    private Integer warehousingId;

    @ApiModelProperty("入库单号")
    private String warehousingNo;

    @ApiModelProperty("总磅单号")
    private String weighingSlipNo;

    // 已移除：收运通知单号（由运输单号唯一标识）

    @ApiModelProperty("运输单号")
    private String dispatchCode;

    @ApiModelProperty("合同号")
    private String contractCode;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty("废物名称")
    private String wasteName;

    @ApiModelProperty("废物代码")
    private String wasteCode;

    @ApiModelProperty("是否存在重大差异")
    private String hasMajorDifference;

    @ApiModelProperty("处理意见")
    private String handlerOpinion;

    @ApiModelProperty("处置方式")
    private String disposalMethod;

    @ApiModelProperty("接受量（吨）")
    private Double acceptedQty;

    @ApiModelProperty("差异说明")
    private String differenceReason;

    @ApiModelProperty("入库时间")
    private String warehousingTime;

    @ApiModelProperty("仓管员编码")
    private Integer warehouseKeeperId;

    @ApiModelProperty("仓管员名称")
    private String warehouseKeeperName;

    @ApiModelProperty("审核时间")
    private String auditTime;

    @ApiModelProperty("审核人编码")
    private Integer auditorId;

    @ApiModelProperty("审核人名称")
    private String auditorName;

    @ApiModelProperty("创建人编码（操作人ID）")
    private Integer creatorId;

    @ApiModelProperty("创建人名称")
    private String creatorName;

    @ApiModelProperty("状态：待结算/已结算/已锁定")
    private String status;

    @ApiModelProperty("是否锁定")
    private Boolean locked;

    @ApiModelProperty("入库备注")
    private String remark;

    @ApiModelProperty("创建时间")
    private String createTime;

    @ApiModelProperty("更新时间")
    private String updateTime;

    @ApiModelProperty("锁定时间")
    private String lockTime;

    @ApiModelProperty("锁定人编码")
    private Integer lockUserId;

    @ApiModelProperty("锁定原因")
    private String lockReason;
}

