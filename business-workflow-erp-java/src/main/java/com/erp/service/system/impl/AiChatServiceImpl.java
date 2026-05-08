package com.erp.service.system.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.AesCryptoUtil;
import com.erp.controller.system.dto.AiChatRequest;
import com.erp.controller.system.dto.AiAgentGlobalConfigResponse;
import com.erp.entity.system.AiAgentConfig;
import com.erp.mapper.system.AiAgentConfigMapper;
import com.erp.service.system.AiAgentConfigService;
import com.erp.service.system.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 助手对话服务实现
 *
 * 默认使用 DeepSeek deepseek-chat 智能体，通过 OpenAI 兼容接口调用
 * 参考文档：https://api-docs.deepseek.com/zh-cn/
 *
 * @author ERP
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final String DEFAULT_PROVIDER = "DEEPSEEK";
    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个集成在危险废物处理企业ERP系统中的中文AI助手，请结合业务语境，用简体中文回答用户问题。回答时尽量给出清晰的操作步骤。";
    private static final int DEFAULT_MAX_MESSAGES = 20;
    private static final int DEFAULT_MAX_CHARS = 6000;

    @Value("${ai.agent.aes-key:}")
    private String aesKey;

    @Autowired
    private AiAgentConfigMapper aiAgentConfigMapper;

    @Autowired
    private AiAgentConfigService aiAgentConfigService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void chat(AiChatRequest request, AiStreamConsumer consumer) {
        AiAgentConfig agent = loadDefaultAgent();
        String apiKeyPlain = decryptApiKey(agent);
        AiAgentGlobalConfigResponse globalConfig = loadGlobalConfigSafe();

        String baseUrl = agent.getBaseUrl();
        if (StrUtil.isBlank(baseUrl)) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "AI智能体接口地址未配置");
        }
        String endpoint = buildChatEndpoint(baseUrl);

        List<ChatMessage> messages = buildMessages(request, globalConfig);
        // 关闭流式：改为一次性请求与响应
        String payload = buildRequestBody(agent, messages, false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKeyPlain);

        try {
            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(payload, headers);
            org.springframework.http.ResponseEntity<String> response =
                    restTemplate.postForEntity(endpoint, entity, String.class);

            HttpStatus status = response.getStatusCode();
            if (!status.is2xxSuccessful()) {
                String error = "调用大模型接口失败：" + status.value();
                log.warn("调用大模型接口失败，状态码：{}", status.value());
                consumer.onError(error);
                return;
            }

            String body = response.getBody();
            if (StrUtil.isBlank(body)) {
                consumer.onError("AI返回内容为空");
                return;
            }

            String content = extractContentFromNonStreamResponse(body);
            if (StrUtil.isNotBlank(content)) {
                consumer.onDelta(content);
            }
            consumer.onCompleted();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("AI 对话调用失败", ex);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "AI对话失败：" + ex.getMessage());
        }
    }

    /**
     * 加载默认启用的智能体配置
     */
    private AiAgentConfig loadDefaultAgent() {
        QueryWrapper<AiAgentConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("启用状态", "ENABLED")
                .eq("默认智能体标记", 1)
                .last("LIMIT 1");
        AiAgentConfig agent = aiAgentConfigMapper.selectOne(wrapper);
        if (agent == null) {
            // 兜底：尝试按提供方 DeepSeek 获取一条
            QueryWrapper<AiAgentConfig> fallback = new QueryWrapper<>();
            fallback.eq("启用状态", "ENABLED")
                    .eq("提供方", DEFAULT_PROVIDER)
                    .last("LIMIT 1");
            agent = aiAgentConfigMapper.selectOne(fallback);
        }
        if (agent == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "尚未配置可用的AI智能体");
        }
        return agent;
    }

    /**
     * 解密 API Key
     */
    private String decryptApiKey(AiAgentConfig agent) {
        if (StrUtil.isBlank(agent.getApiKeyCipher())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "AI智能体未配置API Key");
        }
        if (StrUtil.isBlank(aesKey)) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "未配置AI加密密钥，请联系管理员");
        }
        try {
            return AesCryptoUtil.decrypt(agent.getApiKeyCipher(), aesKey);
        } catch (Exception e) {
            log.error("解密AI智能体API Key失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "AI密钥解密失败，请检查配置");
        }
    }

    private String buildChatEndpoint(String baseUrl) {
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        // DeepSeek 文档：base_url 一般为 https://api.deepseek.com ，对话接口为 /chat/completions
        return trimmed + "/chat/completions";
    }

    /**
     * 构造 OpenAI 兼容格式请求体
     *
     * @param stream 是否开启流式
     */
    private String buildRequestBody(AiAgentConfig agent, List<ChatMessage> messages, boolean stream) {
        String model = StrUtil.isNotBlank(agent.getModelName()) ? agent.getModelName() : DEFAULT_MODEL;

        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"model\":\"").append(escapeJson(model)).append("\",")
                .append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            sb.append("{\"role\":\"").append(escapeJson(msg.getRole()))
                    .append("\",\"content\":\"").append(escapeJson(msg.getContent()))
                    .append("\"}");
            if (i != messages.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("],\"stream\":").append(stream ? "true" : "false").append("}");
        return sb.toString();
    }

    /**
     * 从流式响应分片中提取内容增量
     *
     * 期望结构：
     * {
     *   "choices":[{"delta":{"content":"..."}}, ...]
     * }
     */
    private String extractDeltaFromStreamChunk(String body) {
        if (StrUtil.isBlank(body)) {
            return "";
        }
        int deltaIndex = body.indexOf("\"delta\"");
        if (deltaIndex < 0) {
            return "";
        }
        int contentKeyIndex = body.indexOf("\"content\"", deltaIndex);
        if (contentKeyIndex < 0) {
            return "";
        }
        int colonIndex = body.indexOf(":", contentKeyIndex);
        int firstQuote = body.indexOf("\"", colonIndex + 1);
        int secondQuote = body.indexOf("\"", firstQuote + 1);
        if (firstQuote < 0 || secondQuote < 0) {
            return "";
        }
        String raw = body.substring(firstQuote + 1, secondQuote);
        return unescapeJson(raw);
    }

    /**
     * 从非流式响应中提取完整回答内容
     *
     * 期望结构：
     * {
     *   "choices":[{"message":{"content":"..."}}, ...]
     * }
     */
    private String extractContentFromNonStreamResponse(String body) {
        if (StrUtil.isBlank(body)) {
            return "";
        }
        int messageIndex = body.indexOf("\"message\"");
        if (messageIndex < 0) {
            // 兜底：直接按第一个 content 字段解析
            int contentKeyIndex = body.indexOf("\"content\"");
            if (contentKeyIndex < 0) {
                return "";
            }
            int colonIndex = body.indexOf(":", contentKeyIndex);
            int firstQuote = body.indexOf("\"", colonIndex + 1);
            int secondQuote = body.indexOf("\"", firstQuote + 1);
            if (firstQuote < 0 || secondQuote < 0) {
                return "";
            }
            String raw = body.substring(firstQuote + 1, secondQuote);
            return unescapeJson(raw);
        }

        int contentKeyIndex = body.indexOf("\"content\"", messageIndex);
        if (contentKeyIndex < 0) {
            return "";
        }
        int colonIndex = body.indexOf(":", contentKeyIndex);
        int firstQuote = body.indexOf("\"", colonIndex + 1);
        int secondQuote = body.indexOf("\"", firstQuote + 1);
        if (firstQuote < 0 || secondQuote < 0) {
            return "";
        }
        String raw = body.substring(firstQuote + 1, secondQuote);
        return unescapeJson(raw);
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private List<ChatMessage> buildMessages(AiChatRequest request, AiAgentGlobalConfigResponse globalConfig) {
        List<ChatMessage> messages = new ArrayList<>();

        if (request.getHistory() != null) {
            for (AiChatRequest.HistoryMessage item : request.getHistory()) {
                if (item == null || StrUtil.isBlank(item.getRole()) || StrUtil.isBlank(item.getContent())) {
                    continue;
                }
                messages.add(new ChatMessage(item.getRole(), item.getContent()));
            }
        }

        StringBuilder currentContent = new StringBuilder();
        if (StrUtil.isNotBlank(request.getMode())) {
            currentContent.append("【对话模式】").append(request.getMode()).append("\n");
        }
        if (StrUtil.isNotBlank(request.getContextSummary())) {
            currentContent.append("【业务上下文】").append(request.getContextSummary()).append("\n");
        }
        currentContent.append("【用户问题】").append(request.getQuestion());
        messages.add(new ChatMessage("user", currentContent.toString()));

        applyConversationLimit(messages, globalConfig);

        // 在列表头部加入系统提示词
        messages.add(0, new ChatMessage("system", DEFAULT_SYSTEM_PROMPT));
        return messages;
    }

    private void applyConversationLimit(List<ChatMessage> messages, AiAgentGlobalConfigResponse globalConfig) {
        int maxMessages = globalConfig != null && globalConfig.getMaxConversationMessages() != null
                ? globalConfig.getMaxConversationMessages() : DEFAULT_MAX_MESSAGES;
        int maxChars = globalConfig != null && globalConfig.getMaxConversationChars() != null
                ? globalConfig.getMaxConversationChars() : DEFAULT_MAX_CHARS;

        // 先按条数裁剪（保留最新消息）
        while (messages.size() > maxMessages) {
            messages.remove(0);
        }

        // 再按字符数裁剪（从最旧开始丢弃）
        int totalChars = 0;
        for (ChatMessage msg : messages) {
            totalChars += msg.getContent() != null ? msg.getContent().length() : 0;
        }
        while (totalChars > maxChars && !messages.isEmpty()) {
            ChatMessage removed = messages.remove(0);
            totalChars -= removed.getContent() != null ? removed.getContent().length() : 0;
        }
    }

    private AiAgentGlobalConfigResponse loadGlobalConfigSafe() {
        try {
            AiAgentGlobalConfigResponse cfg = aiAgentConfigService.getGlobalConfig();
            if (cfg.getMaxConversationMessages() == null) {
                cfg.setMaxConversationMessages(DEFAULT_MAX_MESSAGES);
            }
            if (cfg.getMaxConversationChars() == null) {
                cfg.setMaxConversationChars(DEFAULT_MAX_CHARS);
            }
            return cfg;
        } catch (Exception ex) {
            log.warn("加载AI全局配置失败，使用默认会话限制", ex);
            AiAgentGlobalConfigResponse resp = new AiAgentGlobalConfigResponse();
            resp.setMaxRequestsPerSecond(5);
            resp.setMaxTextLengthPerRequest(1200);
            resp.setMaxParagraphsPerRequest(4);
            resp.setMaxConversationMessages(DEFAULT_MAX_MESSAGES);
            resp.setMaxConversationChars(DEFAULT_MAX_CHARS);
            return resp;
        }
    }

    private static class ChatMessage {
        private final String role;
        private final String content;

        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}


