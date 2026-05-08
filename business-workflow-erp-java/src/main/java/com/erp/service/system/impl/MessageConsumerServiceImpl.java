package com.erp.service.system.impl;

import com.erp.config.RabbitMQConfig;
import com.erp.service.system.IMessageConsumerService;
import com.erp.service.system.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息消费者服务实现
 *
 * @author ERP System
 * @date 2025-11-27
 */
@Slf4j
@Service
public class MessageConsumerServiceImpl implements IMessageConsumerService {

    @Autowired
    private com.erp.service.system.MessageService messageService;

    @Override
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotificationMessage(MessageDTO messageDTO) {
        try {
            log.info("接收到通知消息: receiverId={}, title={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle());

            messageService.processMessage(messageDTO);

            log.info("通知消息处理完成: receiverId={}, title={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle());
        } catch (Exception e) {
            log.error("处理通知消息失败: receiverId={}, title={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle(), e);
        }
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.ALERT_QUEUE)
    public void handleAlertMessage(MessageDTO messageDTO) {
        try {
            log.info("接收到预警消息: receiverId={}, title={}, priority={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle(), messageDTO.getMessagePriority());

            messageService.processMessage(messageDTO);

            handleHighPriorityAlert(messageDTO);

            log.info("预警消息处理完成: receiverId={}, title={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle());
        } catch (Exception e) {
            log.error("处理预警消息失败: receiverId={}, title={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle(), e);
        }
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.BUSINESS_QUEUE)
    public void handleBusinessMessage(MessageDTO messageDTO) {
        try {
            log.info("接收到业务通知: receiverId={}, title={}, businessType={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle(), messageDTO.getBusinessType());

            messageService.processMessage(messageDTO);

            log.info("业务通知处理完成: receiverId={}, title={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle());
        } catch (Exception e) {
            log.error("处理业务通知失败: receiverId={}, title={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle(), e);
        }
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.SYSTEM_QUEUE)
    public void handleSystemMessage(MessageDTO messageDTO) {
        try {
            log.info("接收到系统消息: receiverId={}, title={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle());

            messageService.processMessage(messageDTO);

            log.info("系统消息处理完成: receiverId={}, title={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle());
        } catch (Exception e) {
            log.error("处理系统消息失败: receiverId={}, title={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle(), e);
        }
    }

    /**
     * 处理高优先级预警
     * 对于紧急和高优先级的预警消息，可能需要额外的处理逻辑
     */
    private void handleHighPriorityAlert(MessageDTO messageDTO) {
        String priority = messageDTO.getMessagePriority();

        if ("紧急".equals(priority) || "高".equals(priority)) {
            log.warn("收到高优先级预警消息: receiverId={}, title={}, priority={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle(), priority);
        }
    }
}
