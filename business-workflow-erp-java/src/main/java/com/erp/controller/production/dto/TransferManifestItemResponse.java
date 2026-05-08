package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 转移联单废物子项响应
 */
@Data
@ApiModel("转移联单废物子项响应")
public class TransferManifestItemResponse {

    @ApiModelProperty("子项编号")
    private Integer 子项编号;

    @ApiModelProperty("所属联单编号")
    private Integer 联单编号;

    @ApiModelProperty("废物类别")
    private String 废物类别;

    @ApiModelProperty("废物代码")
    private String 废物代码;

    @ApiModelProperty("废物名称")
    private String 废物名称;

    @ApiModelProperty("废物形态")
    private String 废物形态;

    @ApiModelProperty("包装方式")
    private String 包装方式;

    @ApiModelProperty("计划数量")
    private BigDecimal 计划数量;

    @ApiModelProperty("确认数量")
    private BigDecimal 确认数量;

    @ApiModelProperty("计量单位")
    private String 计量单位;
}
