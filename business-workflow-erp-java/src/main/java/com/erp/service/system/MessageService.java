package com.erp.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.system.dto.MessagePageRequest;
import com.erp.controller.system.dto.MessagePageResponse;
import com.erp.controller.system.dto.MessageDetailResponse;
import com.erp.controller.system.dto.MessageStatisticsResponse;
import com.erp.service.system.dto.MessageDTO;

import java.util.List;

/**
 * 消息管理服务接口
 * 
 * @author ERP System
 * @date 2025-11-27
 */
public interface MessageService {

    /**
     * 分页查询消息列表
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<MessagePageResponse> getMessagePage(MessagePageRequest request);

    /**
     * 获取消息详情
     * 
     * @param messageId 消息ID
     * @return 消息详情
     */
    MessageDetailResponse getMessageDetail(Integer messageId);

    /**
     * 标记消息为已读
     * 
     * @param messageId 消息ID
     */
    void markAsRead(Integer messageId);

    /**
     * 批量标记消息为已读
     * 
     * @param messageIds 消息ID列表
     */
    void batchMarkAsRead(List<Integer> messageIds);

    /**
     * 删除消息
     * 
     * @param messageId 消息ID
     */
    void deleteMessage(Integer messageId);

    /**
     * 批量删除消息
     * 
     * @param messageIds 消息ID列表
     */
    void batchDeleteMessages(List<Integer> messageIds);

    /**
     * 获取未读消息数量
     * 
     * @return 未读消息数量
     */
    int getUnreadCount();

    /**
     * 获取消息统计信息
     * 
     * @return 统计信息
     */
    MessageStatisticsResponse getMessageStatistics();

    /**
     * 全部标记为已读
     */
    void markAllAsRead();

    /**
     * 清空所有消息（软删除）
     */
    void clearAllMessages();

    /**
     * 处理消息队列中的消息（保存到数据库）
     * 
     * @param messageDTO 消息DTO
     */
    void processMessage(MessageDTO messageDTO);
}

































