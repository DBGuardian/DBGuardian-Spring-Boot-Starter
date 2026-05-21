package io.dbguardian.coordination;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 数据源分布式协调服务
 * 使用 Redis 实现多个后端实例间的数据源状态同步
 *
 * 注意：此类由 DbGuardianAutoConfiguration 的 @Bean 方法注册
 * 不要添加 @Service 或其他 @Component 注解，避免 Bean 重复定义
 */
@Slf4j
public class DatasourceCoordinationService {

    private static final String MASTER_STATUS_KEY = "dbguardian:datasource:master:status";
    private static final String MASTER_INSTANCE_KEY = "dbguardian:datasource:master:instance";
    private static final String FAILOVER_LOCK_KEY = "dbguardian:datasource:failover:lock";
    private static final String STATUS_CHANNEL = "dbguardian:datasource:status:channel";

    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_SLAVE_PROMOTED = "SLAVE_PROMOTED";

    private RedisTemplate<String, Object> redisTemplate;

    private volatile boolean redisAvailable = true;

    /**
     * 当前实例的唯一标识
     */
    private final String instanceId = UUID.randomUUID().toString();

    @Value("${spring.application.name:dbguardian}")
    private String applicationName;

    /**
     * 用于 Spring 注入 RedisTemplate
     */
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 用于 Spring 注入 applicationName
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * 获取当前实例ID
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 获取当前主库状态
     * @return NORMAL-主库正常, SLAVE_PROMOTED-从库升主库
     */
    public String getMasterStatus() {
        if (redisTemplate == null) {
            return STATUS_NORMAL;
        }
        try {
            Object status = redisTemplate.opsForValue().get(MASTER_STATUS_KEY);
            if (status == null) {
                return STATUS_NORMAL;
            }
            return status.toString();
        } catch (Exception e) {
            log.error("获取主库状态失败: {}", e.getMessage());
            return STATUS_NORMAL;
        }
    }

    /**
     * 设置主库状态（当故障转移发生时）
     * @param status 状态值
     */
    public void setMasterStatus(String status) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(MASTER_STATUS_KEY, status);
            log.info("更新主库状态到Redis: {}", status);

