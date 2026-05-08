package com.erp.controller.system.dto;

import lombok.Data;

/**
 * 大模型全局配置响应
 *
 * @author ERP
 */
@Data
public class AiAgentGlobalConfigResponse {

    /**
     * 每秒最大请求数
     */
    private Integer maxRequestsPerSecond;

    /**
     * 每次请求最大文本长度
     */
    private Integer maxTextLengthPerRequest;

    /**
     * 每次请求最大段落数
     */
    private Integer maxParagraphsPerRequest;

    /**
     * 单会话最大消息条数
     */
    private Integer maxConversationMessages;

    /**
     * 单会话累计字符上限
     */
    private Integer maxConversationChars;
}





