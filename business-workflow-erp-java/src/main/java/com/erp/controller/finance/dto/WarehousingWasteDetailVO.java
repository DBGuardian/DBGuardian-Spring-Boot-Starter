package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入库单危废明细VO
 */
@Data
@ApiModel("入库单危废明细")
public class WarehousingWasteDetailVO {

    @ApiModelProperty("入库危废明细编号")
    private Integer warehousingWasteItemId;

    @ApiModelProperty("入库单编号")
    private Integer warehousingId;

    @ApiModelProperty("入库单号")
    private String warehousingCode;

    @ApiModelProperty("入库时间")
    private LocalDateTime warehousingTime;

    @ApiModelProperty("废物名称")
    private String wasteName;

    @ApiModelProperty("废物代码")
    private String wasteCode;

    @ApiModelProperty("废物形态")
    private String form;

    @ApiModelProperty("基本计量单位")
    private String basicUnit;

    @ApiModelProperty("辅助计量单位")
    private String auxiliaryUnit;

    @ApiModelProperty("是否启用辅助核算")
    private Boolean enableAuxiliaryAccounting;

    @ApiModelProperty("实际收运数量（基本单位）")
    private BigDecimal quantity;

    @ApiModelProperty("实际收运辅助数量")
    private BigDecimal auxiliaryQuantity;

    @ApiModelProperty("付款方（甲方/乙方）")
    private String payer;

    @ApiModelProperty("备注")
    private String remark;
}
