package com.erp.controller.system.dto;

import lombok.Data;

import java.util.Map;

/**
 * 消息统计响应
 * 
 * @author ERP System
 * @date 2025-11-27
 */
@Data
public class MessageStatisticsResponse {

    /**
     * 未读消息总数
     */
    private Integer unreadCount;

    /**
     * 消息总数
     */
    private Integer totalCount;

    /**
     * 按消息类型统计（总数）
     * key: 消息类型, value: 数量
     */
    private Map<String, Integer> typeStatistics;

    /**
     * 按消息类型统计未读消息
     * key: 消息类型, value: 未读数量
     */
    private Map<String, Integer> unreadTypeStatistics;

    /**
     * 按消息优先级统计
     * key: 消息优先级, value: 数量
     */
    private Map<String, Integer> priorityStatistics;

    /**
     * 按消息状态统计
     * key: 消息状态, value: 数量
     */
    private Map<String, Integer> statusStatistics;
}

































