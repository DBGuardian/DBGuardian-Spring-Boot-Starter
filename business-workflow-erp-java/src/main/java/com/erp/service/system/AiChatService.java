package com.erp.service.system;

import com.erp.controller.system.dto.AiChatRequest;

/**
 * AI 助手对话服务
 *
 * 基于已配置的默认大模型智能体，调用第三方大模型接口返回回答
 *
 * @author ERP
 */
public interface AiChatService {

    /**
     * 执行一次流式对话
     *
     * @param request 对话请求（包含会话ID、历史消息、业务上下文等）
     * @param consumer 流式响应消费者，用于将增量内容输出给调用方
     */
    void chat(AiChatRequest request, AiStreamConsumer consumer);

    /**
     * AI 流式响应消费者
     *
     * 用于在 Service 层将模型返回的增量内容传递给 Controller 层，由 Controller 推送给前端。
     */
    interface AiStreamConsumer {

        /**
         * 接收一段增量内容
         *
         * @param delta 模型返回的增量文本内容
         */
        void onDelta(String delta);

        /**
         * 对话完成
         */
        void onCompleted();

        /**
         * 发生错误
         *
         * @param message 错误信息
         */
        void onError(String message);
    }
}




