package com.erp.controller.finance.dto;

import com.erp.entity.finance.InvoiceItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发票分页查询响应（销项发票）
 *
 * <p>对应页面：财务管理:发票管理:销项发票:页面</p>
 * <p>字段权限：仅绑定销项发票页面的字段级权限编码</p>
 */
@Data
@ApiModel("发票分页查询响应（销项发票）")
public class OutputInvoicePageResponse {

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

    /**
     * 从通用发票分页响应复制字段，便于统一组装数据后再区分权限视图。
     */
    public static OutputInvoicePageResponse from(InvoicePageResponse source) {
        if (source == null) {
            return null;
        }
        OutputInvoicePageResponse target = new OutputInvoicePageResponse();
        target.setInvoiceId(source.getInvoiceId());
        target.setInvoiceType(source.getInvoiceType());
        target.setInvoiceForm(source.getInvoiceForm());
        target.setInvoiceNature(source.getInvoiceNature());
        target.setInvoiceStatus(source.getInvoiceStatus());
        target.setAmount(source.getAmount());
        target.setTaxAmount(source.getTaxAmount());
        target.setTotalAmount(source.getTotalAmount());
        target.setTotalAmountInChinese(source.getTotalAmountInChinese());
        target.setTaxExcludedAmount(source.getTaxExcludedAmount());
        target.setInvoiceNumber(source.getInvoiceNumber());
        target.setInvoiceCode(source.getInvoiceCode());
        target.setInvoiceDate(source.getInvoiceDate());
        target.setBuyerName(source.getBuyerName());
        target.setBuyerCreditCode(source.getBuyerCreditCode());
        target.setBuyerAddress(source.getBuyerAddress());
        target.setBuyerPhone(source.getBuyerPhone());
        target.setBuyerBankName(source.getBuyerBankName());
        target.setBuyerBankAccount(source.getBuyerBankAccount());
        target.setSellerName(source.getSellerName());
        target.setSellerCreditCode(source.getSellerCreditCode());
        target.setSellerAddress(source.getSellerAddress());
        target.setSellerPhone(source.getSellerPhone());
        target.setSellerBankName(source.getSellerBankName());
        target.setSellerBankAccount(source.getSellerBankAccount());
        target.setRecordDetails(source.getRecordDetails());
        target.setPdfFileId(source.getPdfFileId());
        target.setImageFileId(source.getImageFileId());
        target.setIssuerName(source.getIssuerName());
        target.setRemark(source.getRemark());
        target.setIsLocked(source.getIsLocked());
        target.setLockTime(source.getLockTime());
        target.setLockerId(source.getLockerId());
        target.setLockReason(source.getLockReason());
        target.setCreatorId(source.getCreatorId());
        target.setCreatorName(source.getCreatorName());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setDetails(source.getDetails());
        return target;
    }
}

