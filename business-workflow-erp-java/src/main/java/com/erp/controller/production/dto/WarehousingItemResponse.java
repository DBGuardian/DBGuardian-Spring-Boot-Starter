package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 入库单危废明细响应
 */
@Data
@ApiModel("入库单危废明细响应")
public class WarehousingItemResponse {

    @ApiModelProperty("入库危废明细编号")
    private Integer itemId;

    @ApiModelProperty("收运通知单明细编号")
    private Integer pickupNoticeItemId;

    @ApiModelProperty("危废条目编号")
    private Integer hazardousWasteItemId;

    @ApiModelProperty("危废类型（废物类别，如HW01）")
    private String wasteCategory;

    @ApiModelProperty("废物名称")
    private String wasteName;

    @ApiModelProperty("废物代码")
    private String wasteCode;

    @ApiModelProperty("废物形态")
    private String form;

    @ApiModelProperty("危险特性（易燃、腐蚀、有毒等）")
    private String hazardFeature;

    @ApiModelProperty("计划收运数量（基本核算数量，吨，-1代表不限量）")
    private BigDecimal plannedQty;

    @ApiModelProperty("基本计量单位（吨/桶/个等）")
    private String measureUnit;

    @ApiModelProperty("是否启用辅助核算")
    private Boolean enableAuxiliaryAccounting;

    @ApiModelProperty("辅助计量单位（业务友好展示单位，如桶/袋/车等）")
    private String auxUnit;

    @ApiModelProperty("辅助单位每基础单位数量（1基本计量单位≈多少辅助单位，例如1吨≈10桶）")
    private BigDecimal auxPerBase;

    @ApiModelProperty("辅助数量（按辅助计量单位表达的数量，通常对应合同中的桶/袋等数量）")
    private BigDecimal auxQuantity;
    
    @ApiModelProperty("实际收运辅助数量（桶/袋等）")
    private BigDecimal actualAuxQuantity;

    @ApiModelProperty("实际收运数量（吨）")
    private BigDecimal actualQty;

    @ApiModelProperty("实际入库日期")
    private String actualWarehousingDate;

    @ApiModelProperty("更新时间")
    private String updateTime;

    @ApiModelProperty("差异原因")
    private String differenceReason;

    @ApiModelProperty("有价类重量（吨，可回收利用部分，保留6位小数）")
    private java.math.BigDecimal valuableWeight;

    @ApiModelProperty("无价类重量（吨，不可回收利用部分，保留6位小数）")
    private java.math.BigDecimal valuelessWeight;
}


