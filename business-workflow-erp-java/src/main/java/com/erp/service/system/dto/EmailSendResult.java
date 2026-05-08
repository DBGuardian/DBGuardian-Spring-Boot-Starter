package com.erp.service.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通用邮件发送结果
 *
 * @author ERP
 */
@Data
public class EmailSendResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 追踪ID
     */
    private String trackingId;

    /**
     * 状态（SENT/FAILED等）
     */
    private String status;

    /**
     * 入队/发送时间
     */
    private LocalDateTime queuedAt;

    /**
     * 实际收件人
     */
    private List<String> targetEmails;
}






























