package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 发票分页查询请求
 *
 * 支持按发票状态（进项/销项）、发票类型、发票形式、发票性质、状态、开票日期范围等条件筛选，
 * 以及按发票号码、销售方名称/统一社会信用代码、购买方名称/统一社会信用代码进行模糊查询。
 */
@Data
@ApiModel("发票分页查询请求")
public class InvoicePageRequest {

    @ApiModelProperty(value = "当前页码（从1开始）", example = "1")
    @Min(value = 1, message = "页码不能小于1")
    private Integer page = 1;

    @ApiModelProperty(value = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量不能小于1")
    private Integer size = 10;

    @ApiModelProperty(value = "发票状态（进项发票/销项发票）", required = true, example = "进项发票")
    private String invoiceStatus;

    @ApiModelProperty(value = "发票类型（增值税专用发票/普通发票）")
    private String invoiceType;

    @ApiModelProperty(value = "发票形式（数电发票/电子发票/纸质发票）")
    private String invoiceForm;

    @ApiModelProperty(value = "发票性质（红字/蓝字）")
    private String invoiceNature;

    @ApiModelProperty(value = "开票日期开始（yyyy-MM-dd）")
    private String invoiceDateStart;

    @ApiModelProperty(value = "开票日期结束（yyyy-MM-dd）")
    private String invoiceDateEnd;

    @ApiModelProperty(value = "发票号码（模糊查询）")
    private String invoiceNumber;

    @ApiModelProperty(value = "销售方名称（模糊查询）")
    private String sellerName;

    @ApiModelProperty(value = "销售方统一社会信用代码（模糊查询）")
    private String sellerCreditCode;

    @ApiModelProperty(value = "购买方名称（模糊查询）")
    private String buyerName;

    @ApiModelProperty(value = "购买方统一社会信用代码（模糊查询）")
    private String buyerCreditCode;

    @ApiModelProperty(value = "排序字段（支持：invoiceId, invoiceNumber, invoiceCode, invoiceType, invoiceForm, invoiceNature, invoiceStatus, amount, taxAmount, totalAmount, invoiceDate, buyerName, sellerName, createTime, updateTime）")
    private String sortField;

    @ApiModelProperty(value = "排序方向：asc/desc", example = "desc")
    private String sortOrder;

    @ApiModelProperty(value = "数据范围过滤：SELF（仅查看自己创建）/ ALL（查看全部）")
    private String viewScope;

    @ApiModelProperty(value = "创建人编码过滤（由Service层自动设置）")
    private Integer creatorFilter;
}



