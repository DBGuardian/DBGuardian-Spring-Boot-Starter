package com.erp.controller.system.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 大模型全局配置保存请求
 *
 * @author ERP
 */
@Data
public class AiAgentGlobalConfigSaveRequest {

    /**
     * 每秒最大请求数
     */
    @NotNull(message = "每秒最大请求数不能为空")
    @Min(value = 1, message = "每秒最大请求数必须大于等于1")
    @Max(value = 1000, message = "每秒最大请求数不能超过1000")
    private Integer maxRequestsPerSecond;

    /**
     * 每次请求最大文本长度
     */
    @NotNull(message = "每次请求最大文本长度不能为空")
    @Min(value = 1, message = "每次请求最大文本长度必须大于等于1")
    @Max(value = 20000, message = "每次请求最大文本长度不能超过20000")
    private Integer maxTextLengthPerRequest;

    /**
     * 每次请求最大段落数
     */
    @NotNull(message = "每次请求最大段落数不能为空")
    @Min(value = 1, message = "每次请求最大段落数必须大于等于1")
    @Max(value = 100, message = "每次请求最大段落数不能超过100")
    private Integer maxParagraphsPerRequest;

    /**
     * 单会话最大消息条数
     */
    @NotNull(message = "单会话最大消息条数不能为空")
    @Min(value = 1, message = "单会话最大消息条数必须大于等于1")
    @Max(value = 200, message = "单会话最大消息条数不能超过200")
    private Integer maxConversationMessages;

    /**
     * 单会话累计字符上限
     */
    @NotNull(message = "单会话累计字符上限不能为空")
    @Min(value = 500, message = "单会话累计字符上限必须大于等于500")
    @Max(value = 20000, message = "单会话累计字符上限不能超过20000")
    private Integer maxConversationChars;
}





