package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 开票通知单-发票关联明细响应DTO
 */
@Data
@ApiModel("开票通知单-发票关联明细响应")
public class InvoiceNoticeInvoiceDTO {

    @ApiModelProperty("明细编号")
    private Integer detailId;

    @ApiModelProperty("开票通知单编号")
    private Integer noticeId;

    @ApiModelProperty("发票编号")
    private Integer invoiceId;

    @ApiModelProperty("发票号码")
    private String invoiceNumber;

    @ApiModelProperty("发票代码")
    private String invoiceCode;

    @ApiModelProperty("开票日期")
    private LocalDate invoiceDate;

    @ApiModelProperty("发票类型")
    private String invoiceType;

    @ApiModelProperty("发票形式")
    private String invoiceForm;

    @ApiModelProperty("不含税金额")
    private BigDecimal amount;

    @ApiModelProperty("税额")
    private BigDecimal taxAmount;

    @ApiModelProperty("价税合计")
    private BigDecimal totalAmount;

    @ApiModelProperty("购买方名称")
    private String buyerName;

    @ApiModelProperty("购买方统一社会信用代码")
    private String buyerCreditCode;

    @ApiModelProperty("销售方名称")
    private String sellerName;

    @ApiModelProperty("销售方统一社会信用代码")
    private String sellerCreditCode;

    @ApiModelProperty("备注")
    private String remark;
}

