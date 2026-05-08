package com.erp.controller.ai;

import com.erp.common.exception.BusinessException;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.controller.system.dto.AiChatRequest;
import com.erp.service.system.AiChatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * AI 助手对话接口
 *
 * 说明：
 * 1. 前端 `/app/ai/assistant` 页面通过此接口与大模型进行对话；
 * 2. 接口对外协议使用 `text/event-stream`（SSE）格式，但当前服务端调用大模型时采用**非流式一次性请求**，
 *    会在拿到完整回答后，以单条 `data:` 消息推送整段内容，再发送一个表示结束的 `[STREAM_DONE]` 标记，
 *    因此前端看到的是“基于 SSE 封装的一次性整段推送（兼容流式协议）”，而不是 token 级实时流式输出。
 *
 * 如需切换为真正的流式对话，需要同步调整 `AiChatService` 的实现与前端 SSE 解析逻辑。
 *
 * @author ERP
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@Api(tags = "AI助手")
@Validated
public class AiAssistantController {

    @Autowired
    private AiChatService aiChatService;

    /**
     * AI 对话接口（基于 SSE 的单次整段推送）
     *
     * 协议说明：
     * - 对外使用 {@code text/event-stream}（SSE） 协议进行推送；
     * - 该接口为特殊通道，**不使用项目统一的 Result<code,message,data,timestamp> 响应包装**，
     *   而是直接按 SSE 规范逐条发送 {@code data:...} 行及一个包含 "[STREAM_DONE]" 的结束事件。
     *
     * 实现说明（当前版本）：
     * - 服务端向大模型发起的是非流式一次性请求；
     * - 拿到完整回答后，通过 SSE 推送单条 data 消息承载整段内容，并追加一个包含 "[STREAM_DONE]" 的结束事件。
     *
     * 因此前端仍按 SSE 协议解析，但体验上更接近“整段返回 + 打字机效果”，
     * 如后续引入网关/中间层，请注意该接口不走统一 Result 包装，避免在此处做 JSON 结构假设。
     */
    @RequirePagePermission("AI助手:页面")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "AI对话", notes = "基于默认智能体配置调用大模型接口，流式返回AI回答")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter chat(@Valid @RequestBody AiChatRequest request) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
                new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(60000L);
        try {
            aiChatService.chat(request, new AiChatService.AiStreamConsumer() {
                @Override
                public void onDelta(String delta) {
                    try {
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .data(delta));
                    } catch (Exception e) {
                        log.error("AI 对话推送增量内容失败", e);
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onCompleted() {
                    try {
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .data("[STREAM_DONE]"));
                    } catch (Exception e) {
                        log.warn("AI 对话发送完成事件失败", e);
                    } finally {
                        emitter.complete();
                    }
                }

                @Override
                public void onError(String message) {
                    try {
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .data("ERROR:" + message));
                    } catch (Exception e) {
                        log.warn("AI 对话发送错误事件失败", e);
                    } finally {
                        emitter.complete();
                    }
                }
            });
        } catch (BusinessException ex) {
            log.warn("AI 对话业务异常：{}", ex.getMessage());
            try {
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .data("ERROR:" + ex.getMessage()));
            } catch (Exception ignored) {
            } finally {
                emitter.complete();
            }
        } catch (Exception ex) {
            log.error("AI 对话失败", ex);
            try {
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .data("ERROR:AI对话失败：" + ex.getMessage()));
            } catch (Exception ignored) {
            } finally {
                emitter.complete();
            }
        }
        return emitter;
    }
}




