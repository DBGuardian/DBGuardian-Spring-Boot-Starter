package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邮件通道配置响应
 *
 * @author ERP
 */
@Data
public class EmailChannelConfigResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer configId;

    private String displayName;

    private String fromAddress;

    private String replyTo;

    private String smtpHost;

    private Integer smtpPort;

    private String authMethod;

    private String username;

    private String encryption;

    private String status;

    private Integer maxPerHour;

    private Integer maxPerDay;

    private Boolean hasPassword;

    private LocalDateTime updatedAt;

    private Integer updatedBy;

    private LocalDateTime lastSelfTestTime;
}






























