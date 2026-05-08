package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 收运通知单危废明细请求
 */
@Data
@ApiModel("收运通知单危废明细请求")
public class TransportApplyItemRequest {

    @ApiModelProperty("明细编号（更新时使用）")
    private Integer id;

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

    @ApiModelProperty("包装方式（兼容字段，用于存储辅助计量单位）")
    private String packageType;

    @ApiModelProperty("包装数量（兼容字段，用于存储辅助核算数量）")
    private BigDecimal packageQty;

    // ========== 基本核算相关字段 ==========
    @ApiModelProperty("基本核算数量（吨），-1代表不限量")
    private BigDecimal plannedQtyTon;

    @ApiModelProperty("基本计量单位（吨/桶/个等），需与合同口径一致")
    private String measureUnit;

    // ========== 辅助核算相关字段 ==========
    @ApiModelProperty("是否启用辅助核算")
    private Boolean enableAuxiliaryAccounting;

    @ApiModelProperty("辅助计量单位（业务友好展示单位，如桶/袋/车等）")
    private String auxUnit;

    @ApiModelProperty("辅助单位每基础单位数量（1基本计量单位≈多少辅助单位，例如1吨≈10桶）")
    private BigDecimal auxPerBase;

    @ApiModelProperty("辅助核算数量（按辅助计量单位表达的数量，通常对应合同中的桶/袋等数量）")
    private BigDecimal auxQuantity;

    @ApiModelProperty("辅助单位数量（兼容旧字段，映射到 auxQuantity）")
    private BigDecimal auxUnitQty;

    @ApiModelProperty("合同危废明细编号")
    private Integer contractWasteItemId;

    @ApiModelProperty("危废条目编号")
    private Integer hazardousWasteItemId;
}

