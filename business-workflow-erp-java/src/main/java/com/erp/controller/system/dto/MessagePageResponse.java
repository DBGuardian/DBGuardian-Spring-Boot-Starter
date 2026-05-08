package com.erp.controller.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息分页响应
 * 
 * @author ERP System
 * @date 2025-11-27
 */
@Data
public class MessagePageResponse {

    /**
     * 消息编号
     */
    private Integer messageId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息分类
     */
    private String messageCategory;

    /**
     * 消息标题
     */
    private String messageTitle;

    /**
     * 消息内容摘要
     */
    private String messageContent;

    /**
     * 消息优先级
     */
    private String messagePriority;

    /**
     * 发送人姓名
     */
    private String senderName;

    /**
     * 关联业务类型
     */
    private String businessType;

    /**
     * 关联业务ID
     */
    private Integer businessId;

    /**
     * 消息状态
     */
    private String messageStatus;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 已读时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime readTime;
}

































