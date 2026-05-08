package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 运输单详情响应
 */
@Data
@ApiModel("运输单详情响应")
public class TransportDispatchDetailResponse {

    @ApiModelProperty("运输单编号")
    private Integer dispatchId;

    @ApiModelProperty("运输单号")
    private String dispatchCode;

    @ApiModelProperty("收运通知单号")
    private String noticeCode;

    @ApiModelProperty("合同号")
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

    @ApiModelProperty("派车时间")
    private String dispatchAt;

    @ApiModelProperty("实际起运时间")
    private String departAt;

    @ApiModelProperty("实际到达时间")
    private String arriveAt;

    @ApiModelProperty("计划转移数量(吨)")
    private Double planQuantityTon;

    @ApiModelProperty("运输距离（公里）")
    private BigDecimal transportDistance;

    @ApiModelProperty("调度备注")
    private String dispatcherRemark;

    @ApiModelProperty("状态：待运输/运输中/已到达/已完成/已取消")
    private String status;

    @ApiModelProperty("是否锁定")
    private Boolean locked;

    @ApiModelProperty("锁定原因")
    private String lockReason;

    @ApiModelProperty("锁定时间")
    private String lockTime;

    @ApiModelProperty("调度员姓名")
    private String dispatcherName;

    @ApiModelProperty("调度员编码")
    private Integer dispatcherId;

    @ApiModelProperty("锁定人编码")
    private Integer lockUserId;

    @ApiModelProperty("创建时间")
    private String createdAt;

    @ApiModelProperty("更新时间")
    private String updatedAt;

    @ApiModelProperty("是否超限")
    private Boolean overLimit;

    @ApiModelProperty("超限条目")
    private List<DispatchOverLimitItemResponse> overLimitItems;

    @ApiModelProperty("合同缺失标记")
    private Boolean contractMissing;

    @ApiModelProperty("危废明细列表")
    private List<WasteItemDetail> wasteItems;

    @ApiModelProperty("常用单位二维码文件路径（本地路径）")
    private String qrcodeFilePath;

    /**
     * 危废明细项
     */
    @Data
    @ApiModel("危废明细项")
    public static class WasteItemDetail {
        @ApiModelProperty("废物名称")
        private String wasteName;

        @ApiModelProperty("废物代码")
        private String wasteCode;

        @ApiModelProperty("危险特性")
        private String hazardFeature;

        @ApiModelProperty("废物形态")
        private String form;

        @ApiModelProperty("有害成分名称")
        private String hazardousComponentName;

        @ApiModelProperty("包装方式")
        private String packageType;

        @ApiModelProperty("包装数量")
        private java.math.BigDecimal packageQty;

        @ApiModelProperty("计划转移数量（吨）")
        private java.math.BigDecimal plannedQtyTon;

        @ApiModelProperty("基本计量单位（吨/桶/个等）")
        private String measureUnit;

        @ApiModelProperty("是否启用辅助核算")
        private Boolean enableAuxiliaryAccounting;

        @ApiModelProperty("辅助计量单位（业务友好展示单位，如桶/袋/车等）")
        private String auxUnit;

        @ApiModelProperty("辅助单位每基础单位数量（1基本计量单位≈多少辅助单位，例如1吨≈10桶）")
        private java.math.BigDecimal auxPerBase;

        @ApiModelProperty("辅助数量（按辅助计量单位表达的数量，通常对应合同中的桶/袋等数量）")
        private java.math.BigDecimal auxQuantity;

        @ApiModelProperty("危废条目编号（引用国家危废名录条目）")
        private Integer hazardousWasteItemId;

        @ApiModelProperty("危废类别编码")
        private String wasteCategory;
    }
}


