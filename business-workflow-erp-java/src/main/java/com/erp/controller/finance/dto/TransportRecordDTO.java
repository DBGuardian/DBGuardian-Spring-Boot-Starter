package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运输记录DTO
 */
@Data
@ApiModel("运输记录")
public class TransportRecordDTO {

    @ApiModelProperty("运输单ID")
    private Long transportId;

    @ApiModelProperty("运输单编码")
    private String transportCode;

    @ApiModelProperty("运输时间")
    private String transportTime;

    @ApiModelProperty("废物代码")
    private String wasteCode;

    @ApiModelProperty("废物名称")
    private String wasteName;

    @ApiModelProperty("运输数量")
    private BigDecimal transportQuantity;

    @ApiModelProperty("计量单位")
    private String unit;

    @ApiModelProperty("是否启用辅助核算")
    private Boolean enableAuxiliaryAccounting;

    @ApiModelProperty("辅助数量")
    private BigDecimal auxiliaryQuantity;

    @ApiModelProperty("辅助计量单位")
    private String auxiliaryUnit;
}
