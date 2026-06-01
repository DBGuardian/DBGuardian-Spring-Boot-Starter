package io.dbguardian.boot3.config;

import io.dbguardian.core.FailoverOrchestrator;
import io.dbguardian.core.ReplicationRecoveryCoordinator;
import io.dbguardian.core.TopologyRegistry;
import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.core.registry.DataSourceRegistry;
import io.dbguardian.runtime.ClusterRuntimeState;
import io.dbguardian.runtime.ClusterStatus;
import io.dbguardian.spring.coordination.DatasourceCoordinationService;
import io.dbguardian.spring.failover.DataSourceHealthChecker;
import io.dbguardian.spring.failover.FailoverController;
import io.dbguardian.spring.runtime.ClusterRuntimeStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DbGuardianRuntimeOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DbGuardianRuntimeOrchestrator.class);

    private final DbGuardianProperties dbGuardianProperties;
    private final SpringDataSourceProperties dataSourceProperties;
    private final TopologyRegistry topologyRegistry;
    private final FailoverOrchestrator failoverOrchestrator;
    private final DataSourceRegistry dataSourceRegistry;
    private final ClusterRuntimeStateManager runtimeStateManager;
    private final DataSourceHealthChecker dataSourceHealthChecker;
    private final FailoverController failoverController;
    private final ReplicationRecoveryCoordinator replicationRecoveryCoordinator;
    private final DatasourceCoordinationService coordinationService;

    public DbGuardianRuntimeOrchestrator(DbGuardianProperties dbGuardianProperties,
                                         SpringDataSourceProperties dataSourceProperties,
                                         TopologyRegistry topologyRegistry,
                                         FailoverOrchestrator failoverOrchestrator,
                                         DataSourceRegistry dataSourceRegistry,
                                         ClusterRuntimeStateManager runtimeStateManager,
                                         DataSourceHealthChecker dataSourceHealthChecker,
                                         FailoverController failoverController,
                                         ReplicationRecoveryCoordinator replicationRecoveryCoordinator,
                                         DatasourceCoordinationService coordinationService) {
        this.dbGuardianProperties = dbGuardianProperties;
        this.dataSourceProperties = dataSourceProperties;
        this.topologyRegistry = topologyRegistry;
        this.failoverOrchestrator = failoverOrchestrator;
        this.dataSourceRegistry = dataSourceRegistry;
        this.runtimeStateManager = runtimeStateManager;
        this.dataSourceHealthChecker = dataSourceHealthChecker;
        this.failoverController = failoverController;
        this.replicationRecoveryCoordinator = replicationRecoveryCoordinator;
        this.coordinationService = coordinationService;
    }

    @PostConstruct
    public void initializeOnStartup() {
        failoverOrchestrator.initialize(topologyRegistry.getClusterModel());
        runtimeStateManager.initialize(dbGuardianProperties.getClusterId(), resolveMasterNodeId(), resolveReadNodeId());
        refreshRuntimeState("startup_initialized");
        log.info("DBGuardian 启动期运行态已初始化: status={}", runtimeStateManager.current().getStatus());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=== 应用启动完成，重新检测数据源状态 ===");
        dataSourceHealthChecker.triggerHealthCheck();
        refreshRuntimeState("application_ready_recheck");
        ensureCoordinationInitializedAndSync();
        log.info("=== 最终数据源状态: {}, 主库: {}, 从库: {} ===",
                runtimeStateManager.current().getStatus(),
                isMasterAvailable() ? "可用" : "不可用",
                isSlaveAvailable() ? "可用" : "不可用");
        log.info("DBGuardian ready 后运行态已同步: status={}", runtimeStateManager.current().getStatus());
    }

    @Scheduled(fixedDelayString = "${spring.datasource.runtime.orchestration-interval-ms:30000}")
    public void runPeriodicOrchestration() {
        if (!dbGuardianProperties.isEnabled() || !dataSourceProperties.isEnabled() || !dataSourceProperties.getRuntime().isPeriodicHealthCheckEnabled()) {
            return;
        }
        dataSourceHealthChecker.triggerHealthCheck();
    }

    private void refreshRuntimeState(String reason) {
        ClusterRuntimeState currentState = runtimeStateManager.current();
        if (attemptOriginalMasterRecovery(reason, currentState)) {
            return;
        }

        boolean masterOk = isMasterAvailable();
        boolean slaveOk = isSlaveAvailable();
        if (masterOk && slaveOk) {
            determineAndConfigureMasterSlave();
            return;
        }
        if (masterOk) {
            runtimeStateManager.markMasterActive(resolveMasterNodeId(), resolveMasterNodeId(), reason + "_master_only");
            log.info("数据源状态已恢复正常: MASTER_ACTIVE（只有主库可用）");
            return;
        }
        if (slaveOk) {
            if (!ClusterStatus.SLAVE_PROMOTED.equals(currentState.getStatus())
                    && !currentState.isRecoveryInProgress()) {
                String failedMasterId = currentState.getActiveMasterId() != null
                        ? currentState.getActiveMasterId()
                        : resolveMasterNodeId();
                log.warn("检测到主库不可用且从库可用，直接触发故障转移控制器执行真实升主: reason={}, failedMasterId={}", reason, failedMasterId);
                failoverController.handleMasterFailure(failedMasterId);
                if (ClusterStatus.SLAVE_PROMOTED.equals(runtimeStateManager.current().getStatus())) {
                    log.warn("数据源状态已恢复: SLAVE_PROMOTED（从库升为主库）");
                }
                return;
            }
            runtimeStateManager.markSlavePromoted(resolveReadNodeId(), currentState.getPendingOriginalMasterId(), reason + "_slave_promoted");
            log.warn("数据源状态已恢复: SLAVE_PROMOTED（从库升为主库）");
            return;
        }
        if (dataSourceProperties.isAllowDegradedStartup()) {
            runtimeStateManager.markDegraded(reason + "_degraded");
            log.warn("数据源仍然不可用，保持 DEGRADED 模式");
            return;
        }
        throw new IllegalStateException("主库和从库都不可用，无法完成运行态判定");
    }

    private boolean attemptOriginalMasterRecovery(String reason, ClusterRuntimeState state) {
        if (!dataSourceProperties.getRecovery().isEnabled() || !dataSourceProperties.getRecovery().isAutoFailback()) {
            return false;
        }
        if (!replicationRecoveryCoordinator.shouldRecover(state)) {
            return false;
        }
        if (!replicationRecoveryCoordinator.shouldStartOriginalMasterRecovery(state)
                && !ClusterStatus.CATCHING_UP_ORIGINAL_MASTER.equals(state.getStatus())
                && !ClusterStatus.RESTORING_ORIGINAL_MASTER.equals(state.getStatus())
                && !ClusterStatus.RESTORING_REPLICATION.equals(state.getStatus())) {
            return false;
        }
        recoverOriginalMasterAndRestoreReplication(reason);
        return true;
    }

    private void recoverOriginalMasterAndRestoreReplication(String reason) {
        if (!failoverController.recoverOriginalMasterAndRestoreReplication(reason)) {
            log.warn("原主恢复闭环触发失败: reason={}", reason);
        }
    }

    private void determineAndConfigureMasterSlave() {
        DataSourceWrapper master = resolveConfiguredMaster();
        DataSourceWrapper slave = resolveConfiguredSlave();
        if (master == null || slave == null) {
            runtimeStateManager.markMasterActive(resolveMasterNodeId(), resolveReadNodeId(), "single_side_available_without_replication_chain");
            return;
        }

        String masterGtid = getGtidPosition(master);
        String slaveGtid = getGtidPosition(slave);
        log.info("主库 GTID: {}", emptyToUnknown(masterGtid));
        log.info("从库 GTID: {}", emptyToUnknown(slaveGtid));

        boolean masterPreferred = isNewerOrEqual(masterGtid, slaveGtid);
        if (masterPreferred) {
            log.info("主库数据更新，以主库为主库");
            disableReadOnly(master, "已关闭数据源只读模式");
            enableReadOnly(slave, "已启用数据源只读模式");
            configureSlaveReplication(master, slave);
            runtimeStateManager.markMasterActive(master.getId(), slave.getId(), "startup_master_confirmed");
            log.info("数据源状态已恢复正常: MASTER_ACTIVE（两个数据源都可用）");
        } else {
            log.info("从库数据更新，以从库为主库");
            disableReadOnly(slave, "已关闭数据源只读模式");
            enableReadOnly(master, "已启用数据源只读模式");
            configureSlaveReplication(slave, master);
            runtimeStateManager.markSlavePromoted(slave.getId(), master.getId(), "startup_slave_promoted_by_gtid");
            log.warn("数据源状态已恢复: SLAVE_PROMOTED（从库升为主库）");
        }
        logReplicationStatus(slave, "启动流程");
    }

    private void ensureCoordinationInitializedAndSync() {
        if (coordinationService == null || !coordinationService.isHealthy()) {
            return;
        }
        coordinationService.registerAsMaster(coordinationService.getInstanceId());
        ClusterStatus currentStatus = runtimeStateManager.current().getStatus();
        if (ClusterStatus.SLAVE_PROMOTED.equals(currentStatus)) {
            coordinationService.setMasterStatus(DatasourceCoordinationService.STATUS_SLAVE_PROMOTED);
            runtimeStateManager.replayStatusMessage(DatasourceCoordinationService.STATUS_SLAVE_PROMOTED);
        } else if (currentStatus != null && currentStatus.isRecoveryStage()) {
            coordinationService.broadcastRecoveryStage(
                    currentStatus.name(),
                    runtimeStateManager.current().getRecoverySourceNodeId(),
                    runtimeStateManager.current().getRecoveryTargetNodeId(),
                    runtimeStateManager.current().getLastReason()
            );
        } else {
            coordinationService.setMasterStatus(DatasourceCoordinationService.STATUS_NORMAL);
            runtimeStateManager.replayStatusMessage(DatasourceCoordinationService.STATUS_NORMAL);
        }
        log.info("状态已同步到 Redis: {}", currentStatus);
    }

    private boolean isMasterAvailable() {
        return dataSourceRegistry.getAvailableMasterCount() > 0;
    }

    private boolean isSlaveAvailable() {
        return dataSourceRegistry.getAvailableSlaveCount() > 0;
    }

    private DataSourceWrapper resolveConfiguredMaster() {
        return dataSourceRegistry.getAllMasters().isEmpty() ? null : dataSourceRegistry.getAllMasters().get(0);
    }

    private DataSourceWrapper resolveConfiguredSlave() {
        return dataSourceRegistry.getAllSlaves().isEmpty() ? null : dataSourceRegistry.getAllSlaves().get(0);
    }

    private DataSourceWrapper resolveNodeById(String nodeId) {
        if (nodeId == null) {
            return null;
        }
        DataSourceWrapper master = dataSourceRegistry.getMaster(nodeId);
        if (master != null) {
            return master;
        }
        return dataSourceRegistry.getSlave(nodeId);
    }

    private void configureSlaveReplication(DataSourceWrapper master, DataSourceWrapper slave) {
        String replicationUser = dataSourceProperties.getReplication().getMasterUser();
        String replicationPassword = dataSourceProperties.getReplication().getMasterPassword();
        if (isBlank(replicationUser) || isBlank(replicationPassword)) {
            log.info("未配置复制用户，跳过复制重建");
            return;
        }
        try (Connection connection = slave.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            stopSlave(statement);
            String sql = String.format(
                    "CHANGE MASTER TO MASTER_HOST='%s', MASTER_PORT=%d, MASTER_USER='%s', MASTER_PASSWORD='%s', MASTER_AUTO_POSITION=1",
                    extractHost(master.getUrl()),
                    extractPort(master.getUrl()),
                    replicationUser,
                    replicationPassword);
            statement.execute(sql);
            statement.execute("START SLAVE");
            log.info("已配置从库复制主库: {}:{}", extractHost(master.getUrl()), extractPort(master.getUrl()));
        } catch (SQLException ex) {
            log.warn("配置从库复制主库失败: {}", ex.getMessage());
        }
    }

    private void logReplicationStatus(DataSourceWrapper slave, String scope) {
        if (slave == null) {
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

    private String getGtidPosition(DataSourceWrapper wrapper) {
        try (Connection connection = wrapper.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SHOW MASTER STATUS")) {
            if (rs.next()) {
                String gtid = rs.getString("Executed_Gtid_Set");
                return gtid == null ? "" : gtid;
            }
        } catch (SQLException ex) {
            log.debug("获取节点 GTID 失败: nodeId={}, reason={}", wrapper.getId(), ex.getMessage());
        }
        return "";
    }

    private boolean isNewerOrEqual(String primaryGtid, String secondaryGtid) {
        if (primaryGtid == null || primaryGtid.trim().isEmpty()) {
            return false;
        }
        if (secondaryGtid == null || secondaryGtid.trim().isEmpty()) {
            return true;
        }
        try {
            Set<String> primarySet = new HashSet<String>(Arrays.asList(primaryGtid.split(",")));
            Set<String> secondarySet = new HashSet<String>(Arrays.asList(secondaryGtid.split(",")));
            return primarySet.size() >= secondarySet.size() || primarySet.containsAll(secondarySet);
        } catch (Exception ex) {
            return true;
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

    private void stopSlave(Statement statement) {
        try {
            statement.execute("STOP SLAVE");
            statement.execute("RESET SLAVE ALL");
        } catch (SQLException ignored) {
        }
    }

    private void broadcastRecoveryStage(ClusterStatus status, String sourceNodeId, String targetNodeId, String reason) {
        if (coordinationService == null || status == null) {
            return;
        }
        coordinationService.broadcastRecoveryStage(status.name(), sourceNodeId, targetNodeId, reason);
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

    private String emptyToUnknown(String value) {
        return value == null || value.trim().isEmpty() ? "UNKNOWN" : value;
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

    private String resolveMasterNodeId() {
        if (!topologyRegistry.getNodes().isEmpty()) {
            for (io.dbguardian.model.NodeModel node : topologyRegistry.getNodes()) {
                if (!"slave".equalsIgnoreCase(node.getRole())) {
                    return node.getId();
                }
            }
        }
        return dataSourceRegistry.getAllMasters().isEmpty() ? "master" : dataSourceRegistry.getAllMasters().get(0).getId();
    }

    private String resolveReadNodeId() {
        if (!topologyRegistry.getNodes().isEmpty()) {
            for (io.dbguardian.model.NodeModel node : topologyRegistry.getNodes()) {
                if ("slave".equalsIgnoreCase(node.getRole())) {
                    return node.getId();
                }
            }
        }
        return dataSourceRegistry.getAllSlaves().isEmpty() ? resolveMasterNodeId() : dataSourceRegistry.getAllSlaves().get(0).getId();
    }
}
