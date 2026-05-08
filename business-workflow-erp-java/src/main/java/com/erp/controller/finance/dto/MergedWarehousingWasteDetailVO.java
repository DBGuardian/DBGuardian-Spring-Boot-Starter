package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合并后的入库危废明细VO
 */
@Data
@ApiModel("合并后的入库危废明细")
public class MergedWarehousingWasteDetailVO {

    @ApiModelProperty("废物名称")
    private String wasteName;

    @ApiModelProperty("废物代码")
    private String wasteCode;

    @ApiModelProperty("总数量（基本单位）")
    private BigDecimal totalQuantity;

    @ApiModelProperty("基本计量单位")
    private String basicUnit;

    @ApiModelProperty("辅助计量单位")
    private String auxiliaryUnit;

    @ApiModelProperty("总辅助数量")
    private BigDecimal totalAuxiliaryQuantity;

    @ApiModelProperty("付款方（甲方/乙方）")
    private String payer;

    @ApiModelProperty("来源数量")
    private Integer sourceCount;
}
