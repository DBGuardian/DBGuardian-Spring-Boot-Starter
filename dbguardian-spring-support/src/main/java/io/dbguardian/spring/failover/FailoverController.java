package io.dbguardian.spring.failover;

import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.core.registry.DataSourceRegistry;
import io.dbguardian.runtime.ClusterRuntimeState;
import io.dbguardian.runtime.ClusterStatus;
import io.dbguardian.spring.coordination.DatasourceCoordinationService;
import io.dbguardian.spring.runtime.ClusterRuntimeStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class FailoverController {

    private static final Logger log = LoggerFactory.getLogger(FailoverController.class);

    private final DataSourceRegistry registry;
    private final DatasourceCoordinationService coordinationService;
    private final AtomicBoolean failoverInProgress = new AtomicBoolean(false);
    private final List<FailoverListener> listeners = new CopyOnWriteArrayList<FailoverListener>();

    private ClusterRuntimeStateManager runtimeStateManager;
    private volatile String currentMasterId;
    private boolean demoteOnFailover = true;
    private String replicationUser;
    private String replicationPassword;
    private int catchupTimeoutSeconds = 300;
    private int catchupCheckIntervalSeconds = 2;

    public FailoverController(DataSourceRegistry registry, DatasourceCoordinationService coordinationService) {
        this.registry = registry;
        this.coordinationService = coordinationService;
    }

    public synchronized void handleMasterFailure(String failedMasterId) {
        if (!failoverInProgress.compareAndSet(false, true)) {
            return;
        }
        boolean coordinationLockHeld = false;
        try {
            if (coordinationService != null && coordinationService.isHealthy()) {
                coordinationLockHeld = coordinationService.tryAcquireFailoverLock(60);
                if (!coordinationLockHeld) {
                    log.warn("故障转移锁未获取，跳过本次升主执行: failedMasterId={}", failedMasterId);
                    return;
                }
            } else if (coordinationService != null) {
                log.warn("协调服务不可用，降级为本地故障转移执行: failedMasterId={}", failedMasterId);
            }
            try {
                List<DataSourceWrapper> availableSlaves = registry.getAvailableSlaves().stream()
                        .filter(wrapper -> !wrapper.getId().equals(failedMasterId))
                        .collect(Collectors.toList());
                if (availableSlaves.isEmpty()) {
                    if (runtimeStateManager != null) {
                        runtimeStateManager.markDegraded("no_available_slave_for_failover");
                    }
                    notifyFailoverFailed(failedMasterId, "无可用从库可提升为主库");
                    return;
                }
                DataSourceWrapper newMaster = availableSlaves.stream()
                        .max(Comparator.comparingInt(DataSourceWrapper::getPriority))
                        .orElse(availableSlaves.get(0));
                executeFailover(failedMasterId, newMaster);
            } finally {
                if (coordinationLockHeld) {
                    coordinationService.releaseFailoverLock();
                }
            }
        } finally {
            failoverInProgress.set(false);
        }
    }

    public synchronized void handleOriginalMasterRecovery(String recoveredMasterId) {
        if (runtimeStateManager == null) {
            return;
        }
        if (!ClusterStatus.SLAVE_PROMOTED.equals(runtimeStateManager.current().getStatus())) {
            return;
        }
        String pendingOriginalMasterId = runtimeStateManager.current().getPendingOriginalMasterId();
        if (pendingOriginalMasterId == null || !pendingOriginalMasterId.equals(recoveredMasterId)) {
            return;
        }
        String activeMasterId = runtimeStateManager.current().getActiveMasterId();
        runtimeStateManager.moveToRecoveryStage(
                ClusterStatus.RECOVERING_ORIGINAL_MASTER,
                recoveredMasterId,
                activeMasterId,
                "original_master_recovered"
        );
        if (coordinationService != null) {
            coordinationService.broadcastRecoveryStage(
                    ClusterStatus.RECOVERING_ORIGINAL_MASTER.name(),
                    recoveredMasterId,
                    activeMasterId,
                    "original_master_recovered"
            );
        }
        log.info("检测到原主恢复，已进入恢复编排入口: originalMaster={}, currentMaster={}", recoveredMasterId, activeMasterId);
    }

    public synchronized boolean recoverOriginalMasterAndRestoreReplication(String reason) {
        if (runtimeStateManager == null) {
            return false;
        }
        ClusterRuntimeState state = runtimeStateManager.current();
        String originalMasterId = resolveRecoverySource(state);
        String promotedMasterId = resolveRecoveryTarget(state);
        DataSourceWrapper originalMaster = resolveNodeById(originalMasterId);
        DataSourceWrapper promotedMaster = resolveNodeById(promotedMasterId);
        if (originalMaster == null || promotedMaster == null) {
            log.warn("恢复原主流程缺少节点: originalMaster={}, promotedMaster={}", originalMasterId, promotedMasterId);
            return false;
        }

        moveToRecoveryStage(ClusterStatus.RECOVERING_ORIGINAL_MASTER, originalMasterId, promotedMasterId, reason + "_recovery_started");
        recoverMasterAsSlave(originalMaster, promotedMaster);

        moveToRecoveryStage(ClusterStatus.CATCHING_UP_ORIGINAL_MASTER, originalMasterId, promotedMasterId, reason + "_catchup_started");
        waitForCatchupOnMaster(originalMaster);

        moveToRecoveryStage(ClusterStatus.RESTORING_ORIGINAL_MASTER, originalMasterId, promotedMasterId, reason + "_original_master_restoring");
        restoreOriginalMasterRole(originalMaster);

        moveToRecoveryStage(ClusterStatus.RESTORING_REPLICATION, originalMasterId, promotedMasterId, reason + "_replication_restoring");
        restoreOriginalReplication(originalMaster, promotedMaster);

        currentMasterId = originalMasterId;
        runtimeStateManager.markMasterActive(originalMasterId, promotedMasterId, reason + "_recovery_completed");
        if (coordinationService != null) {
            coordinationService.setMasterStatus(DatasourceCoordinationService.STATUS_NORMAL);
        }
        logReplicationStatus(promotedMaster, "原主恢复后从库");
        log.info("原主恢复闭环完成: originalMaster={}, slave={}", originalMasterId, promotedMasterId);
        return true;
    }

    private void executeFailover(String failedMasterId, DataSourceWrapper newMaster) {
        String oldMasterId = currentMasterId == null ? failedMasterId : currentMasterId;
        try {
            promoteToMaster(newMaster);
            notifySlavesNewMaster(newMaster);
            currentMasterId = newMaster.getId();
            if (demoteOnFailover) {
                demoteToSlave(failedMasterId);
            }
            if (runtimeStateManager != null) {
                runtimeStateManager.markSlavePromoted(newMaster.getId(), failedMasterId, "master_failure_detected");
            }
            if (coordinationService != null) {
                coordinationService.broadcastFailoverEvent(oldMasterId, newMaster.getId());
            }
            notifyFailoverCompleted(oldMasterId, newMaster.getId());
            log.warn("故障转移完成，已从从库提升新主: oldMaster={}, newMaster={}", oldMasterId, newMaster.getId());
        } catch (Exception ex) {
            log.error("故障转移执行失败: {}", ex.getMessage(), ex);
            if (runtimeStateManager != null) {
                runtimeStateManager.markDegraded("failover_execution_failed");
            }
            notifyFailoverFailed(failedMasterId, ex.getMessage());
        }
    }

    private void notifySlavesNewMaster(DataSourceWrapper newMaster) {
        for (DataSourceWrapper slave : registry.getAllSlaves()) {
            try {
                if (!slave.getId().equals(newMaster.getId())) {
                    reconfigureSlave(slave, newMaster);
                }
            } catch (Exception ex) {
                log.error("重新配置从库 {} 失败: {}", slave.getId(), ex.getMessage());
            }
        }
    }

    private void reconfigureSlave(DataSourceWrapper slave, DataSourceWrapper newMaster) throws SQLException {
        try (Connection conn = slave.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
            stopSlaveReplication(stmt);
            if (replicationUser != null && !replicationUser.isEmpty()) {
                String masterHost = extractHost(newMaster.getUrl());
                int masterPort = extractPort(newMaster.getUrl());
                String sql = String.format(
                        "CHANGE MASTER TO MASTER_HOST='%s', MASTER_PORT=%d, MASTER_USER='%s', MASTER_PASSWORD='%s', MASTER_AUTO_POSITION=1",
                        masterHost, masterPort, replicationUser, replicationPassword);
                stmt.execute(sql);
                stmt.execute("START SLAVE");
            }
            try {
                stmt.execute("SET GLOBAL READ_ONLY=ON");
                stmt.execute("SET GLOBAL SUPER_READ_ONLY=ON");
            } catch (SQLException ignored) {
            }
        }
    }

    private void promoteToMaster(DataSourceWrapper newMaster) throws SQLException {
        try (Connection conn = newMaster.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
            stopSlaveReplication(stmt);
            stmt.execute("SET GLOBAL READ_ONLY=OFF");
            stmt.execute("SET GLOBAL SUPER_READ_ONLY=OFF");
        }
    }

    private void demoteToSlave(String demotedMasterId) {
        DataSourceWrapper demoted = registry.getMaster(demotedMasterId);
        DataSourceWrapper currentMaster = resolveCurrentMaster();
        if (demoted == null || currentMaster == null) {
            return;
        }
        try (Connection conn = demoted.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET GLOBAL READ_ONLY=ON");
            stmt.execute("SET GLOBAL SUPER_READ_ONLY=ON");
            if (replicationUser != null && !replicationUser.isEmpty()) {
                String sql = String.format(
                        "CHANGE MASTER TO MASTER_HOST='%s', MASTER_PORT=%d, MASTER_USER='%s', MASTER_PASSWORD='%s', MASTER_AUTO_POSITION=1",
                        extractHost(currentMaster.getUrl()), extractPort(currentMaster.getUrl()), replicationUser, replicationPassword);
                stopSlaveReplication(stmt);
                stmt.execute(sql);
                stmt.execute("START SLAVE");
            }
        } catch (SQLException ex) {
            log.error("降级节点 {} 失败: {}", demotedMasterId, ex.getMessage());
        }
    }

    private void recoverMasterAsSlave(DataSourceWrapper originalMaster, DataSourceWrapper promotedMaster) {
        if (isBlank(replicationUser) || isBlank(replicationPassword)) {
            log.info("未配置复制用户，跳过原主恢复为从库");
            return;
        }
        try (Connection connection = originalMaster.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            stopSlaveReplication(statement);
            enableReadOnly(originalMaster, "原主恢复阶段已启用只读模式");
            String sql = String.format(
                    "CHANGE MASTER TO MASTER_HOST='%s', MASTER_PORT=%d, MASTER_USER='%s', MASTER_PASSWORD='%s', MASTER_AUTO_POSITION=1",
                    extractHost(promotedMaster.getUrl()),
                    extractPort(promotedMaster.getUrl()),
                    replicationUser,
                    replicationPassword);
            statement.execute(sql);
            statement.execute("START SLAVE");
            log.info("原主已开始作为从库追赶临时主库: originalMaster={}, promotedMaster={}", originalMaster.getId(), promotedMaster.getId());
        } catch (SQLException ex) {
            log.warn("原主恢复为从库失败: {}", ex.getMessage());
        }
    }

    private void waitForCatchupOnMaster(DataSourceWrapper originalMaster) {
        int waited = 0;
        while (waited < catchupTimeoutSeconds) {
            try {
                Thread.sleep(catchupCheckIntervalSeconds * 1000L);
                waited += catchupCheckIntervalSeconds;
                try (Connection connection = originalMaster.getDataSource().getConnection();
                     Statement statement = connection.createStatement();
                     ResultSet rs = statement.executeQuery("SHOW SLAVE STATUS")) {
                    if (!rs.next()) {
                        continue;
                    }
                    String ioRunning = rs.getString("Slave_IO_Running");
                    String sqlRunning = rs.getString("Slave_SQL_Running");
                    String lag = rs.getString("Seconds_Behind_Master");
                    boolean caughtUp = "Yes".equalsIgnoreCase(ioRunning)
                            && "Yes".equalsIgnoreCase(sqlRunning)
                            && ("0".equals(lag) || lag == null || "NULL".equalsIgnoreCase(lag));
                    if (caughtUp) {
                        log.info("原主追赶完成: nodeId={}", originalMaster.getId());
                        return;
                    }
                    log.info("原主追赶中: nodeId={}, lag={}s", originalMaster.getId(), lag == null ? "UNKNOWN" : lag);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("原主追赶等待被中断: {}", ex.getMessage());
                return;
            } catch (Exception ex) {
                log.debug("检查原主追赶状态失败: {}", ex.getMessage());
            }
        }
        log.warn("原主追赶超时，继续执行后续恢复流程");
    }

    private void restoreOriginalMasterRole(DataSourceWrapper originalMaster) {
        disableReadOnly(originalMaster, "原主已恢复可写");
        try (Connection connection = originalMaster.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            stopSlaveReplication(statement);
        } catch (SQLException ex) {
            log.debug("恢复原主主角色时停止复制失败: {}", ex.getMessage());
        }
    }

    private void restoreOriginalReplication(DataSourceWrapper originalMaster, DataSourceWrapper promotedMaster) {
        enableReadOnly(promotedMaster, "临时主已切回从库只读模式");
        try {
            reconfigureSlave(promotedMaster, originalMaster);
        } catch (SQLException ex) {
            log.warn("原从库重新挂回原主失败: {}", ex.getMessage());
        }
    }

    public void handleSlaveFailure(String failedSlaveId) {
        registry.updateSlaveAvailability(failedSlaveId, false);
        if (runtimeStateManager != null) {
            runtimeStateManager.updateSlaveStatus(failedSlaveId, false, false, 0L, "slave_health_check_failed");
        }
        if (coordinationService != null) {
            coordinationService.broadcastSlaveStatus(failedSlaveId, false);
        }
        for (FailoverListener listener : listeners) {
            listener.onSlaveDown(failedSlaveId);
        }
    }

    public void handleSlaveRecovery(String recoveredSlaveId) {
        registry.updateSlaveAvailability(recoveredSlaveId, true);
        if (runtimeStateManager != null) {
            runtimeStateManager.updateSlaveStatus(recoveredSlaveId, true, true, 0L, "slave_health_check_recovered");
        }
        if (coordinationService != null) {
            coordinationService.broadcastSlaveStatus(recoveredSlaveId, true);
        }
        for (FailoverListener listener : listeners) {
            listener.onSlaveUp(recoveredSlaveId);
        }
    }

    public void addListener(FailoverListener listener) {
        listeners.add(listener);
    }

    public void setCurrentMasterId(String currentMasterId) {
        this.currentMasterId = currentMasterId;
    }

    public void setDemoteOnFailover(boolean demoteOnFailover) {
        this.demoteOnFailover = demoteOnFailover;
    }

    public void setReplicationUser(String replicationUser) {
        this.replicationUser = replicationUser;
    }

    public void setReplicationPassword(String replicationPassword) {
        this.replicationPassword = replicationPassword;
    }

    public void setCatchupTimeoutSeconds(int catchupTimeoutSeconds) {
        this.catchupTimeoutSeconds = catchupTimeoutSeconds;
    }

    public void setCatchupCheckIntervalSeconds(int catchupCheckIntervalSeconds) {
        this.catchupCheckIntervalSeconds = catchupCheckIntervalSeconds;
    }

    public void setRuntimeStateManager(ClusterRuntimeStateManager runtimeStateManager) {
        this.runtimeStateManager = runtimeStateManager;
    }

    private void notifyFailoverCompleted(String oldMasterId, String newMasterId) {
        for (FailoverListener listener : listeners) {
            listener.onFailoverCompleted(oldMasterId, newMasterId);
        }
    }

    private void notifyFailoverFailed(String failedMasterId, String reason) {
        for (FailoverListener listener : listeners) {
            listener.onFailoverFailed(failedMasterId, reason);
        }
    }

    private DataSourceWrapper resolveCurrentMaster() {
        DataSourceWrapper master = registry.getMaster(currentMasterId);
        if (master != null) {
            return master;
        }
        return registry.getSlave(currentMasterId);
    }

    private DataSourceWrapper resolveNodeById(String nodeId) {
        if (nodeId == null) {
            return null;
        }
        DataSourceWrapper master = registry.getMaster(nodeId);
        if (master != null) {
            return master;
        }
        return registry.getSlave(nodeId);
    }

    private String resolveRecoverySource(ClusterRuntimeState state) {
        if (state == null) {
            return null;
        }
        if (state.getPendingOriginalMasterId() != null) {
            return state.getPendingOriginalMasterId();
        }
        if (state.getRecoverySourceNodeId() != null) {
            return state.getRecoverySourceNodeId();
        }
        return state.getActiveMasterId();
    }

    private String resolveRecoveryTarget(ClusterRuntimeState state) {
        if (state == null) {
            return null;
        }
        if (state.getRecoveryTargetNodeId() != null) {
            return state.getRecoveryTargetNodeId();
        }
        return state.getActiveMasterId();
    }

    private void moveToRecoveryStage(ClusterStatus status, String sourceNodeId, String targetNodeId, String reason) {
        if (runtimeStateManager != null) {
            runtimeStateManager.moveToRecoveryStage(status, sourceNodeId, targetNodeId, reason);
        }
        if (coordinationService != null) {
            coordinationService.broadcastRecoveryStage(status.name(), sourceNodeId, targetNodeId, reason);
        }
    }

    private void logReplicationStatus(DataSourceWrapper slave, String scope) {
        if (slave == null || runtimeStateManager == null) {
            return;
        }
        try (Connection connection = slave.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SHOW SLAVE STATUS")) {
            if (!rs.next()) {
                return;
            }
            log.info("复制状态 - IO: {}, SQL: {}", rs.getString("Slave_IO_Running"), rs.getString("Slave_SQL_Running"));
            long lag = parseLag(rs.getString("Seconds_Behind_Master"));
            runtimeStateManager.updateSlaveStatus(slave.getId(), true,
                    "Yes".equalsIgnoreCase(rs.getString("Slave_IO_Running"))
                            && "Yes".equalsIgnoreCase(rs.getString("Slave_SQL_Running")),
                    lag,
                    scope + "_replication_observed");
        } catch (SQLException ex) {
            log.debug("读取复制状态失败: {}", ex.getMessage());
        }
    }

    private void disableReadOnly(DataSourceWrapper wrapper, String successLog) {
        if (wrapper == null) {
            return;
        }
        try (Connection connection = wrapper.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SET GLOBAL READ_ONLY=OFF");
            statement.execute("SET GLOBAL SUPER_READ_ONLY=OFF");
            log.info(successLog);
        } catch (SQLException ex) {
            log.debug("关闭只读模式失败: nodeId={}, reason={}", wrapper.getId(), ex.getMessage());
        }
    }

    private void enableReadOnly(DataSourceWrapper wrapper, String successLog) {
        if (wrapper == null) {
            return;
        }
        try (Connection connection = wrapper.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SET GLOBAL READ_ONLY=ON");
            statement.execute("SET GLOBAL SUPER_READ_ONLY=ON");
            log.info(successLog);
        } catch (SQLException ex) {
            log.debug("开启只读模式失败: nodeId={}, reason={}", wrapper.getId(), ex.getMessage());
        }
    }

    private void stopSlaveReplication(Statement stmt) {
        try {
            stmt.execute("STOP SLAVE");
            stmt.execute("RESET SLAVE ALL");
        } catch (SQLException ignored) {
        }
    }

    private long parseLag(String lag) {
        if (lag == null || lag.trim().isEmpty() || "NULL".equalsIgnoreCase(lag)) {
            return 0L;
        }
        try {
            return Long.parseLong(lag);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String extractHost(String jdbcUrl) {
        String body = jdbcUrl.replace("jdbc:mysql://", "");
        int slash = body.indexOf('/');
        String hostPort = slash >= 0 ? body.substring(0, slash) : body;
        int colon = hostPort.indexOf(':');
        return colon >= 0 ? hostPort.substring(0, colon) : hostPort;
    }

    private int extractPort(String jdbcUrl) {
        String body = jdbcUrl.replace("jdbc:mysql://", "");
        int slash = body.indexOf('/');
        String hostPort = slash >= 0 ? body.substring(0, slash) : body;
        int colon = hostPort.indexOf(':');
        if (colon < 0) {
            return 3306;
        }
        return Integer.parseInt(hostPort.substring(colon + 1));
    }

    public interface FailoverListener {
        void onFailoverCompleted(String oldMasterId, String newMasterId);

        void onFailoverFailed(String failedMasterId, String reason);

        void onSlaveDown(String slaveId);

        void onSlaveUp(String slaveId);
    }
}