package io.dbguardian.boot3.config;

import io.dbguardian.core.FailoverOrchestrator;
import io.dbguardian.core.ReplicationRecoveryCoordinator;
import io.dbguardian.core.TopologyRegistry;
import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.core.registry.DataSourceRegistry;
import io.dbguardian.spi.DatabaseDialect;
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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ServiceLoader;

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
        initializeDatabaseAvailability();
        refreshRuntimeState("startup_initialized");
        log.info("DBGuardian 启动期运行态已初始化: status={}", runtimeStateManager.current().getStatus());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=== 应用启动完成，重新检测数据源状态 ===");
        if (!isSlaveConfigured()) {
            dataSourceHealthChecker.triggerHealthCheck();
        }
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
            if (isPostgreSql(resolveConfiguredSlave())) {
                String failedMasterId = currentState.getActiveMasterId() != null
                        ? currentState.getActiveMasterId()
                        : resolveMasterNodeId();
                if (failoverController.promoteStartupSlaveToPrimary(resolveConfiguredSlave(), failedMasterId,
                        reason + "_postgresql_slave_only")) {
                    log.warn("数据源状态已恢复: SLAVE_PROMOTED（PostgreSQL 从库启动期升主）");
                    return;
                }
            }
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
            log.debug("跳过原主恢复闭环: reason={}, recoveryEnabled={}, autoFailback={}",
                    reason,
                    dataSourceProperties.getRecovery().isEnabled(),
                    dataSourceProperties.getRecovery().isAutoFailback());
            return false;
        }
        if (!isOriginalMasterAvailable(state)) {
            log.debug("跳过原主恢复闭环: reason={}, originalMasterUnavailable={}, pendingOriginalMaster={}",
                    reason,
                    true,
                    state == null ? null : state.getPendingOriginalMasterId());
            return false;
        }
        boolean shouldRecover = replicationRecoveryCoordinator.shouldRecover(state);
        if (!shouldRecover) {
            log.debug("跳过原主恢复闭环: reason={}, shouldRecover=false, status={}, activeMaster={}, pendingOriginalMaster={}",
                    reason,
                    state == null ? null : state.getStatus(),
                    state == null ? null : state.getActiveMasterId(),
                    state == null ? null : state.getPendingOriginalMasterId());
            return false;
        }
        boolean shouldStartRecovery = replicationRecoveryCoordinator.shouldStartOriginalMasterRecovery(state);
        boolean remotePostgreSqlRecoveryPending = isRemotePostgreSqlRecoveryPending(state);
        boolean alreadyInRecoveryStage = ClusterStatus.CATCHING_UP_ORIGINAL_MASTER.equals(state.getStatus())
                || ClusterStatus.RESTORING_ORIGINAL_MASTER.equals(state.getStatus())
                || ClusterStatus.RESTORING_REPLICATION.equals(state.getStatus());
        if (!shouldStartRecovery && !remotePostgreSqlRecoveryPending && !alreadyInRecoveryStage) {
            log.debug("跳过原主恢复闭环: reason={}, shouldStartRecovery=false, remotePostgreSqlRecoveryPending=false, status={}, source={}, target={}",
                    reason,
                    state.getStatus(),
                    state.getRecoverySourceNodeId(),
                    state.getRecoveryTargetNodeId());
            return false;
        }
        log.info("进入原主恢复闭环: reason={}, status={}, activeMaster={}, pendingOriginalMaster={}, source={}, target={}",
                reason,
                state.getStatus(),
                state.getActiveMasterId(),
                state.getPendingOriginalMasterId(),
                state.getRecoverySourceNodeId(),
                state.getRecoveryTargetNodeId());
        recoverOriginalMasterAndRestoreReplication(reason);
        return true;
    }

    private boolean isOriginalMasterAvailable(ClusterRuntimeState state) {
        if (state == null || state.getPendingOriginalMasterId() == null) {
            return false;
        }
        DataSourceWrapper originalMaster = dataSourceRegistry.getMaster(state.getPendingOriginalMasterId());
        return originalMaster != null
                && dataSourceRegistry.getAvailableMasters().stream()
                .anyMatch(node -> state.getPendingOriginalMasterId().equals(node.getId()));
    }
    private boolean isRemotePostgreSqlRecoveryPending(ClusterRuntimeState state) {
        if (state == null || !ClusterStatus.SLAVE_PROMOTED.equals(state.getStatus())) {
            return false;
        }
        SpringDataSourceProperties.PostgreSqlRecoveryProperties postgresql = dataSourceProperties.getRecovery().getPostgresql();
        return postgresql.isEnabled()
                && postgresql.isRemoteExecutionEnabled()
                && !isBlank(postgresql.getRemoteExecutorCommand())
                && state.getPendingOriginalMasterId() != null
                && state.getActiveMasterId() != null;
    }

    private void recoverOriginalMasterAndRestoreReplication(String reason) {
        log.info("调用故障转移控制器执行原主恢复闭环: reason={}", reason);
        if (!failoverController.recoverOriginalMasterAndRestoreReplication(reason)) {
            log.warn("原主恢复闭环触发失败: reason={}", reason);
        }
    }

    private void determineAndConfigureMasterSlave() {
        if (!isMasterAvailable() || !isSlaveAvailable()) {
            runtimeStateManager.markMasterActive(resolveMasterNodeId(), resolveMasterNodeId(),
                    "replication_comparison_skipped_node_unavailable");
            log.info("跳过主从复制位点比较，降级到单主库模式: masterAvailable={}, slaveAvailable={}",
                    isMasterAvailable(), isSlaveAvailable());
            return;
        }
        DataSourceWrapper master = resolveConfiguredMaster();
        DataSourceWrapper slave = resolveConfiguredSlave();
        if (master == null || slave == null) {
            runtimeStateManager.markMasterActive(resolveMasterNodeId(), resolveReadNodeId(), "single_side_available_without_replication_chain");
            return;
        }

        String masterGtid = getGtidPosition(master);
        String slaveGtid = getGtidPosition(slave);
        String positionLabel = resolvePositionLabel(master, slave);
        log.info("主库 {}: {}", positionLabel, emptyToUnknown(masterGtid));
        log.info("从库 {}: {}", positionLabel, emptyToUnknown(slaveGtid));

        boolean masterPreferred = isNewerOrEqual(masterGtid, slaveGtid);
        if (masterPreferred) {
            log.info("主库数据更新，以主库为主库");
            if (attemptStartupPostgreSqlReplicationRestore(master, slave, masterGtid, slaveGtid)) {
                return;
            }
            disableReadOnly(master, "已关闭数据源只读模式");
            enableReadOnly(slave, "已启用数据源只读模式");
            configureSlaveReplication(master, slave);
            runtimeStateManager.markMasterActive(master.getId(), slave.getId(), "startup_master_confirmed");
            log.info("数据源状态已恢复正常: MASTER_ACTIVE（两个数据源都可用）");
        } else {
            log.info("从库数据更新，以从库为主库");
            if (attemptStartupPostgreSqlRecoveryClosure(master, slave, slaveGtid)) {
                return;
            }
            if (attemptStartupMySqlPromotion(master, slave)) {
                return;
            }
            fallbackToGenericPromotion(master, slave);
            return;
        }
        logReplicationStatus(slave, "启动流程");
    }

    private boolean attemptStartupPostgreSqlReplicationRestore(DataSourceWrapper master,
                                                               DataSourceWrapper slave,
                                                               String masterGtid,
                                                               String slaveGtid) {
        DatabaseDialect masterDialect = resolveDialect(master);
        if (masterDialect == null || !"postgresql".equalsIgnoreCase(masterDialect.getDatabaseType())) {
            return false;
        }
        if (isReplicaReplicationHealthy(slave)) {
            log.info("PostgreSQL 从节点复制已健康，跳过启动期复制关系恢复闭环: slave={}, masterLsn={}, slaveLsn={}",
                    slave.getId(),
                    emptyToUnknown(masterGtid),
                    emptyToUnknown(slaveGtid));
            runtimeStateManager.markMasterActive(master.getId(), slave.getId(), "startup_postgresql_replication_already_healthy");
            return true;
        }
        String replicationDecision = "startup_master_lsn=" + emptyToUnknown(masterGtid)
                + ", slave_lsn=" + emptyToUnknown(slaveGtid);
        log.warn("检测到 PostgreSQL 配置主库位点领先，但从节点未恢复复制，直接进入复制关系恢复闭环: slave={}, decision={}",
                slave.getId(),
                replicationDecision);
        disableReadOnly(master, "已关闭数据源只读模式");
        enableReadOnly(slave, "已启用数据源只读模式");
        if (!failoverController.restoreReplicationToPrimary(master, slave, "startup_postgresql_master_preferred")) {
            log.warn("PostgreSQL 启动期复制关系恢复触发失败，保持当前恢复阶段等待后续处理: master={}, slave={}", master.getId(), slave.getId());
        }
        return true;
    }

    private boolean attemptStartupPostgreSqlRecoveryClosure(DataSourceWrapper master, DataSourceWrapper slave, String slaveGtid) {
        DatabaseDialect masterDialect = resolveDialect(master);
        if (masterDialect == null || !"postgresql".equalsIgnoreCase(masterDialect.getDatabaseType())) {
            return false;
        }
        String gtidDecision = "startup_slave_has_newer_lsn=" + emptyToUnknown(slaveGtid);
        log.warn("检测到 PostgreSQL 从库位点领先，按已升主现状接管并直接进入原主恢复闭环: slave={}, decision={}", slave.getId(), gtidDecision);
        disableReadOnly(slave, "已关闭数据源只读模式");
        enableReadOnly(master, "已启用数据源只读模式");
        runtimeStateManager.markSlavePromoted(slave.getId(), master.getId(), "startup_postgresql_slave_ahead");
        if (!failoverController.recoverOriginalMasterAndRestoreReplication("startup_postgresql_slave_ahead")) {
            log.warn("PostgreSQL 启动期原主恢复闭环触发失败，保持当前从库升主状态等待后续恢复: master={}, slave={}", master.getId(), slave.getId());
        }
        return true;
    }

    private boolean attemptStartupMySqlPromotion(DataSourceWrapper master, DataSourceWrapper slave) {
        DatabaseDialect masterDialect = resolveDialect(master);
        if (masterDialect == null || !"mysql".equalsIgnoreCase(masterDialect.getDatabaseType())) {
            return false;
        }
        fallbackToGenericPromotion(master, slave);
        return true;
    }

    private void fallbackToGenericPromotion(DataSourceWrapper master, DataSourceWrapper slave) {
        promoteToPrimary(slave, "已执行从库提升为主库");
        disableReadOnly(slave, "已关闭数据源只读模式");
        enableReadOnly(master, "已启用数据源只读模式");
        configureSlaveReplication(slave, master);
        runtimeStateManager.markSlavePromoted(slave.getId(), master.getId(), "startup_slave_promoted_by_gtid");
        log.warn("数据源状态已恢复: SLAVE_PROMOTED（从库升为主库）");
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

    private void initializeDatabaseAvailability() {
        DataSourceWrapper master = resolveConfiguredMaster();
        DataSourceWrapper slave = resolveConfiguredSlave();
        boolean masterHealthy = isHealthyAtStartup(master);
        boolean slaveHealthy = isSlaveConfigured() && isHealthyAtStartup(slave);
        if (master != null) {
            dataSourceRegistry.updateMasterAvailability(master.getId(), masterHealthy);
        }
        if (slave != null) {
            dataSourceRegistry.updateSlaveAvailability(slave.getId(), slaveHealthy);
        }
        log.info("数据库启动可用性探测完成: master={}, slave={}", masterHealthy, slaveHealthy);
    }

    private boolean isHealthyAtStartup(DataSourceWrapper dataSource) {
        if (dataSource == null) {
            return false;
        }
        try (Connection connection = openStartupHealthCheckConnection(dataSource)) {
            return connection.isValid(5);
        } catch (SQLException ex) {
            log.warn("数据库启动探测失败: nodeId={}, reason={}", dataSource.getId(), ex.getMessage());
            return false;
        }
    }

    private Connection openStartupHealthCheckConnection(DataSourceWrapper dataSource) throws SQLException {
        if (isDirectJdbcDatabase(dataSource)) {
            return DriverManager.getConnection(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
        }
        return dataSource.getDataSource().getConnection();
    }

    private boolean isDirectJdbcDatabase(DataSourceWrapper dataSource) {
        if (dataSource == null || dataSource.getUrl() == null) {
            return false;
        }
        String url = dataSource.getUrl().toLowerCase();
        return url.startsWith("jdbc:mysql:") || url.startsWith("jdbc:postgresql:");
    }

    private Connection openDirectOrPoolConnection(DataSourceWrapper dataSource) throws SQLException {
        if (isDirectJdbcDatabase(dataSource)) {
            return DriverManager.getConnection(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
        }
        return dataSource.getDataSource().getConnection();
    }

    private boolean isPostgreSql(DataSourceWrapper dataSource) {
        DatabaseDialect dialect = resolveDialect(dataSource);
        return dialect != null
                && dialect.getDatabaseType() != null
                && "postgresql".equalsIgnoreCase(dialect.getDatabaseType());
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
        DatabaseDialect dialect = resolveDialect(slave);
        if (dialect == null || dialect.getStartReplicationSql() == null || dialect.buildChangeReplicationSourceSql("", 0, "", "") == null) {
            log.info("当前方言不支持复制重建，跳过: slave={}", slave.getId());
            return;
        }
        if (isBlank(replicationUser) || isBlank(replicationPassword)) {
            log.info("未配置复制用户，跳过复制重建");
            return;
        }
        try (Connection connection = openDirectOrPoolConnection(slave);
             Statement statement = connection.createStatement()) {
            stopSlave(statement, dialect);
            String sql = dialect.buildChangeReplicationSourceSql(
                    dialect.getHostFromJdbcUrl(master.getUrl()),
                    dialect.getPortFromJdbcUrl(master.getUrl()),
                    replicationUser,
                    replicationPassword);
            statement.execute(sql);
            statement.execute(dialect.getStartReplicationSql());
            log.info("已配置从库复制主库: {}:{}", dialect.getHostFromJdbcUrl(master.getUrl()), dialect.getPortFromJdbcUrl(master.getUrl()));
        } catch (SQLException ex) {
            log.warn("配置从库复制主库失败: {}", ex.getMessage());
        }
    }

    private void logReplicationStatus(DataSourceWrapper slave, String scope) {
        if (slave == null) {
            return;
        }
        DatabaseDialect dialect = resolveDialect(slave);
        if (dialect == null || dialect.getShowReplicaStatusSql() == null) {
            return;
        }
        try (Connection connection = openDirectOrPoolConnection(slave);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(dialect.getShowReplicaStatusSql())) {
            if (!rs.next()) {
                return;
            }
            String ioRunning = getNullableColumn(rs, dialect.getReplicaIoRunningColumn());
            String sqlRunning = getNullableColumn(rs, dialect.getReplicaSqlRunningColumn());
            log.info("复制状态 - IO: {}, SQL: {}", ioRunning, sqlRunning);
            long lag = parseLag(getNullableColumn(rs, dialect.getReplicaLagSecondsColumn()));
            runtimeStateManager.updateSlaveStatus(slave.getId(), true,
                    dialect.isReplicaRunning(ioRunning, sqlRunning),
                    lag,
                    scope + "_replication_observed");
        } catch (SQLException ex) {
            log.debug("读取复制状态失败: {}", ex.getMessage());
        }
    }

    private boolean isReplicaReplicationHealthy(DataSourceWrapper slave) {
        if (slave == null) {
            return false;
        }
        DatabaseDialect dialect = resolveDialect(slave);
        if (dialect == null || dialect.getShowReplicaStatusSql() == null) {
            return false;
        }
        try (Connection connection = openDirectOrPoolConnection(slave);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(dialect.getShowReplicaStatusSql())) {
            if (!rs.next()) {
                return false;
            }
            String ioRunning = getNullableColumn(rs, dialect.getReplicaIoRunningColumn());
            String sqlRunning = getNullableColumn(rs, dialect.getReplicaSqlRunningColumn());
            return dialect.isReplicaRunning(ioRunning, sqlRunning);
        } catch (SQLException ex) {
            log.debug("读取复制健康状态失败: nodeId={}, reason={}", slave.getId(), ex.getMessage());
            return false;
        }
    }

    private String getGtidPosition(DataSourceWrapper wrapper) {
        DatabaseDialect dialect = resolveDialect(wrapper);
        if (dialect == null || dialect.getShowPrimaryStatusSql() == null || dialect.getExecutedPositionColumn() == null) {
            return "";
        }
        try (Connection connection = openDirectOrPoolConnection(wrapper);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(dialect.getShowPrimaryStatusSql())) {
            if (rs.next()) {
                String gtid = getNullableColumn(rs, dialect.getExecutedPositionColumn());
                return gtid == null ? "" : gtid;
            }
        } catch (SQLException ex) {
            log.debug("获取节点复制位点失败: nodeId={}, reason={}", wrapper.getId(), ex.getMessage());
        }
        return "";
    }

    private String resolvePositionLabel(DataSourceWrapper master, DataSourceWrapper slave) {
        DatabaseDialect masterDialect = resolveDialect(master);
        if (masterDialect != null && "postgresql".equalsIgnoreCase(masterDialect.getDatabaseType())) {
            return "LSN";
        }
        DatabaseDialect slaveDialect = resolveDialect(slave);
        if (slaveDialect != null && "postgresql".equalsIgnoreCase(slaveDialect.getDatabaseType())) {
            return "LSN";
        }
        return "GTID";
    }

    private boolean isNewerOrEqual(String primaryGtid, String secondaryGtid) {
        if (primaryGtid == null || primaryGtid.trim().isEmpty()) {
            return false;
        }
        if (secondaryGtid == null || secondaryGtid.trim().isEmpty()) {
            return true;
        }
        Long lsnComparison = comparePostgreSqlLsn(primaryGtid, secondaryGtid);
        if (lsnComparison != null) {
            return lsnComparison.longValue() >= 0L;
        }
        try {
            java.util.Set<String> primarySet = new java.util.HashSet<String>(java.util.Arrays.asList(primaryGtid.split(",")));
            java.util.Set<String> secondarySet = new java.util.HashSet<String>(java.util.Arrays.asList(secondaryGtid.split(",")));
            return primarySet.size() >= secondarySet.size() || primarySet.containsAll(secondarySet);
        } catch (Exception ex) {
            return primaryGtid.trim().compareToIgnoreCase(secondaryGtid.trim()) >= 0;
        }
    }

    private Long comparePostgreSqlLsn(String left, String right) {
        long[] leftParts = parsePostgreSqlLsn(left);
        long[] rightParts = parsePostgreSqlLsn(right);
        if (leftParts == null || rightParts == null) {
            return null;
        }
        if (leftParts[0] != rightParts[0]) {
            return Long.valueOf(leftParts[0] - rightParts[0]);
        }
        return Long.valueOf(leftParts[1] - rightParts[1]);
    }

    private long[] parsePostgreSqlLsn(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.trim().split("/");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new long[]{
                    Long.parseLong(parts[0], 16),
                    Long.parseLong(parts[1], 16)
            };
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void promoteToPrimary(DataSourceWrapper wrapper, String successLog) {
        if (wrapper == null) {
            return;
        }
        DatabaseDialect dialect = resolveDialect(wrapper);
        if (dialect == null || dialect.getPromoteToPrimarySql() == null) {
            return;
        }
        try (Connection connection = openDirectOrPoolConnection(wrapper);
             Statement statement = connection.createStatement()) {
            statement.execute(dialect.getPromoteToPrimarySql());
            log.info(successLog);
        } catch (SQLException ex) {
            log.debug("提升节点为主库失败: nodeId={}, reason={}", wrapper.getId(), ex.getMessage());
        }
    }

    private void disableReadOnly(DataSourceWrapper wrapper, String successLog) {
        if (wrapper == null) {
            return;
        }
        DatabaseDialect dialect = resolveDialect(wrapper);
        if (dialect == null || dialect.getDisableReadOnlySql() == null) {
            return;
        }
        try (Connection connection = openDirectOrPoolConnection(wrapper);
             Statement statement = connection.createStatement()) {
            statement.execute(dialect.getDisableReadOnlySql());
            executeIfPresent(statement, dialect.getDisableStrictReadOnlySql());
            log.info(successLog);
        } catch (SQLException ex) {
            log.debug("关闭只读模式失败: nodeId={}, reason={}", wrapper.getId(), ex.getMessage());
        }
    }

    private void enableReadOnly(DataSourceWrapper wrapper, String successLog) {
        if (wrapper == null) {
            return;
        }
        DatabaseDialect dialect = resolveDialect(wrapper);
        if (dialect == null || dialect.getEnableReadOnlySql() == null) {
            return;
        }
        try (Connection connection = openDirectOrPoolConnection(wrapper);
             Statement statement = connection.createStatement()) {
            statement.execute(dialect.getEnableReadOnlySql());
            executeIfPresent(statement, dialect.getEnableStrictReadOnlySql());
            log.info(successLog);
        } catch (SQLException ex) {
            log.debug("开启只读模式失败: nodeId={}, reason={}", wrapper.getId(), ex.getMessage());
        }
    }

    private void stopSlave(Statement statement, DatabaseDialect dialect) {
        try {
            executeIfPresent(statement, dialect == null ? null : dialect.getStopReplicationSql());
            executeIfPresent(statement, dialect == null ? null : dialect.getResetReplicationSql());
        } catch (SQLException ignored) {
        }
    }

    private void executeIfPresent(Statement statement, String sql) throws SQLException {
        if (sql != null && !sql.trim().isEmpty()) {
            statement.execute(sql);
        }
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

    private boolean isSlaveConfigured() {
        String url = dataSourceProperties.getSlave().getUrl();
        return url != null && !url.trim().isEmpty();
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
