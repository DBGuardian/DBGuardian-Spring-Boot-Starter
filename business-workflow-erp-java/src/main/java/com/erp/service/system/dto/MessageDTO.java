package com.erp.service.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 消息通知DTO
 * 用于RabbitMQ消息传递
 * 
 * @author ERP System
 * @date 2025-11-27
 */
@Data
public class MessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息类型：预警消息/业务通知/系统消息
     */
    private String messageType;

    /**
     * 消息分类：合同/客户/财务/生产/系统等
     */
    private String messageCategory;

    /**
     * 消息标题
     */
    private String messageTitle;

    /**
     * 消息内容
     */
    private String messageContent;

    /**
     * 消息优先级：紧急/高/中/低
     */
    private String messagePriority;

    /**
     * 接收人编码
     */
    private Integer receiverId;

    /**
     * 发送人编码
     */
    private Integer senderId;

    /**
     * 关联业务类型
     */
    private String businessType;

    /**
     * 关联业务ID
     */
    private Integer businessId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建预警消息
     */
    public static MessageDTO createAlert(String category, String title, String content, 
                                       String priority, Integer receiverId, 
                                       String businessType, Integer businessId) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageType("预警消息");
        dto.setMessageCategory(category);
        dto.setMessageTitle(title);
        dto.setMessageContent(content);
        dto.setMessagePriority(priority);
        dto.setReceiverId(receiverId);
        dto.setBusinessType(businessType);
        dto.setBusinessId(businessId);
        return dto;
    }

    /**
     * 创建业务通知
     */
    public static MessageDTO createBusinessNotification(String category, String title, String content,
                                                      Integer receiverId, Integer senderId,
                                                      String businessType, Integer businessId) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageType("业务通知");
        dto.setMessageCategory(category);
        dto.setMessageTitle(title);
        dto.setMessageContent(content);
        dto.setMessagePriority("中");
        dto.setReceiverId(receiverId);
        dto.setSenderId(senderId);
        dto.setBusinessType(businessType);
        dto.setBusinessId(businessId);
        return dto;
    }

    /**
     * 创建系统消息
     */
    public static MessageDTO createSystemMessage(String title, String content, Integer receiverId) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageType("系统消息");
        dto.setMessageCategory("系统");
        dto.setMessageTitle(title);
        dto.setMessageContent(content);
        dto.setMessagePriority("低");
        dto.setReceiverId(receiverId);
        return dto;
    }
}


































