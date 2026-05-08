package io.dbguardian.coordination;

import io.dbguardian.config.DbGuardianDataSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Redis 消息监听器
 * 监听数据源状态变更消息，实现多后端实例间的状态同步
 * 
 * 基于 business-workflow-erp-java 项目的 DatasourceStatusListener 实现
 */
@Slf4j
@Component
public class DatasourceStatusListener implements MessageListener {

    private static final String STATUS_CHANNEL = "dbguardian:datasource:status:channel";

    @Autowired(required = false)
    private RedisMessageListenerContainer container;

    @Autowired(required = false)
    private DatasourceCoordinationService coordinationService;

    @Autowired(required = false)
    private DbGuardianDataSourceConfig dataSourceConfig;

    /**
     * 订阅者注册标记（避免重复订阅）
     */
    private volatile boolean subscribed = false;

    /**
     * 初始化订阅
     */
    @PostConstruct
    public void init() {
        subscribe();
    }

    /**
     * 订阅状态变更消息
     */
    private void subscribe() {
        if (subscribed) {
            return;
        }

        // 检查所有依赖是否都可用
        if (container == null || coordinationService == null || dataSourceConfig == null) {
            log.warn("数据源状态监听器依赖不完整，跳过订阅。RedisContainer: {}, CoordinationService: {}, DataSourceConfig: {}",
                    container != null, coordinationService != null, dataSourceConfig != null);
            return;
        }

        try {
            container.addMessageListener(this, new ChannelTopic(STATUS_CHANNEL));
            subscribed = true;
            log.info("已订阅数据源状态变更频道: {}", STATUS_CHANNEL);
        } catch (Exception e) {
            log.error("订阅数据源状态变更频道失败: {}", e.getMessage());
        }
    }

    /**
     * 取消订阅
     */
    public void unsubscribe() {
        if (!subscribed) {
            return;
        }

        try {
            container.removeMessageListener(this, new ChannelTopic(STATUS_CHANNEL));
            subscribed = false;
            log.info("已取消订阅数据源状态变更频道");
        } catch (Exception e) {
            log.error("取消订阅失败: {}", e.getMessage());
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (coordinationService == null) {
            return;
        }

        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());

            log.info("收到状态变更消息 - 频道: {}, 内容: {}", channel, body);

            // 忽略自己发送的消息
            if (body.equals(coordinationService.getInstanceId())) {
                log.debug("忽略自己发送的消息");
                return;
            }

            // 处理状态变更
            handleStatusChange(body);

        } catch (Exception e) {
            log.error("处理状态变更消息异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理状态变更
     */
    private void handleStatusChange(String status) {
        if (dataSourceConfig == null) {
            return;
        }

        if (DatasourceCoordinationService.STATUS_NORMAL.equals(status)) {
            log.info("收到通知：系统恢复正常，使用主库");
            dataSourceConfig.syncToNormalMode();
        } else if (DatasourceCoordinationService.STATUS_SLAVE_PROMOTED.equals(status)) {
            log.info("收到通知：从库已升为主库，切换到故障转移模式");
            dataSourceConfig.syncToSlavePromotedMode();
        }
    }
}
