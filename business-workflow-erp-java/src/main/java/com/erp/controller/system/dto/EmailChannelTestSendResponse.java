package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邮件自检响应
 *
 * @author ERP
 */
@Data
public class EmailChannelTestSendResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String trackingId;

    private String status;

    private LocalDateTime queuedAt;

    private String targetEmail;
}






