            // 广播状态变更
            broadcastStatusChange(status);
        } catch (Exception e) {
            log.error("设置主库状态失败: {}", e.getMessage());
        }
    }

    /**
     * 获取当前主库实例ID
     * @return 实例ID
     */
    public String getMasterInstanceId() {
        if (redisTemplate == null) {
            return null;
        }
        try {
            Object instanceId = redisTemplate.opsForValue().get(MASTER_INSTANCE_KEY);
            return instanceId != null ? instanceId.toString() : null;
        } catch (Exception e) {
            log.error("获取主库实例ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 注册为主库实例
     * @param instanceId 实例ID
     */
    public void registerAsMaster(String instanceId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(MASTER_INSTANCE_KEY, instanceId);
            log.info("注册主库实例: {}", instanceId);
        } catch (Exception e) {
            log.error("注册主库实例失败: {}", e.getMessage());
        }
    }

    /**
     * 尝试获取故障转移锁（分布式锁）
     * 确保只有一个实例执行故障转移
     * @param expireSeconds 锁过期时间（秒）
     * @return true-获取成功，false-获取失败
     */
    public boolean tryAcquireFailoverLock(long expireSeconds) {
        if (redisTemplate == null) {
            return false;
        }
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                FAILOVER_LOCK_KEY,
                instanceId,
                expireSeconds,
                TimeUnit.SECONDS
            );
            boolean acquired = Boolean.TRUE.equals(success);
            if (acquired) {
                log.info("获取故障转移锁成功，当前实例: {}", instanceId);
            } else {
                log.info("获取故障转移锁失败，已有其他实例在执行");
            }
            return acquired;
        } catch (Exception e) {
            log.error("获取故障转移锁异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 释放故障转移锁
     */
    public void releaseFailoverLock() {
        if (redisTemplate == null) {
            return;
        }
        try {
            Object currentHolder = redisTemplate.opsForValue().get(FAILOVER_LOCK_KEY);
            if (instanceId.equals(currentHolder)) {
                redisTemplate.delete(FAILOVER_LOCK_KEY);
                log.info("释放故障转移锁");
            }
        } catch (Exception e) {
            log.error("释放故障转移锁失败: {}", e.getMessage());
        }
    }

    /**
     * 检查是否应该由当前实例执行故障转移
     * @return true-应该执行，false-不应该执行
     */
    public boolean shouldExecuteFailover() {
        try {
            String masterInstanceId = getMasterInstanceId();
            if (masterInstanceId == null) {
                return true;
            }
            return instanceId.equals(masterInstanceId);
        } catch (Exception e) {
            log.error("检查故障转移执行权失败: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 广播状态变更到所有实例
     */
    private void broadcastStatusChange(String status) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.convertAndSend(STATUS_CHANNEL, status);
            log.info("广播状态变更: {}", status);
        } catch (Exception e) {
            log.error("广播状态变更失败: {}", e.getMessage());
        }
    }

    /**
     * 广播故障转移事件
     * @param oldMasterId 原主库ID
     * @param newMasterId 新主库ID
     */
    public void broadcastFailoverEvent(String oldMasterId, String newMasterId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String message = String.format("FAILOVER:%s:%s", oldMasterId, newMasterId);
            redisTemplate.convertAndSend(STATUS_CHANNEL, message);
            log.info("广播故障转移事件: {} -> {}", oldMasterId, newMasterId);
        } catch (Exception e) {
            log.error("广播故障转移事件失败: {}", e.getMessage());
        }
    }

    /**
     * 广播从库状态变更
     * @param slaveId 从库ID
     * @param available 是否可用
     */
    public void broadcastSlaveStatus(String slaveId, boolean available) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String message = String.format("SLAVE_STATUS:%s:%s", slaveId, available);
            redisTemplate.convertAndSend(STATUS_CHANNEL, message);
            log.info("广播从库状态变更: {} -> {}", slaveId, available ? "UP" : "DOWN");
        } catch (Exception e) {
            log.error("广播从库状态变更失败: {}", e.getMessage());
        }
    }

    /**
     * 初始化协调服务
     * 在应用启动时调用
     */
    public void initialize() {
        if (redisTemplate == null) {
            log.warn("Redis不可用，跳过协调服务初始化");
            return;
        }
        try {
            // 注册当前实例
            registerAsMaster(instanceId);
            // 设置初始状态
            setMasterStatus(STATUS_NORMAL);
            log.info("数据源协调服务初始化完成，实例ID: {}", instanceId);
        } catch (Exception e) {
            log.error("数据源协调服务初始化失败: {}", e.getMessage());
        }
    }

    /**
     * 健康检查
     * @return true-Redis可用
     */
    public boolean isHealthy() {
        if (redisTemplate == null) {
            redisAvailable = false;
            return false;
        }
        try {
            String result = redisTemplate.execute((RedisConnection connection) -> {
                return new String(connection.ping());
            });
            redisAvailable = "PONG".equals(result);
            return redisAvailable;
        } catch (Exception e) {
            redisAvailable = false;
            return false;
        }
    }

    /**
     * 获取协调状态信息
     */
    public CoordinationStatus getCoordinationStatus() {
        CoordinationStatus status = new CoordinationStatus();
        status.setInstanceId(instanceId);
        status.setMasterInstanceId(redisTemplate != null ? getMasterInstanceId() : null);
        status.setMasterStatus(redisTemplate != null ? getMasterStatus() : STATUS_NORMAL);
        status.setRedisHealthy(redisTemplate != null && isHealthy());
        return status;
    }

    /**
     * 协调状态信息
     */
    public static class CoordinationStatus {
        private String instanceId;
        private String masterInstanceId;
        private String masterStatus;
        private boolean redisHealthy;

        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String v) { this.instanceId = v; }
        public String getMasterInstanceId() { return masterInstanceId; }
        public void setMasterInstanceId(String v) { this.masterInstanceId = v; }
        public String getMasterStatus() { return masterStatus; }
        public void setMasterStatus(String v) { this.masterStatus = v; }
        public boolean isRedisHealthy() { return redisHealthy; }
        public void setRedisHealthy(boolean v) { this.redisHealthy = v; }
    }
}
