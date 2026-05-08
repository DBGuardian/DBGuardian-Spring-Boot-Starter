package com.erp.service.system;

import com.erp.service.system.dto.MessageDTO;

/**
 * 消息消费者服务接口
 *
 * @author ERP System
 * @date 2025-11-27
 */
public interface IMessageConsumerService {

    /**
     * 消费通知消息
     *
     * @param messageDTO 消息DTO
     */
    void handleNotificationMessage(MessageDTO messageDTO);

    /**
     * 消费预警消息
     *
     * @param messageDTO 消息DTO
     */
    void handleAlertMessage(MessageDTO messageDTO);

    /**
     * 消费业务通知
     *
     * @param messageDTO 消息DTO
     */
    void handleBusinessMessage(MessageDTO messageDTO);

    /**
     * 消费系统消息
     *
     * @param messageDTO 消息DTO
     */
    void handleSystemMessage(MessageDTO messageDTO);
}
