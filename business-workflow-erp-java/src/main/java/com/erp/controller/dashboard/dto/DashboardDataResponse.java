package com.erp.controller.dashboard.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 工作台数据响应DTO
 *
 * @author ERP System
 * @date 2026-01-12
 */
@Data
public class DashboardDataResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户信息
     */
    private UserInfo userInfo;

    /**
     * 统计数据
     */
    private Statistics statistics;

    /**
     * 待办事项列表
     */
    private List<TodoItem> todoList;

    /**
     * 消息通知列表
     */
    private List<MessageItem> messageList;

    /**
     * 用户信息
     */
    @Data
    public static class UserInfo implements Serializable {
        private String name;
        private String department;
        private String position;
    }

    /**
     * 统计数据
     */
    @Data
    public static class Statistics implements Serializable {
        /**
         * 客户总数
         */
        private Long customerTotal;

        /**
         * 合同总数
         */
        private Long contractTotal;

        /**
         * 待派单数量
         */
        private Long pendingDispatchCount;

        /**
         * 待收款金额
         */
        private BigDecimal pendingReceiptAmount;
    }

    /**
     * 待办事项
     */
    @Data
    public static class TodoItem implements Serializable {
        /**
         * 事项类型
         */
        private String type;

        /**
         * 事项标题
         */
        private String title;

        /**
         * 事项描述
         */
        private String description;

        /**
         * 数量
         */
        private Long count;
    }

    /**
     * 消息通知
     */
    @Data
    public static class MessageItem implements Serializable {
        /**
         * 消息标题
         */
        private String title;

        /**
         * 消息内容
         */
        private String content;

        /**
         * 消息时间（相对时间，如：2小时前）
         */
        private String time;

        /**
         * 是否已读
         */
        private Boolean isRead;
    }
}

