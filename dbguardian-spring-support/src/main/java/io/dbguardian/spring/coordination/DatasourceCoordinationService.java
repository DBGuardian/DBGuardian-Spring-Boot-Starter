package io.dbguardian.spring.coordination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DatasourceCoordinationService {

    private static final Logger log = LoggerFactory.getLogger(DatasourceCoordinationService.class);

    private static final String MASTER_STATUS_KEY = "dbguardian:datasource:master:status";
    private static final String MASTER_INSTANCE_KEY = "dbguardian:datasource:master:instance";
    private static final String FAILOVER_LOCK_KEY = "dbguardian:datasource:failover:lock";
    private static final String STATUS_CHANNEL = "dbguardian:datasource:status:channel";

    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_SLAVE_PROMOTED = "SLAVE_PROMOTED";

    private RedisTemplate<String, Object> redisTemplate;
    private String applicationName = "dbguardian";
    private volatile boolean redisAvailable = true;
    private final String instanceId = UUID.randomUUID().toString();

    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getMasterStatus() {
        if (redisTemplate == null) {
            return STATUS_NORMAL;
        }
        try {
            Object status = redisTemplate.opsForValue().get(MASTER_STATUS_KEY);
            return status == null ? STATUS_NORMAL : status.toString();
        } catch (Exception ex) {
            log.error("获取主库状态失败: {}", ex.getMessage());
            return STATUS_NORMAL;
        }
    }

    public void setMasterStatus(String status) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(MASTER_STATUS_KEY, status);
            log.info("更新主库状态到Redis: {}", status);
            broadcastStatusChange(status);
        } catch (Exception ex) {
            log.error("设置主库状态失败: {}", ex.getMessage());
        }
    }

    public void registerAsMaster(String masterInstanceId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(MASTER_INSTANCE_KEY, masterInstanceId);
            log.info("注册主库实例: {}", masterInstanceId);
        } catch (Exception ex) {
            log.error("注册主库实例失败: {}", ex.getMessage());
        }
    }

    public String getMasterInstanceId() {
        if (redisTemplate == null) {
            return null;
        }
        try {
            Object value = redisTemplate.opsForValue().get(MASTER_INSTANCE_KEY);
            return value == null ? null : value.toString();
        } catch (Exception ex) {
            log.error("获取主库实例ID失败: {}", ex.getMessage());
            return null;
        }
    }

    public boolean tryAcquireFailoverLock(long expireSeconds) {
        if (redisTemplate == null) {
            return false;
        }
        try {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(FAILOVER_LOCK_KEY, instanceId, expireSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(success);
        } catch (Exception ex) {
            log.error("获取故障转移锁失败: {}", ex.getMessage());
            return false;
        }
    }

    public void releaseFailoverLock() {
        if (redisTemplate == null) {
            return;
        }
        try {
            Object currentHolder = redisTemplate.opsForValue().get(FAILOVER_LOCK_KEY);
            if (instanceId.equals(currentHolder)) {
                redisTemplate.delete(FAILOVER_LOCK_KEY);
            }
        } catch (Exception ex) {
            log.error("释放故障转移锁失败: {}", ex.getMessage());
        }
    }

    public boolean shouldExecuteFailover() {
        try {
            String masterInstanceId = getMasterInstanceId();
            if (masterInstanceId == null) {
                return true;
            }
            return instanceId.equals(masterInstanceId);
        } catch (Exception ex) {
            log.error("检查故障转移执行权失败: {}", ex.getMessage());
            return true;
        }
    }

    private void broadcastStatusChange(String status) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.convertAndSend(STATUS_CHANNEL, status);
            log.info("广播状态变更: {}", status);
        } catch (Exception ex) {
            log.error("广播状态变更失败: {}", ex.getMessage());
        }
    }

    public void broadcastFailoverEvent(String oldMasterId, String newMasterId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String message = String.format("FAILOVER:%s:%s", oldMasterId, newMasterId);
            redisTemplate.convertAndSend(STATUS_CHANNEL, message);
        } catch (Exception ex) {
            log.error("广播故障转移事件失败: {}", ex.getMessage());
        }
    }

    public void broadcastRecoveryStage(String stage, String sourceNodeId, String targetNodeId, String reason) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String message = String.format(
                    "RECOVERY_STAGE:%s:%s:%s:%s",
                    nullToEmpty(stage),
                    nullToEmpty(sourceNodeId),
                    nullToEmpty(targetNodeId),
                    nullToEmpty(reason)
            );
            redisTemplate.convertAndSend(STATUS_CHANNEL, message);
        } catch (Exception ex) {
            log.error("广播恢复阶段失败: {}", ex.getMessage());
        }
    }

    public void broadcastSlaveStatus(String slaveId, boolean available) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String message = String.format("SLAVE_STATUS:%s:%s", slaveId, available);
            redisTemplate.convertAndSend(STATUS_CHANNEL, message);
        } catch (Exception ex) {
            log.error("广播从库状态变更失败: {}", ex.getMessage());
        }
    }

    public void initialize() {
        if (redisTemplate == null) {
            log.warn("Redis不可用，跳过协调服务初始化");
            redisAvailable = false;
            return;
        }
        try {
            registerAsMaster(instanceId);
            setMasterStatus(STATUS_NORMAL);
            redisAvailable = true;
            log.info("Redis 分布式协调服务已初始化: instanceId={}", instanceId);
        } catch (Exception ex) {
            redisAvailable = false;
            log.error("数据源协调服务初始化失败: {}", ex.getMessage());
        }
    }

    public boolean isHealthy() {
        if (redisTemplate == null) {
            redisAvailable = false;
            return false;
        }
        try {
            String result = redisTemplate.execute((RedisConnection connection) -> connection.ping());
            redisAvailable = "PONG".equalsIgnoreCase(result);
            return redisAvailable;
        } catch (Exception ex) {
            redisAvailable = false;
            return false;
        }
    }

    public CoordinationStatus getCoordinationStatus() {
        CoordinationStatus status = new CoordinationStatus();
        status.setInstanceId(instanceId);
        status.setMasterInstanceId(redisTemplate != null ? getMasterInstanceId() : null);
        status.setMasterStatus(redisTemplate != null ? getMasterStatus() : STATUS_NORMAL);
        status.setRedisHealthy(redisTemplate != null && isHealthy());
        return status;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static class CoordinationStatus {
        private String instanceId;
        private String masterInstanceId;
        private String masterStatus;
        private boolean redisHealthy;

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }

        public String getMasterInstanceId() {
            return masterInstanceId;
        }

        public void setMasterInstanceId(String masterInstanceId) {
            this.masterInstanceId = masterInstanceId;
        }

        public String getMasterStatus() {
            return masterStatus;
        }

        public void setMasterStatus(String masterStatus) {
            this.masterStatus = masterStatus;
        }

        public boolean isRedisHealthy() {
            return redisHealthy;
        }

        public void setRedisHealthy(boolean redisHealthy) {
            this.redisHealthy = redisHealthy;
        }
    }
}