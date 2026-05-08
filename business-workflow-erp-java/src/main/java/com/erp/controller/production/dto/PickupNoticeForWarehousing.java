package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收运通知单信息（用于入库单创建）
 */
@Data
@ApiModel("收运通知单信息（用于入库单创建）")
public class PickupNoticeForWarehousing {

    @ApiModelProperty("收运通知单编号")
    private Integer pickupNoticeId;

    @ApiModelProperty("收运通知单号")
    private String pickupNoticeNo;

    @ApiModelProperty("运输单号")
    private String dispatchCode;

    @ApiModelProperty("合同号")
    private String contractCode;

    @ApiModelProperty("合同待补标记")
    private Boolean contractPending;

    @ApiModelProperty("合同补齐时间")
    private String contractFixTime;

    @ApiModelProperty("客户编码")
    private Integer customerId;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty("单位名称")
    private String companyName;

    @ApiModelProperty("统一社会信用代码")
    private String creditCode;

    @ApiModelProperty("运输地址")
    private String transportAddress;

    @ApiModelProperty("现场联系人")
    private String onsiteContact;

    @ApiModelProperty("现场联系电话")
    private String onsitePhone;

    @ApiModelProperty("应急联系电话")
    private String emergencyPhone;

    @ApiModelProperty("转移交付时间")
    private String transferDeliveryTime;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("危废详情文件")
    private String wasteDetailFile;

    @ApiModelProperty("产废单位二维码")
    private String qrCode;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("提交时间")
    private String submittedAt;

    @ApiModelProperty("审核时间")
    private String auditedAt;

    @ApiModelProperty("审核人编码")
    private Integer auditorId;

    @ApiModelProperty("审核人名称")
    private String auditorName;

    @ApiModelProperty("创建人编码")
    private Integer creatorId;

    @ApiModelProperty("创建人名称")
    private String creatorName;

    @ApiModelProperty("创建时间")
    private String createdAt;

    @ApiModelProperty("更新时间")
    private String updateTime;

    @ApiModelProperty("是否锁定")
    private Boolean locked;

    @ApiModelProperty("锁定时间")
    private String lockTime;

    @ApiModelProperty("锁定人编码")
    private Integer lockUserId;

    @ApiModelProperty("锁定原因")
    private String lockReason;

    @ApiModelProperty("明细列表")
    private List<WasteItem> items;

    /**
     * 危废明细项
     */
    @Data
    @ApiModel("危废明细项")
    public static class WasteItem {
        @ApiModelProperty("收运通知单明细编号")
        private Integer pickupNoticeItemId;

        @ApiModelProperty("废物名称")
        private String wasteName;

        @ApiModelProperty("废物代码")
        private String wasteCode;

        @ApiModelProperty("危险特性（易燃、腐蚀、有毒等）")
        private String hazardFeature;

        @ApiModelProperty("危废条目编号")
        private Integer hazardousWasteItemId;

        @ApiModelProperty("废物类别")
        private String wasteCategory;

        @ApiModelProperty("计划转移数量（吨）")
        private BigDecimal plannedQtyTon;

        @ApiModelProperty("包装方式")
        private String packageType;

        @ApiModelProperty("包装数量")
        private BigDecimal packageQty;

        @ApiModelProperty("废物形态")
        private String form;

        // ========== 基本核算相关字段 ==========
        @ApiModelProperty("基本计量单位（吨/桶/个等），需与合同口径一致")
        private String measureUnit;

        // ========== 辅助核算相关字段 ==========
        @ApiModelProperty("是否启用辅助核算")
        private Boolean enableAuxiliaryAccounting;

        @ApiModelProperty("辅助计量单位（业务友好展示单位，如桶/袋/车等）")
        private String auxUnit;

        @ApiModelProperty("辅助单位每基础单位数量（1基本计量单位≈多少辅助单位，例如1吨≈10桶）")
        private BigDecimal auxPerBase;

        @ApiModelProperty("辅助数量（按辅助计量单位表达的数量，通常对应合同中的桶/袋等数量）")
        private BigDecimal auxQuantity;

        @ApiModelProperty("创建时间")
        private String createdAt;

        @ApiModelProperty("更新时间")
        private String updateTime;
    }
}


