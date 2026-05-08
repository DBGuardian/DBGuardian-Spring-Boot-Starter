package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 出库单详情响应
 */
@Data
@ApiModel("出库单详情响应")
public class OutboundDetailResponse {

    @ApiModelProperty("出库单编号")
    private Integer outboundId;

    @ApiModelProperty("出库单号")
    private String outboundNo;

    @ApiModelProperty("出库类型")
    private String outboundType;

    @ApiModelProperty("出库时间")
    private String outboundTime;

    @ApiModelProperty("经办人编码")
    private Integer handlerId;

    @ApiModelProperty("经办人名称")
    private String handlerName;

    @ApiModelProperty("审核时间")
    private String auditTime;

    @ApiModelProperty("审核人编码")
    private Integer auditorId;

    @ApiModelProperty("审核人名称")
    private String auditorName;

    @ApiModelProperty("关联合同号")
    private String contractCode;

    @ApiModelProperty("关联客户编号")
    private Integer customerId;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty("出库去向类型")
    private String destinationType;

    @ApiModelProperty("出库去向名称")
    private String destinationName;

    @ApiModelProperty("出库备注")
    private String remark;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("是否锁定")
    private Boolean locked;

    @ApiModelProperty("创建时间")
    private String createTime;

    @ApiModelProperty("更新时间")
    private String updateTime;

    @ApiModelProperty("出库危废明细列表")
    private List<OutboundItemResponse> items;

    /**
     * 出库危废明细响应
     */
    @Data
    @ApiModel("出库危废明细响应")
    public static class OutboundItemResponse {

        @ApiModelProperty("出库危废明细编号")
        private Integer itemId;

        @ApiModelProperty("库存编号")
        private Integer stockId;

        @ApiModelProperty("入库危废明细编号")
        private Integer warehousingItemId;

        @ApiModelProperty("危废条目编号")
        private Integer hazardousWasteItemId;

        @ApiModelProperty("废物名称")
        private String wasteName;

        @ApiModelProperty("废物代码")
        private String wasteCode;

        @ApiModelProperty("危险特性")
        private String hazardFeature;

        @ApiModelProperty("废物形态")
        private String form;

        @ApiModelProperty("库位")
        private String location;

        @ApiModelProperty("基本计量单位")
        private String measureUnit;

        @ApiModelProperty("是否启用辅助核算")
        private Boolean enableAuxiliaryAccounting;

        @ApiModelProperty("辅助计量单位")
        private String auxUnit;

        @ApiModelProperty("辅助单位每基础单位数量")
        private BigDecimal auxPerBase;

        @ApiModelProperty("出库数量（吨）")
        private BigDecimal outboundQty;

        @ApiModelProperty("出库辅助数量")
        private BigDecimal outboundAuxQty;

        @ApiModelProperty("扣减前库存重量")
        private BigDecimal beforeStockQty;

        @ApiModelProperty("扣减后库存重量")
        private BigDecimal afterStockQty;

        @ApiModelProperty("扣减前辅助数量")
        private BigDecimal beforeAuxQty;

        @ApiModelProperty("扣减后辅助数量")
        private BigDecimal afterAuxQty;

        @ApiModelProperty("有价类重量（吨）")
        private BigDecimal valuableWeight;

        @ApiModelProperty("无价类重量（吨）")
        private BigDecimal valuelessWeight;

        @ApiModelProperty("出库原因")
        private String outboundReason;

        @ApiModelProperty("创建时间")
        private String createTime;
    }
}
