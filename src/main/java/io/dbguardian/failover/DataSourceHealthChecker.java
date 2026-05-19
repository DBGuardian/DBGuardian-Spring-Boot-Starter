package io.dbguardian.failover;

import io.dbguardian.loadbalance.DataSourceWrapper;
import io.dbguardian.registry.DataSourceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据源心跳检测器
 * 定期检测所有数据源的可用性，触发故障转移
 */
@Slf4j
@Component
public class DataSourceHealthChecker {

    @Autowired
    private DataSourceRegistry registry;

    @Autowired
    private FailoverController failoverController;

    // 健康检查线程池
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    // 配置参数
    @Value("${spring.datasource.dbguardian.failover.check-interval-seconds:30}")
    private int masterCheckIntervalSeconds = 30;

    @Value("${spring.datasource.dbguardian.failover.slave-check-interval-seconds:5}")
    private int slaveCheckIntervalSeconds = 5;

    @Value("${spring.datasource.dbguardian.failover.enabled:true}")
    private boolean failoverEnabled = true;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 启动所有数据源的心跳检测
     */
    @PostConstruct
    public void startHealthChecks() {
        if (!failoverEnabled) {
            log.info("故障转移已禁用，跳过健康检查");
            return;
        }

        running.set(true);

        // 主库健康检查
        executor.scheduleAtFixedRate(
                this::checkMasters,
                0,
                masterCheckIntervalSeconds,
                TimeUnit.SECONDS
        );

        // 从库健康检查
        executor.scheduleAtFixedRate(
                this::checkSlaves,
                0,
                slaveCheckIntervalSeconds,
                TimeUnit.SECONDS
        );

        log.info("数据源健康检查已启动: 主库间隔={}s, 从库间隔={}s",
                masterCheckIntervalSeconds, slaveCheckIntervalSeconds);
    }

    /**
     * 停止健康检查
     */
    @PreDestroy
    public void stopHealthChecks() {
        running.set(false);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("数据源健康检查已停止");
    }

    /**
     * 检测主库健康状态
     */
    private void checkMasters() {
        if (!running.get()) return;

        for (DataSourceWrapper master : registry.getAllMasters()) {
            boolean healthy = isHealthy(master);
            boolean wasAvailable = registry.isMasterAvailable(master.getId());

            if (healthy) {
                master.healthCheckSuccess();
                if (!wasAvailable) {
                    // 主库恢复
                    log.info("主库 {} 恢复", master.getId());
                    registry.updateMasterAvailability(master.getId(), true);
                }
            } else {
                master.healthCheckFailed();
                if (wasAvailable) {
                    // 主库故障
                    log.error("主库 {} 不可用，连续失败次数={}", master.getId(), master.getConsecutiveFailures());
                    registry.updateMasterAvailability(master.getId(), false);
                    
                    // 触发故障转移
                    failoverController.handleMasterFailure(master.getId());
                }
            }
        }
    }

    /**
     * 检测从库健康状态
     */
    private void checkSlaves() {
        if (!running.get()) return;

        for (DataSourceWrapper slave : registry.getAllSlaves()) {
            boolean healthy = isHealthy(slave);
            boolean wasAvailable = registry.isSlaveAvailable(slave.getId());

            if (healthy) {
                slave.healthCheckSuccess();
                if (!wasAvailable) {
                    // 从库恢复
                    log.info("从库 {} 恢复", slave.getId());
                    registry.updateSlaveAvailability(slave.getId(), true);
                    failoverController.handleSlaveRecovery(slave.getId());
                }
            } else {
                slave.healthCheckFailed();
                if (wasAvailable) {
                    // 从库故障
                    log.warn("从库 {} 不可用，已从负载均衡池移除", slave.getId());
                    registry.updateSlaveAvailability(slave.getId(), false);
                    failoverController.handleSlaveFailure(slave.getId());
                }
            }
        }
    }

    /**
     * 健康检测
     */
    private boolean isHealthy(DataSourceWrapper ds) {
        long start = System.currentTimeMillis();
        try (Connection conn = ds.getDataSource().getConnection()) {
            boolean valid = conn.isValid(5);
            if (valid) {
                long responseTime = System.currentTimeMillis() - start;
                ds.updateResponseTime(responseTime);
            }
            return valid;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 手动触发一次健康检查
     */
    public void triggerHealthCheck() {
        checkMasters();
        checkSlaves();
    }

    /**
     * 获取健康检查状态
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("running", running.get());
        status.put("masters", registry.getAllMasters().stream()
                .map(m -> {
                    Map<String, Object> info = new java.util.HashMap<>();
                    info.put("id", m.getId());
                    info.put("available", m.isAvailable());
                    info.put("healthy", isHealthy(m));
                    info.put("consecutiveFailures", m.getConsecutiveFailures());
                    info.put("avgResponseTime", m.getAvgResponseTime());
                    return info;
                })
                .collect(java.util.stream.Collectors.toList()));
        status.put("slaves", registry.getAllSlaves().stream()
                .map(s -> {
                    Map<String, Object> info = new java.util.HashMap<>();
                    info.put("id", s.getId());
                    info.put("available", s.isAvailable());
                    info.put("healthy", isHealthy(s));
                    info.put("consecutiveFailures", s.getConsecutiveFailures());
                    info.put("avgResponseTime", s.getAvgResponseTime());
                    return info;
                })
                .collect(java.util.stream.Collectors.toList()));
        return status;
    }
}
