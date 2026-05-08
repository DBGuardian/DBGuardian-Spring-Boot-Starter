package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.entity.system.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 消息通知Mapper
 * 
 * @author ERP System
 * @date 2025-11-27
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 分页查询消息列表
     * 
     * @param page 分页参数
     * @param receiverId 接收人ID
     * @param messageType 消息类型
     * @param messageStatus 消息状态
     * @param messagePriority 消息优先级
     * @param keyword 关键词
     * @return 分页结果
     */
    IPage<Message> selectMessagePage(Page<Message> page,
                                   @Param("receiverId") Integer receiverId,
                                   @Param("messageType") String messageType,
                                   @Param("messageStatus") String messageStatus,
                                   @Param("messagePriority") String messagePriority,
                                   @Param("keyword") String keyword);

    /**
     * 查询用户未读消息数量
     * 
     * @param receiverId 接收人ID
     * @return 未读消息数量
     */
    int selectUnreadCount(@Param("receiverId") Integer receiverId);

    /**
     * 批量标记消息为已读
     * 
     * @param messageIds 消息ID列表
     * @param receiverId 接收人ID
     * @return 更新数量
     */
    int batchMarkAsRead(@Param("messageIds") List<Integer> messageIds, 
                       @Param("receiverId") Integer receiverId);

    /**
     * 批量删除消息（软删除）
     * 
     * @param messageIds 消息ID列表
     * @param receiverId 接收人ID
     * @return 删除数量
     */
    int batchDeleteMessages(@Param("messageIds") List<Integer> messageIds, 
                           @Param("receiverId") Integer receiverId);

    /**
     * 查询消息统计信息 - 按类型统计
     * 
     * @param receiverId 接收人ID
     * @return 统计信息
     */
    List<Map<String, Object>> selectMessageStatisticsByType(@Param("receiverId") Integer receiverId);

    /**
     * 查询未读消息统计信息 - 按类型统计
     * 
     * @param receiverId 接收人ID
     * @return 统计信息
     */
    List<Map<String, Object>> selectUnreadStatisticsByType(@Param("receiverId") Integer receiverId);

    /**
     * 查询消息统计信息 - 按优先级统计
     * 
     * @param receiverId 接收人ID
     * @return 统计信息
     */
    List<Map<String, Object>> selectMessageStatisticsByPriority(@Param("receiverId") Integer receiverId);

    /**
     * 查询消息统计信息 - 按状态统计
     * 
     * @param receiverId 接收人ID
     * @return 统计信息
     */
    List<Map<String, Object>> selectMessageStatisticsByStatus(@Param("receiverId") Integer receiverId);

    /**
     * 查询消息总数
     * 
     * @param receiverId 接收人ID
     * @return 消息总数
     */
    int selectMessageTotalCount(@Param("receiverId") Integer receiverId);

    /**
     * 查询所有未读消息ID
     * 
     * @param receiverId 接收人ID
     * @return 消息ID列表
     */
    List<Integer> selectAllUnreadMessageIds(@Param("receiverId") Integer receiverId);

    /**
     * 查询所有消息ID（用于清空）
     * 
     * @param receiverId 接收人ID
     * @return 消息ID列表
     */
    List<Integer> selectAllMessageIds(@Param("receiverId") Integer receiverId);

    /**
     * 全部标记为已读
     * 
     * @param receiverId 接收人ID
     * @return 更新数量
     */
    int markAllAsRead(@Param("receiverId") Integer receiverId);

    /**
     * 清空所有消息（软删除）
     * 
     * @param receiverId 接收人ID
     * @return 删除数量
     */
    int clearAllMessages(@Param("receiverId") Integer receiverId);
}

































