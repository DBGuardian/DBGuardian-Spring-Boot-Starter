package io.dbguardian.spring.failover;

import io.dbguardian.core.GtidConsistencyInspector;
import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.core.registry.DataSourceRegistry;
import io.dbguardian.runtime.ClusterRuntimeState;
import io.dbguardian.runtime.ClusterStatus;
import io.dbguardian.spring.runtime.ClusterRuntimeStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DataSourceHealthChecker {

    private static final Logger log = LoggerFactory.getLogger(DataSourceHealthChecker.class);

    private final DataSourceRegistry registry;
    private final FailoverController failoverController;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ClusterRuntimeStateManager runtimeStateManager;
    private GtidConsistencyInspector gtidConsistencyInspector;
    private int masterCheckIntervalSeconds = 30;
    private int slaveCheckIntervalSeconds = 5;
    private boolean failoverEnabled = true;
    private boolean gtidProtectionEnabled;
    private boolean blockSlaveReadsOnRisk = true;

    public DataSourceHealthChecker(DataSourceRegistry registry, FailoverController failoverController) {
        this.registry = registry;
        this.failoverController = failoverController;
    }

    @PostConstruct
    public void startHealthChecks() {
        if (!failoverEnabled) {
            log.info("故障转移已禁用，跳过健康检查");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executor.scheduleAtFixedRate(this::checkMasters, 0, masterCheckIntervalSeconds, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(this::checkSlaves, 0, slaveCheckIntervalSeconds, TimeUnit.SECONDS);
        log.info("数据源健康检查已启动: 主库间隔={}s, 从库间隔={}s", masterCheckIntervalSeconds, slaveCheckIntervalSeconds);
    }

    @PreDestroy
    public void stopHealthChecks() {
        running.set(false);
        executor.shutdown();
        log.info("数据源健康检查已停止");
    }

    private void checkMasters() {
        if (!running.get()) {
            return;
        }
        for (DataSourceWrapper master : registry.getAllMasters()) {
            boolean healthy = isHealthy(master);
            boolean wasAvailable = registry.getAvailableMasters().stream().anyMatch(item -> item.getId().equals(master.getId()));
            if (healthy) {
                master.healthCheckSuccess();
                handleMasterHealthy(master, wasAvailable);
            } else {
                master.healthCheckFailed();
                if (wasAvailable) {
                    registry.updateMasterAvailability(master.getId(), false);
                    if (runtimeStateManager != null) {
                        runtimeStateManager.markDegraded("master_health_check_failed");
                    }
                    log.error("主库 {} 不可用，连续失败次数={}", master.getId(), master.getConsecutiveFailures());
                    failoverController.handleMasterFailure(master.getId());
                }
            }
        }
    }

    private void handleMasterHealthy(DataSourceWrapper master, boolean wasAvailable) {
        ClusterRuntimeState state = runtimeStateManager == null ? null : runtimeStateManager.current();
        if (!wasAvailable) {
            registry.updateMasterAvailability(master.getId(), true);
            log.info("主库 {} 恢复", master.getId());
        }
        if (state == null) {
            return;
        }
        if (shouldResumeOriginalMasterRecovery(state, master.getId())) {
            failoverController.handleOriginalMasterRecovery(master.getId());
            return;
        }
        if (state.getActiveMasterId() == null && !ClusterStatus.SLAVE_PROMOTED.equals(state.getStatus())) {
            runtimeStateManager.markMasterActive(master.getId(), state.getActiveReadNodeId(), "master_health_check_ok");
        }
    }

    private void checkSlaves() {
        if (!running.get()) {
            return;
        }
        for (DataSourceWrapper slave : registry.getAllSlaves()) {
            boolean promotedActiveMaster = isPromotedActiveMaster(slave.getId());
            boolean healthy = isHealthy(slave);
            boolean replicationHealthy = promotedActiveMaster || checkReplicationHealthy(slave);
            long lag = promotedActiveMaster ? 0L : getReplicationLagSeconds(slave);
            boolean wasAvailable = registry.getAvailableSlaves().stream().anyMatch(item -> item.getId().equals(slave.getId()));
            boolean promotionCandidate = shouldKeepSlaveAvailableForPromotion(healthy, replicationHealthy);
            if (healthy && (replicationHealthy || promotionCandidate)) {
                slave.healthCheckSuccess();
                if (runtimeStateManager != null) {
                    runtimeStateManager.updateSlaveStatus(slave.getId(), true, replicationHealthy, lag,
                            promotedActiveMaster ? "slave_promoted_master_active"
                                    : promotionCandidate ? "slave_promotion_candidate"
                                    : "slave_health_check_ok");
                }
                applyGtidProtection();
                if (!wasAvailable) {
                    registry.updateSlaveAvailability(slave.getId(), true);
                    log.info("从库 {} 恢复", slave.getId());
                    failoverController.handleSlaveRecovery(slave.getId());
                }
            } else {
                slave.healthCheckFailed();
                if (runtimeStateManager != null) {
                    runtimeStateManager.updateSlaveStatus(slave.getId(), healthy, replicationHealthy, lag, "slave_health_check_failed");
                }
                applyGtidProtection();
                if (wasAvailable) {
                    registry.updateSlaveAvailability(slave.getId(), false);
                    log.warn("从库 {} 不可用，已从负载池移除", slave.getId());
                    failoverController.handleSlaveFailure(slave.getId());
                }
            }
        }
    }

    private void applyGtidProtection() {
        if (!gtidProtectionEnabled || runtimeStateManager == null || gtidConsistencyInspector == null) {
            return;
        }
        if (!blockSlaveReadsOnRisk) {
            return;
        }
        String blockedSlaveId = gtidConsistencyInspector.findBlockedSlaveId(runtimeStateManager.current());
        if (blockedSlaveId != null) {
            runtimeStateManager.markContamination(
                    blockedSlaveId,
                    true,
                    true,
                    "gtid_risk_detected",
                    "gtid_protection_blocked_slave_reads",
                    "gtid_protection_blocked_slave_reads"
            );
            log.warn("GTID 保护已启用，暂停从库读流量: nodeId={}", blockedSlaveId);
        }
    }

    private boolean shouldResumeOriginalMasterRecovery(ClusterRuntimeState state, String recoveredMasterId) {
        if (state == null || recoveredMasterId == null) {
            return false;
        }
        if (state.getPendingOriginalMasterId() == null || !recoveredMasterId.equals(state.getPendingOriginalMasterId())) {
            return false;
        }
        return ClusterStatus.SLAVE_PROMOTED.equals(state.getStatus())
                || ClusterStatus.RECOVERING_ORIGINAL_MASTER.equals(state.getStatus())
                || ClusterStatus.CATCHING_UP_ORIGINAL_MASTER.equals(state.getStatus())
                || ClusterStatus.RESTORING_ORIGINAL_MASTER.equals(state.getStatus())
                || ClusterStatus.RESTORING_REPLICATION.equals(state.getStatus());
    }

    private boolean shouldKeepSlaveAvailableForPromotion(boolean healthy, boolean replicationHealthy) {
        if (!healthy || replicationHealthy || runtimeStateManager == null) {
            return false;
        }
        ClusterRuntimeState state = runtimeStateManager.current();
        if (state == null) {
            return false;
        }
        return state.getActiveMasterId() == null
                || ClusterStatus.DEGRADED.equals(state.getStatus())
                || registry.getAvailableMasterCount() == 0;
    }

    private boolean isPromotedActiveMaster(String slaveNodeId) {
        if (runtimeStateManager == null || slaveNodeId == null) {
            return false;
        }
        ClusterRuntimeState state = runtimeStateManager.current();
        if (state == null || state.getActiveMasterId() == null) {
            return false;
        }
        return slaveNodeId.equals(state.getActiveMasterId())
                && ClusterStatus.SLAVE_PROMOTED.equals(state.getStatus());
    }

    private boolean isHealthy(DataSourceWrapper dataSourceWrapper) {
        long start = System.currentTimeMillis();
        try (Connection connection = dataSourceWrapper.getDataSource().getConnection()) {
            boolean valid = connection.isValid(5);
            if (valid) {
                dataSourceWrapper.updateResponseTime(System.currentTimeMillis() - start);
            }
            return valid;
        } catch (SQLException ex) {
            return false;
        }
    }

    private boolean checkReplicationHealthy(DataSourceWrapper slave) {
        try (Connection connection = slave.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SHOW SLAVE STATUS")) {
            if (!rs.next()) {
                return true;
            }
            String ioRunning = rs.getString("Slave_IO_Running");
            String sqlRunning = rs.getString("Slave_SQL_Running");
            log.info("复制状态 - IO: {}, SQL: {}", ioRunning, sqlRunning);
            return "Yes".equalsIgnoreCase(ioRunning)
                    && "Yes".equalsIgnoreCase(sqlRunning);
        } catch (SQLException ex) {
            return false;
        }
    }

    private long getReplicationLagSeconds(DataSourceWrapper slave) {
        try (Connection connection = slave.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SHOW SLAVE STATUS")) {
            if (!rs.next()) {
                return 0L;
            }
            String lag = rs.getString("Seconds_Behind_Master");
            if (lag == null || "NULL".equalsIgnoreCase(lag)) {
                return 0L;
            }
            return Long.parseLong(lag);
        } catch (Exception ex) {
            return 0L;
        }
    }

    public void triggerHealthCheck() {
        checkMasters();
        checkSlaves();
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<String, Object>();
        status.put("running", Boolean.valueOf(running.get()));
        status.put("gtidProtectionEnabled", Boolean.valueOf(gtidProtectionEnabled));
        status.put("masters", registry.getAllMasters().stream().map(master -> {
            Map<String, Object> info = new HashMap<String, Object>();
            info.put("id", master.getId());
            info.put("available", Boolean.valueOf(master.isAvailable()));
            info.put("consecutiveFailures", Integer.valueOf(master.getConsecutiveFailures()));
            info.put("avgResponseTime", Long.valueOf(master.getAvgResponseTime()));
            return info;
        }).collect(Collectors.toList()));
        status.put("slaves", registry.getAllSlaves().stream().map(slave -> {
            Map<String, Object> info = new HashMap<String, Object>();
            info.put("id", slave.getId());
            info.put("available", Boolean.valueOf(slave.isAvailable()));
            info.put("consecutiveFailures", Integer.valueOf(slave.getConsecutiveFailures()));
            info.put("avgResponseTime", Long.valueOf(slave.getAvgResponseTime()));
            info.put("replicationLagSeconds", Long.valueOf(getReplicationLagSeconds(slave)));
            return info;
        }).collect(Collectors.toList()));
        return status;
    }

    public void setRuntimeStateManager(ClusterRuntimeStateManager runtimeStateManager) {
        this.runtimeStateManager = runtimeStateManager;
    }

    public void setGtidConsistencyInspector(GtidConsistencyInspector gtidConsistencyInspector) {
        this.gtidConsistencyInspector = gtidConsistencyInspector;
    }

    public void setMasterCheckIntervalSeconds(int masterCheckIntervalSeconds) {
        this.masterCheckIntervalSeconds = masterCheckIntervalSeconds;
    }

    public void setSlaveCheckIntervalSeconds(int slaveCheckIntervalSeconds) {
        this.slaveCheckIntervalSeconds = slaveCheckIntervalSeconds;
    }

    public void setFailoverEnabled(boolean failoverEnabled) {
        this.failoverEnabled = failoverEnabled;
    }

    public void setGtidProtectionEnabled(boolean gtidProtectionEnabled) {
        this.gtidProtectionEnabled = gtidProtectionEnabled;
    }

    public void setBlockSlaveReadsOnRisk(boolean blockSlaveReadsOnRisk) {
        this.blockSlaveReadsOnRisk = blockSlaveReadsOnRisk;
    }
}