package com.erp.controller.finance.dto;

import com.erp.entity.finance.InvoiceItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发票详情DTO（包含发票信息和明细列表）
 * 用于连表查询结果映射
 *
 * @author ERP System
 * @date 2025-01-07
 */
@Data
public class InvoiceDetailDTO {
    /**
     * 发票基本信息（继承Invoice的所有字段）
     */
    private Integer invoiceId;
    private String invoiceType;
    private String invoiceForm;
    private String invoiceNature;
    private String invoiceStatus;
    private String invoiceNumber;
    private String invoiceCode;
    private LocalDateTime invoiceDate;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String totalAmountInChinese;
    private BigDecimal taxExcludedAmount;
    private Boolean recordDetails;
    private String remark;
    private String issuerName;
    private Integer pdfFileId;
    private Integer imageFileId;
    private Boolean isLocked;
    private LocalDateTime lockTime;
    private Integer lockerId;
    private String lockReason;

    // 创建人信息
    private Integer creatorId;
    private String creatorName;
    
    // 购买方信息
    private String buyerName;
    private String buyerCreditCode;
    private String buyerAddress;
    private String buyerPhone;
    private String buyerBankName;
    private String buyerBankAccount;
    
    // 销售方信息
    private String sellerName;
    private String sellerCreditCode;
    private String sellerAddress;
    private String sellerPhone;
    private String sellerBankName;
    private String sellerBankAccount;
    
    /**
     * 发票明细列表
     */
    private List<InvoiceItem> details;
}

