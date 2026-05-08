package com.erp.service.system.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.system.dto.*;
import com.erp.entity.system.Message;
import com.erp.mapper.system.MessageMapper;
import com.erp.service.system.MessageService;
import com.erp.service.system.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 消息管理服务实现类
 * 
 * @author ERP System
 * @date 2025-11-27
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public IPage<MessagePageResponse> getMessagePage(MessagePageRequest request) {
        Integer currentUserId = getCurrentUserId();
        
        Page<Message> page = new Page<>(request.getCurrent(), request.getSize());
        
        // 处理查询条件
        String messageType = "全部".equals(request.getMessageType()) ? null : request.getMessageType();
        String messageStatus = "全部".equals(request.getMessageStatus()) ? null : request.getMessageStatus();
        String messagePriority = "全部".equals(request.getMessagePriority()) ? null : request.getMessagePriority();
        
        IPage<Message> messagePage = messageMapper.selectMessagePage(page, currentUserId, 
                messageType, messageStatus, messagePriority, request.getKeyword());
        
        // 转换为响应对象
        return messagePage.convert(this::convertToPageResponse);
    }

    @Override
    public MessageDetailResponse getMessageDetail(Integer messageId) {
        Integer currentUserId = getCurrentUserId();
        
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "消息不存在");
        }
        
        // 验证权限
        if (!Objects.equals(message.getReceiverId(), currentUserId)) {
            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "无权查看此消息");
        }
        
        // 自动标记为已读
        if ("未读".equals(message.getMessageStatus())) {
            message.setMessageStatus("已读");
            message.setReadTime(LocalDateTime.now());
            int rows = messageMapper.updateById(message);
            if (rows == 0) {
                log.warn("标记消息已读失败（乐观锁冲突），messageId={}", messageId);
            }
        }

        return convertToDetailResponse(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Integer messageId) {
        Integer currentUserId = getCurrentUserId();
        
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "消息不存在");
        }
        
        if (!Objects.equals(message.getReceiverId(), currentUserId)) {
            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "无权操作此消息");
        }
        
        if (!"未读".equals(message.getMessageStatus())) {
            return; // 已经是已读状态
        }
        
        message.setMessageStatus("已读");
        message.setReadTime(LocalDateTime.now());
        int rows = messageMapper.updateById(message);
        if (rows == 0) {
            log.warn("标记消息已读失败（乐观锁冲突），messageId={}", messageId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMarkAsRead(List<Integer> messageIds) {
        Integer currentUserId = getCurrentUserId();
        
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        
        int updatedCount = messageMapper.batchMarkAsRead(messageIds, currentUserId);
        log.info("批量标记消息已读完成: userId={}, messageIds={}, updatedCount={}", 
                currentUserId, messageIds, updatedCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Integer messageId) {
        Integer currentUserId = getCurrentUserId();
        
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "消息不存在");
        }
        
        if (!Objects.equals(message.getReceiverId(), currentUserId)) {
            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "无权操作此消息");
        }
        
        message.setMessageStatus("已删除");
        message.setDeleteTime(LocalDateTime.now());
        int rows = messageMapper.updateById(message);
        if (rows == 0) {
            throw new BusinessException("删除消息失败：记录已被其他用户修改");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteMessages(List<Integer> messageIds) {
        Integer currentUserId = getCurrentUserId();
        
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        
        int deletedCount = messageMapper.batchDeleteMessages(messageIds, currentUserId);
        log.info("批量删除消息完成: userId={}, messageIds={}, deletedCount={}", 
                currentUserId, messageIds, deletedCount);
    }

    @Override
    public int getUnreadCount() {
        Integer currentUserId = getCurrentUserId();
        return messageMapper.selectUnreadCount(currentUserId);
    }

    @Override
    public MessageStatisticsResponse getMessageStatistics() {
        Integer currentUserId = getCurrentUserId();
        
        MessageStatisticsResponse response = new MessageStatisticsResponse();
        response.setUnreadCount(messageMapper.selectUnreadCount(currentUserId));
        response.setTotalCount(messageMapper.selectMessageTotalCount(currentUserId));
        
        // 按类型统计（总数）
        List<Map<String, Object>> typeStatsList = messageMapper.selectMessageStatisticsByType(currentUserId);
        Map<String, Integer> typeStats = new HashMap<>();
        for (Map<String, Object> stat : typeStatsList) {
            String type = (String) stat.get("type");
            Integer count = ((Number) stat.get("count")).intValue();
            typeStats.put(type, count);
        }
        response.setTypeStatistics(typeStats);

        // 按类型统计未读消息
        List<Map<String, Object>> unreadTypeStatsList = messageMapper.selectUnreadStatisticsByType(currentUserId);
        Map<String, Integer> unreadTypeStats = new HashMap<>();
        for (Map<String, Object> stat : unreadTypeStatsList) {
            String type = (String) stat.get("type");
            Integer count = ((Number) stat.get("count")).intValue();
            unreadTypeStats.put(type, count);
        }
        response.setUnreadTypeStatistics(unreadTypeStats);
        
        // 按优先级统计
        List<Map<String, Object>> priorityStatsList = messageMapper.selectMessageStatisticsByPriority(currentUserId);
        Map<String, Integer> priorityStats = new HashMap<>();
        for (Map<String, Object> stat : priorityStatsList) {
            String priority = (String) stat.get("priority");
            Integer count = ((Number) stat.get("count")).intValue();
            priorityStats.put(priority, count);
        }
        response.setPriorityStatistics(priorityStats);
        
        // 按状态统计
        List<Map<String, Object>> statusStatsList = messageMapper.selectMessageStatisticsByStatus(currentUserId);
        Map<String, Integer> statusStats = new HashMap<>();
        for (Map<String, Object> stat : statusStatsList) {
            String status = (String) stat.get("status");
            Integer count = ((Number) stat.get("count")).intValue();
            statusStats.put(status, count);
        }
        response.setStatusStatistics(statusStats);
        
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead() {
        Integer currentUserId = getCurrentUserId();
        int updatedCount = messageMapper.markAllAsRead(currentUserId);
        log.info("全部标记已读完成: userId={}, updatedCount={}", currentUserId, updatedCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllMessages() {
        Integer currentUserId = getCurrentUserId();
        int deletedCount = messageMapper.clearAllMessages(currentUserId);
        log.info("清空所有消息完成: userId={}, deletedCount={}", currentUserId, deletedCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processMessage(MessageDTO messageDTO) {
        try {
            // 检查接收人编码是否为空，如果为空则跳过
            if (messageDTO.getReceiverId() == null) {
                log.warn("消息保存跳过：接收人编码为空, title={}, messageType={}, businessType={}",
                        messageDTO.getMessageTitle(), messageDTO.getMessageType(), messageDTO.getBusinessType());
                return;
            }

            Message message = convertFromDTO(messageDTO);
            messageMapper.insert(message);
            log.info("消息保存成功: receiverId={}, title={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle());
        } catch (Exception e) {
            log.error("消息保存失败: receiverId={}, title={}",
                    messageDTO.getReceiverId(), messageDTO.getMessageTitle(), e);
            throw e;
        }
    }

    /**
     * 转换为分页响应对象
     */
    private MessagePageResponse convertToPageResponse(Message message) {
        MessagePageResponse response = new MessagePageResponse();
        response.setMessageId(message.getMessageId());
        response.setMessageType(message.getMessageType());
        response.setMessageCategory(message.getMessageCategory());
        response.setMessageTitle(message.getMessageTitle());
        
        // 内容摘要（截取前100个字符）
        String content = message.getMessageContent();
        if (StrUtil.isNotBlank(content) && content.length() > 100) {
            content = content.substring(0, 100) + "...";
        }
        response.setMessageContent(content);
        
        response.setMessagePriority(message.getMessagePriority());
        response.setSenderName(message.getSenderName());
        response.setBusinessType(message.getBusinessType());
        response.setBusinessId(message.getBusinessId());
        response.setMessageStatus(message.getMessageStatus());
        response.setCreateTime(message.getCreateTime());
        response.setReadTime(message.getReadTime());
        
        return response;
    }

    /**
     * 转换为详情响应对象
     */
    private MessageDetailResponse convertToDetailResponse(Message message) {
        MessageDetailResponse response = new MessageDetailResponse();
        response.setMessageId(message.getMessageId());
        response.setMessageType(message.getMessageType());
        response.setMessageCategory(message.getMessageCategory());
        response.setMessageTitle(message.getMessageTitle());
        response.setMessageContent(message.getMessageContent());
        response.setMessagePriority(message.getMessagePriority());
        response.setReceiverId(message.getReceiverId());
        response.setReceiverName(message.getReceiverName());
        response.setSenderId(message.getSenderId());
        response.setSenderName(message.getSenderName());
        response.setBusinessType(message.getBusinessType());
        response.setBusinessId(message.getBusinessId());
        response.setMessageStatus(message.getMessageStatus());
        response.setCreateTime(message.getCreateTime());
        response.setReadTime(message.getReadTime());
        response.setRemark(message.getRemark());
        
        return response;
    }

    /**
     * 从DTO转换为实体对象
     */
    private Message convertFromDTO(MessageDTO dto) {
        Message message = new Message();
        message.setMessageType(dto.getMessageType());
        message.setMessageCategory(dto.getMessageCategory());
        message.setMessageTitle(dto.getMessageTitle());
        message.setMessageContent(dto.getMessageContent());
        message.setMessagePriority(dto.getMessagePriority());
        message.setReceiverId(dto.getReceiverId());
        message.setSenderId(dto.getSenderId());
        message.setBusinessType(dto.getBusinessType());
        message.setBusinessId(dto.getBusinessId());
        message.setMessageStatus("未读");
        message.setRemark(dto.getRemark());
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        
        return message;
    }

    /**
     * 获取当前用户ID
     */
    private Integer getCurrentUserId() {
        Integer userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "未登录");
        }
        return userId;
    }
}

































