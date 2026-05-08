package com.erp.controller.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * AI 助手对话请求
 *
 * @author ERP
 */
@Data
@ApiModel("AI助手对话请求")
public class AiChatRequest {

    /**
     * 对话模式：general/business/analysis 等
     */
    @ApiModelProperty(value = "对话模式", example = "general")
    private String mode;

    /**
     * 用户问题
     */
    @NotBlank(message = "问题内容不能为空")
    @ApiModelProperty(value = "问题内容", required = true, example = "如何创建新的收运通知单？")
    private String question;

    /**
     * 业务上下文摘要（可选）
     */
    @ApiModelProperty(value = "业务上下文摘要", example = "当前页面：收运通知单列表，业务类型：PRODUCTION")
    private String contextSummary;

    /**
     * 会话标识（前端用于聚合同一会话）
     */
    @ApiModelProperty(value = "会话ID", example = "session-1700000000000")
    private String sessionId;

    /**
     * 前端本地维护的对话历史
     */
    @ApiModelProperty(value = "对话历史", notes = "包含 user/assistant 的历史消息，用于服务端拼装上下文")
    private java.util.List<HistoryMessage> history;

    @Data
    public static class HistoryMessage {

        /**
         * 角色：user/assistant
         */
        @ApiModelProperty(value = "角色", allowableValues = "user,assistant")
        private String role;

        /**
         * 消息内容
         */
        @ApiModelProperty(value = "消息内容")
        private String content;
    }
}




