package com.erp.controller.finance.dto;

import lombok.Data;

/**
 * 创建开票通知单响应DTO
 *
 * @author ERP System
 * @date 2026-01-06
 */
@Data
public class InvoiceNoticeCreateResponse {

    /**
     * 通知单ID
     */
    private Integer noticeId;

    /**
     * 通知单号
     */
    private String noticeNo;
}

