package com.erp.controller.finance.dto;

import com.erp.entity.finance.InvoiceItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发票分页查询响应
 *
 * 对应 INVOICE 表的所有字段，包含明细列表，用于发票列表展示。
 * 字段权限：同时支持进项发票和销项发票两个页面的字段级权限控制。
 */
@Data
@ApiModel("发票分页查询响应")
public class InvoicePageResponse {

    @ApiModelProperty("发票编号")
    private Integer invoiceId;

    @ApiModelProperty("发票类型：增值税专用发票/普通发票")
    private String invoiceType;

    @ApiModelProperty("发票形式：数电发票/电子发票/纸质发票")
    private String invoiceForm;

    @ApiModelProperty("发票性质：红字/蓝字")
    private String invoiceNature;

    @ApiModelProperty("发票状态：进项发票/销项发票")
    private String invoiceStatus;

    @ApiModelProperty("金额（元）")
    private BigDecimal amount;

    @ApiModelProperty("税额（元）")
    private BigDecimal taxAmount;

    @ApiModelProperty("价税合计（元）")
    private BigDecimal totalAmount;

    @ApiModelProperty("价税合计大写")
    private String totalAmountInChinese;

    @ApiModelProperty("不含税金额（元）")
    private BigDecimal taxExcludedAmount;

    @ApiModelProperty("发票号码")
    private String invoiceNumber;

    @ApiModelProperty("发票代码")
    private String invoiceCode;

    @ApiModelProperty("开票日期")
    private LocalDateTime invoiceDate;

    @ApiModelProperty("购买方名称")
    private String buyerName;

    @ApiModelProperty("购买方统一社会信用代码")
    private String buyerCreditCode;

    @ApiModelProperty("购买方地址")
    private String buyerAddress;

    @ApiModelProperty("购买方电话")
    private String buyerPhone;

    @ApiModelProperty("购买方开户行")
    private String buyerBankName;

    @ApiModelProperty("购买方账号")
    private String buyerBankAccount;

    @ApiModelProperty("销售方名称")
    private String sellerName;

    @ApiModelProperty("销售方统一社会信用代码")
    private String sellerCreditCode;

    @ApiModelProperty("销售方地址")
    private String sellerAddress;

    @ApiModelProperty("销售方电话")
    private String sellerPhone;

    @ApiModelProperty("销售方开户行")
    private String sellerBankName;

    @ApiModelProperty("销售方账号")
    private String sellerBankAccount;

    @ApiModelProperty("是否录入发票明细")
    private Boolean recordDetails;

    @ApiModelProperty("发票扫描件PDF文件编号")
    private Integer pdfFileId;

    @ApiModelProperty("发票扫描件图片文件编号")
    private Integer imageFileId;

    @ApiModelProperty("开票人名称")
    private String issuerName;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("是否锁定")
    private Boolean isLocked;

    @ApiModelProperty("锁定时间")
    private LocalDateTime lockTime;

    @ApiModelProperty("锁定人编码")
    private Integer lockerId;

    @ApiModelProperty("锁定原因")
    private String lockReason;

    @ApiModelProperty("创建人编码")
    private Integer creatorId;

    @ApiModelProperty("创建人名称")
    private String creatorName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("发票明细列表")
    private List<InvoiceItem> details;
}




