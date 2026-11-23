package io.dbguardian.spring.failover;

import io.dbguardian.core.GtidConsistencyInspector;
import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.core.registry.DataSourceRegistry;
import io.dbguardian.spi.DatabaseDialect;
import io.dbguardian.spi.GtidDatabaseDialect;
import io.dbguardian.spi.GtidDatabaseDialect.PositionOrder;
import io.dbguardian.runtime.ClusterRuntimeState;
import io.dbguardian.runtime.ClusterStatus;
import io.dbguardian.spring.runtime.ClusterRuntimeStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
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
    private boolean autoStart = true;

    public DataSourceHealthChecker(DataSourceRegistry registry, FailoverController failoverController) {
        this.registry = registry;
        this.failoverController = failoverController;
    }

    @PostConstruct
    public void startHealthChecks() {
        if (!autoStart) {
            log.info("健康检查已配置为由运行时编排器启动");
            return;
        }
        startHealthChecksInternal();
    }

    public void startHealthChecksAfterInitialization() {
        startHealthChecksInternal();
    }

    private void startHealthChecksInternal() {
        if (!failoverEnabled) {
            log.info("故障转移已禁用，跳过健康检查");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executor.scheduleAtFixedRate(this::checkMasters, 0, masterCheckIntervalSeconds, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(this::checkSlaves, slaveCheckIntervalSeconds, slaveCheckIntervalSeconds, TimeUnit.SECONDS);
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
            DatabaseDialect dialect = resolveDialect(master);
            if (dialect instanceof GtidDatabaseDialect && runtimeStateManager != null) {
                checkGtidNode(master, (GtidDatabaseDialect) dialect);
                continue;
            }
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
            DatabaseDialect dialect = resolveDialect(slave);
            if (dialect instanceof GtidDatabaseDialect && runtimeStateManager != null) {
                checkGtidNode(slave, (GtidDatabaseDialect) dialect);
                continue;
            }
            boolean promotedActiveMaster = isPromotedActiveMaster(slave.getId());
            boolean healthy = isHealthy(slave);
            boolean replicationHealthy = promotedActiveMaster || (healthy && checkReplicationHealthy(slave));
            boolean wasAvailable = registry.getAvailableSlaves().stream().anyMatch(item -> item.getId().equals(slave.getId()));
            if (shouldRecoverSlaveReplication(slave, healthy, replicationHealthy, promotedActiveMaster, wasAvailable)) {
                boolean rebuilt = failoverController.recoverSlaveReplication(slave.getId());
                if (rebuilt) {
                    // The next scheduled observation is the admission gate for read traffic.
                    replicationHealthy = false;
                }
            }
            long lag = promotedActiveMaster || !healthy ? 0L : getReplicationLagSeconds(slave);
            boolean promotionCandidate = shouldKeepSlaveAvailableForPromotion(healthy, replicationHealthy);
            if (healthy && (replicationHealthy || promotionCandidate)) {
                slave.healthCheckSuccess();
                if (runtimeStateManager != null) {
                    runtimeStateManager.updateSlaveStatus(slave.getId(), true, replicationHealthy, lag,
                            promotedActiveMaster ? "slave_promoted_master_active"
                                    : promotionCandidate ? "slave_promotion_candidate"
                                    : "slave_health_check_ok");
                }
                applyGtidProtection(slave, promotedActiveMaster);
                if (!wasAvailable) {
                    registry.updateSlaveAvailability(slave.getId(), true);
                    log.info("从库 {} 恢复", slave.getId());
                    failoverController.handleSlaveRecovery(slave.getId());
                }
            } else {
                slave.healthCheckFailed();
                registry.updateSlaveAvailability(slave.getId(), false);
                if (runtimeStateManager != null) {
                    runtimeStateManager.updateSlaveStatus(slave.getId(), healthy, replicationHealthy, lag, "slave_health_check_failed");
                }
                applyGtidProtection(slave, promotedActiveMaster);
                if (wasAvailable) {
                    log.warn("从库 {} 不可用，已从负载池移除", slave.getId());
                    failoverController.handleSlaveFailure(slave.getId());
                }
            }
        }
    }

    private void checkGtidNode(DataSourceWrapper node, GtidDatabaseDialect dialect) {
        boolean reachable = isHealthy(node);
        synchronized (failoverController) {
            ClusterRuntimeState state = runtimeStateManager.current();
            if (node.getId().equals(state.getActiveMasterId())) {
                updateGtidAvailability(node, reachable);
                if (!reachable && state.getPendingOriginalMasterId() == null) {
                    failoverController.handleMasterFailure(node.getId());
                }
            } else if (node.getId().equals(state.getPendingOriginalMasterId())) {
                updateGtidAvailability(node, reachable);
                if (reachable) {
                    failoverController.handleOriginalMasterRecovery(node.getId());
                }
            } else if (ClusterStatus.MASTER_ACTIVE.equals(state.getStatus()) && !state.isRecoveryInProgress()) {
                checkGtidReplica(node, reachable, dialect);
            } else if (!reachable) {
                updateGtidAvailability(node, false);
            }
        }
    }

    private void updateGtidAvailability(DataSourceWrapper node, boolean available) {
        if (registry.getMaster(node.getId()) != null) {
            registry.updateMasterAvailability(node.getId(), available);
        } else {
            registry.updateSlaveAvailability(node.getId(), available);
        }
    }

    private void checkGtidReplica(DataSourceWrapper replica, boolean reachable, GtidDatabaseDialect dialect) {
        ClusterRuntimeState state = runtimeStateManager.current();
        DataSourceWrapper source = resolveCurrentMasterForSlaveCheck(state, replica.getId());
        if (source == null || !source.isAvailable()) {
            if (!reachable) {
                updateGtidAvailability(replica, false);
            }
            return;
        }
        boolean healthy = false;
        long lag = Long.MAX_VALUE;
        boolean positionRisk = false;
        boolean readOnly = false;
        Object channelStatus = "replica unreachable";
        if (reachable) {
            try (Connection connection = openHealthCheckConnection(replica)) {
                GtidReplication.ChannelStatus channel = GtidReplication.readChannelStatus(connection, dialect, source);
                channelStatus = channel;
                healthy = channel.running;
                lag = channel.lagSeconds;
                if (healthy) {
                    try {
                        readOnly = GtidReplication.isGlobalReadOnly(connection, dialect);
                    } catch (SQLException ex) {
                        log.warn("无法确认从库全局只读: replica={}, sqlState={}, errorCode={}",
                                replica.getId(), ex.getSQLState(), ex.getErrorCode());
                    }
                }
                if (gtidProtectionEnabled) {
                    String replicaPosition;
                    try (Statement statement = connection.createStatement()) {
                        replicaPosition = GtidReplication.readPosition(statement, dialect);
                    }
                    String sourcePosition;
                    try (Connection sourceConnection = openHealthCheckConnection(source);
                         Statement statement = sourceConnection.createStatement()) {
                        sourcePosition = GtidReplication.readPosition(statement, dialect);
                    } catch (SQLException ex) {
                        // The writer may be going down; preserve the last known replica eligibility.
                        log.debug("跳过 GTID 位点比较: source={}, replica={}, sqlState={}, errorCode={}",
                                source.getId(), replica.getId(), ex.getSQLState(), ex.getErrorCode());
                        return;
                    }
                    PositionOrder order = dialect.comparePositions(replicaPosition, sourcePosition);
                    positionRisk = order != PositionOrder.EQUAL && order != PositionOrder.BEHIND;
                    if (positionRisk) {
                        log.warn("GTID 复制位点未知或从库含额外事务，暂停从读: replica={}, decision={}", replica.getId(), order);
                    }
                }
            } catch (SQLException ex) {
                healthy = false;
                channelStatus = "status unavailable (sqlState=" + ex.getSQLState() + ", errorCode=" + ex.getErrorCode() + ")";
            } catch (IllegalArgumentException ex) {
                healthy = false;
                channelStatus = "invalid source configuration";
            }
        }
        if (healthy && !readOnly) {
            readOnly = failoverController.restoreGtidReplicaReadOnly(replica, source, dialect);
            // A concurrent role switch may have made this observation obsolete.
            ClusterRuntimeState current = runtimeStateManager.current();
            if (!source.getId().equals(current.getActiveMasterId()) || current.isRecoveryInProgress()) {
                return;
            }
        }
        boolean wasAvailable = replica.isAvailable();
        boolean lagRisk = gtidProtectionEnabled && blockSlaveReadsOnRisk && gtidConsistencyInspector != null
                && gtidConsistencyInspector.exceedsLagLimit(lag);
        boolean admitted = reachable && healthy && readOnly && !positionRisk && !lagRisk;
        if (admitted != wasAvailable) {
            if (admitted) {
                log.info("GTID 复制检查已恢复从读: source={}, replica={}, {}", source.getId(), replica.getId(), channelStatus);
            } else {
                log.warn("GTID 复制检查暂停从读: source={}, replica={}, readOnly={}, positionRisk={}, lagRisk={}, {}",
                        source.getId(), replica.getId(), readOnly, positionRisk, lagRisk, channelStatus);
            }
        }
        log.debug("GTID 复制状态（定时检查）: source={}, replica={}, readOnly={}, readEligible={}, {}",
                source.getId(), replica.getId(), readOnly, admitted, channelStatus);
        updateGtidAvailability(replica, admitted);
        if (admitted) {
            replica.healthCheckSuccess();
            if (!wasAvailable || (state != null && state.isSlaveReadsBlocked())) {
                failoverController.handleSlaveRecovery(replica.getId());
            }
        } else {
            replica.healthCheckFailed();
            if (wasAvailable) {
                failoverController.handleSlaveFailure(replica.getId());
            }
            if (reachable && !healthy && !positionRisk) {
                failoverController.recoverSlaveReplication(replica.getId());
            }
        }
        if (runtimeStateManager != null) {
            String detail = "gtid_replication_observed";
            if (positionRisk) {
                detail = GtidReplication.POSITION_RISK;
            } else if (healthy && !readOnly) {
                detail = "gtid_replica_read_only_unconfirmed";
            } else if (lagRisk) {
                detail = "gtid_lag_risk";
            }
            runtimeStateManager.updateSlaveStatus(replica.getId(), admitted, healthy, lag, detail);
        }
    }

    private void applyGtidProtection(DataSourceWrapper slave, boolean promotedActiveMaster) {
        if (!gtidProtectionEnabled || runtimeStateManager == null || gtidConsistencyInspector == null || slave == null) {
            return;
        }
        if (promotedActiveMaster) {
            return;
        }
        ClusterRuntimeState state = runtimeStateManager.current();
        if (state == null) {
            return;
        }
        if (ClusterStatus.CONTAMINATION_RECOVERY.equals(state.getStatus())) {
            return;
        }
        DataSourceWrapper master = resolveCurrentMasterForSlaveCheck(state, slave.getId());
        if (master == null) {
            return;
        }

        String masterGtidSet = getMasterGtidPosition(master);
        String slaveGtidSet = getExecutedGtidSet(slave);
        Set<String> extraTransactions = gtidConsistencyInspector.detectExtraTransactions(masterGtidSet, slaveGtidSet);
        if (!extraTransactions.isEmpty()) {
            String gtidDecision = gtidConsistencyInspector.describeExtraTransactions(extraTransactions);
            runtimeStateManager.markContamination(
                    slave.getId(),
                    true,
                    true,
                    gtidDecision,
                    "gtid_extra_transactions_detected",
                    "gtid_contamination_detected"
            );
            log.warn("检测到从库存在额外 GTID，暂停从读并触发主库追赶: slaveId={}, gtidDecision={}", slave.getId(), gtidDecision);
            failoverController.recoverMasterFromContaminatedSlave(slave.getId(), gtidDecision, "gtid_contamination_detected");
            return;
        }

        if (!blockSlaveReadsOnRisk) {
            return;
        }
        String blockedSlaveId = gtidConsistencyInspector.findBlockedSlaveId(state);
        if (blockedSlaveId != null) {
            runtimeStateManager.markContamination(
                    blockedSlaveId,
                    false,
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

    private boolean shouldRecoverSlaveReplication(DataSourceWrapper slave,
                                                  boolean healthy,
                                                  boolean replicationHealthy,
                                                  boolean promotedActiveMaster,
                                                  boolean wasAvailable) {
        if (!healthy || replicationHealthy || promotedActiveMaster || wasAvailable || slave == null) {
            return false;
        }
        DatabaseDialect dialect = resolveDialect(slave);
        if (dialect == null || dialect.getDatabaseType() == null || registry.getAvailableMasterCount() == 0) {
            return false;
        }
        String databaseType = dialect.getDatabaseType();
        return "mysql".equalsIgnoreCase(databaseType) || "postgresql".equalsIgnoreCase(databaseType);
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
                && (ClusterStatus.SLAVE_PROMOTED.equals(state.getStatus())
                || state.getStatus() != null && state.getStatus().isRecoveryStage());
    }

    private boolean isHealthy(DataSourceWrapper dataSourceWrapper) {
        long start = System.currentTimeMillis();
        try (Connection connection = openHealthCheckConnection(dataSourceWrapper)) {
            boolean valid = connection != null && connection.isValid(5);
            if (valid) {
                dataSourceWrapper.updateResponseTime(System.currentTimeMillis() - start);
            }
            return valid;
        } catch (SQLException ex) {
            return false;
        }
    }

    private Connection openHealthCheckConnection(DataSourceWrapper dataSourceWrapper) throws SQLException {
        if (isDirectJdbcDatabase(dataSourceWrapper)) {
            return DriverManager.getConnection(
                    dataSourceWrapper.getUrl(),
                    dataSourceWrapper.getUsername(),
                    dataSourceWrapper.getPassword());
        }
        return dataSourceWrapper.getDataSource().getConnection();
    }

    private boolean isDirectJdbcDatabase(DataSourceWrapper dataSourceWrapper) {
        if (dataSourceWrapper == null || dataSourceWrapper.getUrl() == null) {
            return false;
        }
        String url = dataSourceWrapper.getUrl().toLowerCase();
        return url.startsWith("jdbc:mysql:") || url.startsWith("jdbc:postgresql:") || url.startsWith("jdbc:mariadb:");
    }

    private boolean checkReplicationHealthy(DataSourceWrapper slave) {
        DatabaseDialect dialect = resolveDialect(slave);
        if (dialect == null || dialect.getShowReplicaStatusSql() == null) {
            return true;
        }
        try (Connection connection = openHealthCheckConnection(slave);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(dialect.getShowReplicaStatusSql())) {
            if (!rs.next()) {
                return true;
            }
            String ioRunning = getNullableColumn(rs, dialect.getReplicaIoRunningColumn());
            String sqlRunning = getNullableColumn(rs, dialect.getReplicaSqlRunningColumn());
            log.debug("复制状态 - IO: {}, SQL: {}", ioRunning, sqlRunning);
            if (isPostgreSqlPrimaryRoleState(dialect, ioRunning, sqlRunning)) {
                return true;
            }
            return dialect.isReplicaRunning(ioRunning, sqlRunning);
        } catch (SQLException ex) {
            return false;
        }
    }

    private boolean isPostgreSqlPrimaryRoleState(DatabaseDialect dialect, String ioRunning, String sqlRunning) {
        if (dialect == null || dialect.getDatabaseType() == null || !"postgresql".equalsIgnoreCase(dialect.getDatabaseType())) {
            return false;
        }
        return "primary".equalsIgnoreCase(ioRunning) && "primary".equalsIgnoreCase(sqlRunning);
    }

    private long getReplicationLagSeconds(DataSourceWrapper slave) {
        DatabaseDialect dialect = resolveDialect(slave);
        if (dialect == null || dialect.getShowReplicaStatusSql() == null || dialect.getReplicaLagSecondsColumn() == null) {
            return 0L;
        }
        try (Connection connection = openHealthCheckConnection(slave);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(dialect.getShowReplicaStatusSql())) {
            if (!rs.next()) {
                return 0L;
            }
            String lag = getNullableColumn(rs, dialect.getReplicaLagSecondsColumn());
            if (lag == null || "NULL".equalsIgnoreCase(lag)) {
                return 0L;
            }
            return Long.parseLong(lag);
        } catch (Exception ex) {
            return 0L;
        }
    }

    private DataSourceWrapper resolveCurrentMasterForSlaveCheck(ClusterRuntimeState state, String slaveNodeId) {
        if (state != null && state.getActiveMasterId() != null && !state.getActiveMasterId().equals(slaveNodeId)) {
            DataSourceWrapper activeMaster = registry.getMaster(state.getActiveMasterId());
            if (activeMaster != null) {
                return activeMaster;
            }
            DataSourceWrapper promotedMaster = registry.getSlave(state.getActiveMasterId());
            if (promotedMaster != null && !promotedMaster.getId().equals(slaveNodeId)) {
                return promotedMaster;
            }
        }
        for (DataSourceWrapper master : registry.getAllMasters()) {
            if (!master.getId().equals(slaveNodeId)) {
                return master;
            }
        }
        return null;
    }

    private String getMasterGtidPosition(DataSourceWrapper master) {
        if (master == null) {
            return null;
        }
        DatabaseDialect dialect = resolveDialect(master);
        if (dialect == null || dialect.getShowPrimaryStatusSql() == null || dialect.getExecutedPositionColumn() == null) {
            return null;
        }
        try (Connection connection = openHealthCheckConnection(master);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(dialect.getShowPrimaryStatusSql())) {
            if (rs.next()) {
                return getNullableColumn(rs, dialect.getExecutedPositionColumn());
            }
        } catch (SQLException ex) {
            log.debug("读取主库复制位点失败: nodeId={}, reason={}", master.getId(), ex.getMessage());
        }
        return null;
    }

    private String getExecutedGtidSet(DataSourceWrapper slave) {
        DatabaseDialect dialect = resolveDialect(slave);
        if (dialect == null || dialect.getShowReplicaStatusSql() == null || dialect.getExecutedPositionColumn() == null) {
            return getMasterGtidPosition(slave);
        }
        try (Connection connection = openHealthCheckConnection(slave);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(dialect.getShowReplicaStatusSql())) {
            if (rs.next()) {
                String executed = getNullableColumn(rs, dialect.getExecutedPositionColumn());
                if (executed != null && !executed.trim().isEmpty()) {
                    return executed;
                }
            }
        } catch (SQLException ex) {
            log.debug("读取从库复制位点失败: nodeId={}, reason={}", slave.getId(), ex.getMessage());
        }
        return getMasterGtidPosition(slave);
    }

    private DatabaseDialect resolveDialect(DataSourceWrapper wrapper) {
        if (wrapper == null || wrapper.getUrl() == null) {
            return null;
        }
        for (DatabaseDialect dialect : ServiceLoader.load(DatabaseDialect.class)) {
            String databaseType = dialect.getDatabaseType();
            if (databaseType != null && wrapper.getUrl().toLowerCase().startsWith("jdbc:" + databaseType.toLowerCase() + ":")) {
                return dialect;
            }
        }
        return null;
    }

    private String getNullableColumn(ResultSet rs, String column) throws SQLException {
        if (column == null || column.trim().isEmpty()) {
            return null;
        }
        try {
            return rs.getString(column);
        } catch (SQLException ex) {
            return null;
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

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }
}