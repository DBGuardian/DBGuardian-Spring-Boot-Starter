package io.dbguardian.spring.coordination;

import io.dbguardian.spring.runtime.ClusterRuntimeStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

public class DatasourceStatusListener implements MessageListener {

    public static final String STATUS_CHANNEL = "dbguardian:datasource:status:channel";

    private static final Logger log = LoggerFactory.getLogger(DatasourceStatusListener.class);

    private RedisMessageListenerContainer container;
    private DatasourceCoordinationService coordinationService;
    private ClusterRuntimeStateManager runtimeStateManager;
    private volatile boolean subscribed;

    public void setContainer(RedisMessageListenerContainer container) {
        this.container = container;
    }

    public void setCoordinationService(DatasourceCoordinationService coordinationService) {
        this.coordinationService = coordinationService;
    }

    public void setRuntimeStateManager(ClusterRuntimeStateManager runtimeStateManager) {
        this.runtimeStateManager = runtimeStateManager;
    }

    public void subscribe() {
        if (subscribed) {
            return;
        }
        if (container == null || coordinationService == null) {
            log.debug("数据源状态监听器依赖不完整，跳过订阅。containerReady={}, coordinationReady={}",
                    Boolean.valueOf(container != null), Boolean.valueOf(coordinationService != null));
            return;
        }
        try {
            container.addMessageListener(this, new ChannelTopic(STATUS_CHANNEL));
            subscribed = true;
            log.info("已订阅数据源状态变更频道: {}", STATUS_CHANNEL);
        } catch (Exception ex) {
            log.error("订阅数据源状态变更频道失败: {}", ex.getMessage(), ex);
        }
    }

    public void unsubscribe() {
        if (!subscribed || container == null) {
            return;
        }
        try {
            container.removeMessageListener(this, new ChannelTopic(STATUS_CHANNEL));
            subscribed = false;
            log.info("已取消订阅数据源状态变更频道: {}", STATUS_CHANNEL);
        } catch (Exception ex) {
            log.error("取消订阅数据源状态变更频道失败: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String rawBody = new String(message.getBody(), StandardCharsets.UTF_8);
        String body = normalizeMessage(rawBody);
        log.info("收到状态变更消息 - 频道: {}, 内容: {}", channel, rawBody);
        if (runtimeStateManager != null) {
            runtimeStateManager.replayStatusMessage(body);
        }
        if (DatasourceCoordinationService.STATUS_NORMAL.equals(body)) {
            log.info("系统状态已恢复正常");
        } else if (DatasourceCoordinationService.STATUS_SLAVE_PROMOTED.equals(body)) {
            log.info("收到从库升主通知");
        }
    }

    private String normalizeMessage(String rawBody) {
        if (rawBody == null) {
            return null;
        }
        String body = rawBody.trim();
        if (body.length() >= 2 && body.startsWith("\"") && body.endsWith("\"")) {
            body = body.substring(1, body.length() - 1);
        }
        return body;
    }
}
