package com.erp.controller.customer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 供应商下拉选项响应
 * 用于下拉框等轻量级选择场景
 */
@Data
@ApiModel(description = "供应商下拉选项响应")
public class SupplierSelectResponse {

    @ApiModelProperty(value = "供应商ID", example = "1")
    private Integer supplierId;

    @ApiModelProperty(value = "企业名称", example = "XX危险废物处理有限公司")
    private String enterpriseName;

    @ApiModelProperty(value = "统一社会信用代码", example = "91110000XXXXXXX")
    private String creditCode;

    @ApiModelProperty(value = "联系人", example = "张三")
    private String contactPerson;

    @ApiModelProperty(value = "联系电话", example = "13800138000")
    private String contactPhone;

    @ApiModelProperty(value = "地址", example = "广东省广州市XX区XX路XX号")
    private String address;

    @ApiModelProperty(value = "供应商状态", example = "正常")
    private String supplierStatus;
}
