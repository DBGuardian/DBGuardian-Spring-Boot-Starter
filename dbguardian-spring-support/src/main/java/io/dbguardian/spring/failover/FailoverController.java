package io.dbguardian.spring.failover;

import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.core.recovery.DatabaseRecoveryHandler;
import io.dbguardian.core.recovery.DatabaseRecoverySettings;
import io.dbguardian.core.recovery.DialectRecoveryContext;
import io.dbguardian.core.registry.DataSourceRegistry;
import io.dbguardian.spi.DatabaseDialect;
import io.dbguardian.spi.GtidDatabaseDialect;
import io.dbguardian.spi.GtidDatabaseDialect.PositionOrder;
import io.dbguardian.runtime.ClusterRuntimeState;
import io.dbguardian.runtime.ClusterStatus;
import io.dbguardian.runtime.NodeRuntimeState;
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
import java.util.ServiceLoader;
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
    private DatabaseRecoverySettings recoverySettings = new DatabaseRecoverySettings();

    public FailoverController(DataSourceRegistry registry, DatasourceCoordinationService coordinationService) {
        this.registry = registry;
        this.coordinationService = coordinationService;
    }

    public synchronized void handleMasterFailure(String failedMasterId) {
        if (runtimeStateManager != null) {
            ClusterRuntimeState state = runtimeStateManager.current();
            if (state != null
                    && ClusterStatus.SLAVE_PROMOTED.equals(state.getStatus())
                    && state.getActiveMasterId() != null
                    && !state.getActiveMasterId().equals(failedMasterId)) {
                log.warn("当前已处于从库升主状态，跳过重复故障转移: failedMasterId={}, activeMaster={}", failedMasterId, state.getActiveMasterId());
                return;
            }
        }
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
                DatabaseDialect failedDialect = resolveDialect(resolveNodeById(failedMasterId));
                List<DataSourceWrapper> candidates = registry.getAvailableSlaves();
                if (failedDialect instanceof GtidDatabaseDialect && registry.getSlave(failedMasterId) != null) {
                    // After a no-failback recovery, the configured primary is the actual replica.
                    candidates.addAll(registry.getAvailableMasters());
                }
                List<DataSourceWrapper> availableSlaves = candidates.stream()
                        .filter(wrapper -> !wrapper.getId().equals(failedMasterId))
                        .filter(wrapper -> isEligiblePromotionCandidate(wrapper, failedDialect))
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

    private boolean isEligiblePromotionCandidate(DataSourceWrapper candidate, DatabaseDialect failedDialect) {
        DatabaseDialect candidateDialect = resolveDialect(candidate);
        if (!(failedDialect instanceof GtidDatabaseDialect) && !(candidateDialect instanceof GtidDatabaseDialect)) {
            return true;
        }
        if (failedDialect != null && (candidateDialect == null
                || !failedDialect.getDatabaseType().equalsIgnoreCase(candidateDialect.getDatabaseType()))) {
            return false;
        }
        NodeRuntimeState node = runtimeStateManager == null ? null : runtimeStateManager.current().getNodeState(candidate.getId());
        return node == null || !GtidReplication.POSITION_RISK.equals(node.getLastReplicationError());
    }

    public synchronized boolean promoteStartupSlaveToPrimary(DataSourceWrapper promotedNode,
                                                              String originalMasterId,
                                                              String reason) {
        if (promotedNode == null) {
            log.warn("启动期 PostgreSQL 升主跳过: promotedNode=null");
            return false;
        }
        try {
            promoteToMaster(promotedNode);
            currentMasterId = promotedNode.getId();
            if (runtimeStateManager != null) {
                runtimeStateManager.markSlavePromoted(promotedNode.getId(), originalMasterId, reason);
            }
            if (coordinationService != null && coordinationService.isHealthy()) {
                if (resolveDialect(promotedNode) instanceof GtidDatabaseDialect) {
                    coordinationService.broadcastFailoverEvent(originalMasterId, promotedNode.getId());
                }
                coordinationService.setMasterStatus(DatasourceCoordinationService.STATUS_SLAVE_PROMOTED);
            }
            log.warn("启动期从库已升为主库，跳过失联原主操作: promotedMaster={}, originalMaster={}",
                    promotedNode.getId(), originalMasterId);
            return true;
        } catch (SQLException ex) {
            log.error("启动期从库升主失败: nodeId={}, reason={}", promotedNode.getId(), ex.getMessage(), ex);
            if (runtimeStateManager != null) {
                runtimeStateManager.markDegraded(reason + "_promotion_failed");
            }
            return false;
        } catch (RuntimeException ex) {
            log.error("启动期从库升主失败: nodeId={}, reason={}", promotedNode.getId(), ex.getMessage(), ex);
            if (runtimeStateManager != null) {
                runtimeStateManager.markDegraded(reason + "_promotion_failed");
            }
            return false;
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
        DataSourceWrapper recoveredMaster = resolveNodeById(recoveredMasterId);
        DatabaseDialect dialect = resolveDialect(recoveredMaster);
        if (dialect instanceof GtidDatabaseDialect) {
            try {
                setReadOnly(recoveredMaster, (GtidDatabaseDialect) dialect, true);
            } catch (SQLException ex) {
                log.warn("原主恢复后无法设置只读，暂不开始 GTID 追赶: nodeId={}, reason={}",
                        recoveredMasterId, ex.getMessage());
                return;
            }
        }
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

    public boolean recoverOriginalMasterAndRestoreReplication(String reason) {
        return recoverOriginalMasterAndRestoreReplication(reason, true);
    }

    public synchronized boolean recoverOriginalMasterAndRestoreReplication(String reason, boolean autoFailback) {
        if (runtimeStateManager == null) {
            log.warn("恢复原主流程跳过: runtimeStateManager 未初始化, reason={}", reason);
            return false;
        }
        ClusterRuntimeState state = runtimeStateManager.current();
        String originalMasterId = resolveRecoverySource(state);
        String promotedMasterId = resolveRecoveryTarget(state);
        log.info("恢复原主流程入口: reason={}, status={}, activeMaster={}, pendingOriginalMaster={}, source={}, target={}",
                reason,
                safeClusterStatus(state),
                safeActiveMasterId(state),
                safePendingOriginalMasterId(state),
                originalMasterId,
                promotedMasterId);
        DataSourceWrapper originalMaster = resolveNodeById(originalMasterId);
        DataSourceWrapper promotedMaster = resolveNodeById(promotedMasterId);
        if (originalMaster == null || promotedMaster == null) {
            log.warn("恢复原主流程缺少节点: originalMaster={}, promotedMaster={}, resolvedOriginalMaster={}, resolvedPromotedMaster={}",
                    originalMasterId,
                    promotedMasterId,
                    originalMaster != null,
                    promotedMaster != null);
            return false;
        }
        if (originalMaster.getId().equals(promotedMaster.getId())) {
            log.info("恢复原主流程跳过同节点闭环: reason={}, nodeId={}, status={}",
                    reason,
                    originalMaster.getId(),
                    safeClusterStatus(state));
            return true;
        }

        DatabaseDialect originalMasterDialect = resolveDialect(originalMaster);
        if (originalMasterDialect instanceof GtidDatabaseDialect) {
            boolean coordinated = coordinationService != null && coordinationService.isHealthy();
            if (coordinated && !coordinationService.tryAcquireFailoverLock(60L + 2L * Math.max(0, catchupTimeoutSeconds))) {
                return false;
            }
            try {
                return recoverGtidPrimary(originalMaster, promotedMaster, (GtidDatabaseDialect) originalMasterDialect, reason, autoFailback);
            } finally {
                if (coordinated) {
                    coordinationService.releaseFailoverLock();
                }
            }
        }
        ClusterStatus currentStatus;
        if (state == null) {
            currentStatus = null;
        } else {
            currentStatus = state.getStatus();
        }
        if (ClusterStatus.RESTORING_REPLICATION.equals(currentStatus)
                && isPostgreSqlStandby(originalMaster)
                && isPostgreSqlPrimary(promotedMaster)) {
            log.warn("PostgreSQL 回切阶段与实际角色不一致，先恢复原主主库角色: originalMaster={}, promotedMaster={}",
                    originalMasterId, promotedMasterId);
            moveToRecoveryStage(ClusterStatus.RESTORING_ORIGINAL_MASTER, originalMasterId, promotedMasterId,
                    reason + "_postgresql_role_reconcile");
            currentStatus = ClusterStatus.RESTORING_ORIGINAL_MASTER;
        }
        if (ClusterStatus.RESTORING_REPLICATION.equals(currentStatus)) {
            currentMasterId = originalMasterId;
            if (!restoreOriginalReplication(originalMaster, promotedMaster)) {
                log.warn("原始复制关系恢复未完成，保持恢复阶段: originalMaster={}, rejoinNode={}", originalMasterId, promotedMasterId);
                return false;
            }
            finishOriginalMasterRecovery(reason, originalMaster, promotedMaster);
            return true;
        }
        if (ClusterStatus.RESTORING_ORIGINAL_MASTER.equals(currentStatus)) {
            if (!restoreOriginalMasterRole(originalMaster)) {
                log.warn("原主主库身份恢复未完成，保持恢复阶段: originalMaster={}", originalMasterId);
                return false;
            }
            currentMasterId = originalMasterId;
            moveToRecoveryStage(ClusterStatus.RESTORING_REPLICATION, originalMasterId, promotedMasterId, reason + "_replication_restoring");
            if (!restoreOriginalReplication(originalMaster, promotedMaster)) {
                log.warn("原始复制关系恢复未完成，保持恢复阶段: originalMaster={}, rejoinNode={}", originalMasterId, promotedMasterId);
                return false;
            }
            finishOriginalMasterRecovery(reason, originalMaster, promotedMaster);
            return true;
        }

        moveToRecoveryStage(ClusterStatus.RECOVERING_ORIGINAL_MASTER, originalMasterId, promotedMasterId, reason + "_recovery_started");
        if (!recoverMasterAsSlave(originalMaster, promotedMaster)) {
            runtimeStateManager.markSlavePromoted(promotedMasterId, originalMasterId, reason + "_manual_original_master_rebuild_required");
            log.warn("原主恢复为从库未完成，停止自动恢复闭环: originalMaster={}, currentMaster={}", originalMasterId, promotedMasterId);
            return false;
        }

        moveToRecoveryStage(ClusterStatus.CATCHING_UP_ORIGINAL_MASTER, originalMasterId, promotedMasterId, reason + "_catchup_started");
        waitForCatchupOnMaster(originalMaster);

        boolean keepPromotedPrimary = shouldKeepPromotedPrimaryAfterRecovery(originalMasterDialect);
        log.info("原主追赶完成后的主从决策: keepPromotedPrimary={}, dialect={}, currentMaster={}, originalMaster={}",
                keepPromotedPrimary,
                safeDialectType(originalMasterDialect),
                promotedMasterId,
                originalMasterId);
        if (keepPromotedPrimary) {
            moveToRecoveryStage(ClusterStatus.RESTORING_REPLICATION, promotedMasterId, originalMasterId, reason + "_dialect_replication_restored");
            currentMasterId = promotedMasterId;
            runtimeStateManager.markMasterActive(promotedMasterId, originalMasterId, reason + "_dialect_recovery_completed");
            if (coordinationService != null) {
                coordinationService.setMasterStatus(DatasourceCoordinationService.STATUS_NORMAL);
            }
            logReplicationStatus(originalMaster, "dialect_original_master_catchup");
            log.info("原主追赶完成并保留当前主库: currentMaster={}, standby={}", promotedMasterId, originalMasterId);
            return true;
        }

        moveToRecoveryStage(ClusterStatus.RESTORING_ORIGINAL_MASTER, originalMasterId, promotedMasterId, reason + "_original_master_restoring");
        if (!restoreOriginalMasterRole(originalMaster)) {
            log.warn("原主主库身份恢复未完成，保持恢复阶段: originalMaster={}", originalMasterId);
            return false;
        }

        currentMasterId = originalMasterId;
        moveToRecoveryStage(ClusterStatus.RESTORING_REPLICATION, originalMasterId, promotedMasterId, reason + "_replication_restoring");
        if (!restoreOriginalReplication(originalMaster, promotedMaster)) {
            log.warn("原始复制关系恢复未完成，保持恢复阶段: originalMaster={}, rejoinNode={}", originalMasterId, promotedMasterId);
            return false;
        }

        finishOriginalMasterRecovery(reason, originalMaster, promotedMaster);
        return true;
    }

    /** Reconcile the configured pair using the dialect's position ordering, not GTID string counts. */
    public synchronized void reconcileGtidTopology(DataSourceWrapper master, DataSourceWrapper replica,
                                                   GtidDatabaseDialect dialect, boolean autoFailback) {
        reconcileGtidTopology(master, replica, dialect, true, autoFailback);
    }

    public synchronized void reconcileGtidTopology(DataSourceWrapper master, DataSourceWrapper replica,
                                                   GtidDatabaseDialect dialect, boolean recoveryEnabled, boolean autoFailback) {
        try {
            DatabaseDialect replicaDialect = resolveDialect(replica);
            if (!(replicaDialect instanceof GtidDatabaseDialect)
                    || !dialect.getDatabaseType().equalsIgnoreCase(replicaDialect.getDatabaseType())) {
                throw new SQLException("GTID replication requires matching database types");
            }
            // A known reverse channel identifies the current writer, even while its replica is
            // advancing. Rejoin checks the positions again before changing any replication state.
            if (GtidReplication.hasSource(master, replica, dialect)
                    && !GtidReplication.hasSource(replica, master, dialect)
                    && !isGtidReadOnly(replica, dialect)) {
                setReadOnly(master, dialect, true);
                currentMasterId = replica.getId();
                runtimeStateManager.markSlavePromoted(replica.getId(), master.getId(), "startup_gtid_existing_promoted_primary");
                if (recoveryEnabled) {
                    handleOriginalMasterRecovery(master.getId());
                    recoverOriginalMasterAndRestoreReplication("startup_gtid_existing_promoted_primary", autoFailback);
                }
                return;
            }
            String masterPosition = GtidReplication.readPosition(master, dialect);
            PositionOrder order = dialect.comparePositions(masterPosition, GtidReplication.readPosition(replica, dialect));
            switch (order) {
                case EQUAL:
                case AHEAD:
                    setReadOnly(replica, dialect, true);
                    promoteToMaster(master);
                    currentMasterId = master.getId();
                    GtidReplication.ChannelStatus channel = GtidReplication.rejoin(replica, master, dialect,
                            replicationUser, replicationPassword) ? readGtidChannel(replica, master, dialect) : null;
                    if (channel != null && channel.running) {
                        runtimeStateManager.updateSlaveStatus(replica.getId(), true, true, channel.lagSeconds, "startup_gtid_replication_healthy");
                        runtimeStateManager.markMasterActive(master.getId(), replica.getId(), "startup_gtid_master_confirmed");
                    } else {
                        registry.updateSlaveAvailability(replica.getId(), false);
                        runtimeStateManager.markSlaveReadsFallbackToMaster(master.getId(), "startup_gtid_waiting_for_replication");
                    }
                    break;
                case BEHIND:
                    setReadOnly(master, dialect, true);
                    // Recheck after fencing the configured primary to catch writes made during discovery.
                    PositionOrder fencedOrder = dialect.comparePositions(GtidReplication.readPosition(master, dialect),
                            GtidReplication.readPosition(replica, dialect));
                    if (fencedOrder != PositionOrder.BEHIND && fencedOrder != PositionOrder.EQUAL) {
                        runtimeStateManager.markDegraded("startup_gtid_positions_changed");
                        return;
                    }
                    if (promoteStartupSlaveToPrimary(replica, master.getId(), "startup_gtid_replica_ahead") && recoveryEnabled) {
                        handleOriginalMasterRecovery(master.getId());
                        recoverOriginalMasterAndRestoreReplication("startup_gtid_replica_ahead", autoFailback);
                    }
                    break;
                default:
                    runtimeStateManager.markDegraded("startup_gtid_" + order.name());
                    log.warn("GTID 位点未知或分叉，跳过自动角色切换: master={}, replica={}, decision={}",
                            master.getId(), replica.getId(), order);
            }
        } catch (SQLException | IllegalArgumentException ex) {
            runtimeStateManager.markDegraded("startup_gtid_reconcile_failed");
            log.warn("GTID 启动编排失败: {}", ex.getMessage());
        }
    }

    private boolean recoverGtidPrimary(DataSourceWrapper original, DataSourceWrapper active, GtidDatabaseDialect dialect,
                                       String reason, boolean autoFailback) {
        ClusterStatus stage = runtimeStateManager.current().getStatus();
        try {
            if (!ClusterStatus.RESTORING_REPLICATION.equals(stage)) {
                setReadOnly(original, dialect, true);
                if (!GtidReplication.rejoin(original, active, dialect, replicationUser, replicationPassword)) {
                    return false;
                }
                moveToRecoveryStage(ClusterStatus.CATCHING_UP_ORIGINAL_MASTER, original.getId(), active.getId(), reason);
                // Catch up to an initial snapshot while the current primary still accepts writes.
                GtidReplication.ChannelStatus caughtUp = waitForGtidPosition(original, active, dialect, false);
                if (caughtUp == null) {
                    return false;
                }
                if (!autoFailback) {
                    completeGtidRecovery(active, original, caughtUp, reason + "_promoted_primary_retained");
                    return true;
                }
                setReadOnly(active, dialect, true);
                boolean promoted = false;
                try {
                    // Capture the final position only after fencing writes on the temporary primary.
                    if (waitForGtidPosition(original, active, dialect, true) == null) {
                        return false;
                    }
                    moveToRecoveryStage(ClusterStatus.RESTORING_ORIGINAL_MASTER, original.getId(), active.getId(), reason);
                    promoteToMaster(original);
                    promoted = true;
                } finally {
                    // If promotion failed, verify the original is fenced before reopening writes.
                    if (!promoted) {
                        setReadOnly(original, dialect, true);
                        setReadOnly(active, dialect, false);
                    }
                }
                currentMasterId = original.getId();
                runtimeStateManager.current().setActiveMasterId(original.getId());
                moveToRecoveryStage(ClusterStatus.RESTORING_REPLICATION, original.getId(), active.getId(), reason);
            }
            GtidReplication.ChannelStatus channel = GtidReplication.rejoin(active, original, dialect,
                    replicationUser, replicationPassword) ? readGtidChannel(active, original, dialect) : null;
            if (channel == null || !channel.running) {
                return false;
            }
            completeGtidRecovery(original, active, channel, reason + "_recovery_completed");
            return true;
        } catch (SQLException ex) {
            log.warn("GTID 原主恢复未完成: original={}, active={}, reason={}", original.getId(), active.getId(), ex.getMessage());
            return false;
        }
    }

    private boolean isGtidReadOnly(DataSourceWrapper node, GtidDatabaseDialect dialect) throws SQLException {
        try (Connection connection = node.getDataSource().getConnection()) {
            return GtidReplication.isGlobalReadOnly(connection, dialect);
        }
    }

    private void completeGtidRecovery(DataSourceWrapper writer, DataSourceWrapper replica,
                                      GtidReplication.ChannelStatus channel, String reason) {
        currentMasterId = writer.getId();
        runtimeStateManager.updateSlaveStatus(replica.getId(), true, true, channel.lagSeconds, reason);
        runtimeStateManager.markMasterActive(writer.getId(), replica.getId(), reason);
        if (coordinationService != null && coordinationService.isHealthy()) {
            coordinationService.broadcastFailbackCompleted(writer.getId(), replica.getId());
            coordinationService.setMasterStatus(DatasourceCoordinationService.STATUS_NORMAL);
        }
        log.info("GTID 恢复完成: writer={}, replica={}, reason={}", writer.getId(), replica.getId(), reason);
        log.info("GTID 复制状态（恢复完成）: source={}, replica={}, {}", writer.getId(), replica.getId(), channel);
    }

    private GtidReplication.ChannelStatus waitForGtidPosition(DataSourceWrapper replica, DataSourceWrapper source,
                                                             GtidDatabaseDialect dialect, boolean requireExact)
            throws SQLException {
        String target = GtidReplication.readPosition(source, dialect);
        if (dialect.comparePositions(target, target) == PositionOrder.UNKNOWN) {
            log.warn("无法确认 GTID 追赶目标位点: replica={}, source={}", replica.getId(), source.getId());
            return null;
        }
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(Math.max(0, catchupTimeoutSeconds));
        GtidReplication.ChannelStatus channel;
        do {
            channel = readGtidChannel(replica, source, dialect);
            if (channel.running) {
                PositionOrder order = dialect.comparePositions(GtidReplication.readPosition(replica, dialect), target);
                if (order == PositionOrder.EQUAL || (!requireExact && order == PositionOrder.AHEAD)) {
                    return channel;
                }
                if (order == PositionOrder.DIVERGED || (requireExact && order == PositionOrder.AHEAD)) {
                    log.warn("GTID 追赶结果不安全: replica={}, source={}, positionOrder={}", replica.getId(), source.getId(), order);
                    return null;
                }
            }
            long remainingMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            if (remainingMs <= 0L) {
                break;
            }
            try {
                Thread.sleep(Math.min(remainingMs, Math.max(1L, catchupCheckIntervalSeconds) * 1000L));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return null;
            }
        } while (System.nanoTime() < deadline);
        log.warn("GTID 追赶超时，保持当前主库: replica={}, source={}, channel={}", replica.getId(), source.getId(), channel);
        return null;
    }

    private GtidReplication.ChannelStatus readGtidChannel(DataSourceWrapper replica, DataSourceWrapper source,
                                                         GtidDatabaseDialect dialect) throws SQLException {
        try (Connection connection = replica.getDataSource().getConnection()) {
            return GtidReplication.readChannelStatus(connection, dialect, source);
        } catch (IllegalArgumentException ex) {
            throw new SQLException(ex.getMessage(), ex);
        }
    }

    /** Restore a known replica's global read-only setting without changing its replication channel. */
    synchronized boolean restoreGtidReplicaReadOnly(DataSourceWrapper replica, DataSourceWrapper expectedPrimary,
                                                  GtidDatabaseDialect dialect) {
        // Use the same lock as promotion and failback, and recheck roles after acquiring it.
        ClusterRuntimeState state = runtimeStateManager == null ? null : runtimeStateManager.current();
        if (state == null || state.isRecoveryInProgress() || !expectedPrimary.isAvailable()
                || !expectedPrimary.getId().equals(state.getActiveMasterId())
                || replica.getId().equals(state.getActiveMasterId())
                || replica.getId().equals(state.getPendingOriginalMasterId())) {
            return false;
        }
        try {
            setReadOnly(replica, dialect, true);
            log.info("已恢复从库全局只读: replica={}", replica.getId());
            return true;
        } catch (SQLException ex) {
            log.warn("无法恢复从库全局只读，暂停从读: replica={}, sqlState={}, errorCode={}; 请检查管理账号权限",
                    replica.getId(), ex.getSQLState(), ex.getErrorCode());
            return false;
        }
    }

    private void setReadOnly(DataSourceWrapper node, GtidDatabaseDialect dialect, boolean readOnly) throws SQLException {
        try (Connection connection = node.getDataSource().getConnection(); Statement statement = connection.createStatement()) {
            if (readOnly) {
                enableReadOnlyStatements(statement, dialect);
                if (!GtidReplication.isGlobalReadOnly(connection, dialect)) {
                    throw new SQLException("Replica global read-only setting was not enabled");
                }
            } else {
                disableReadOnlyStatements(statement, dialect);
            }
        }
    }

    private void finishOriginalMasterRecovery(String reason,
                                              DataSourceWrapper originalMaster,
                                              DataSourceWrapper promotedMaster) {
        String originalMasterId = safeNodeId(originalMaster);
        String promotedMasterId = safeNodeId(promotedMaster);
        currentMasterId = originalMasterId;
        runtimeStateManager.markMasterActive(originalMasterId, promotedMasterId, reason + "_recovery_completed");
        if (coordinationService != null) {
            if (resolveDialect(originalMaster) instanceof GtidDatabaseDialect) {
                coordinationService.broadcastFailbackCompleted(originalMasterId, promotedMasterId);
            }
            coordinationService.setMasterStatus(DatasourceCoordinationService.STATUS_NORMAL);
        }
        logReplicationStatus(promotedMaster, "原主恢复后从库");
        log.info("原主恢复闭环完成: originalMaster={}, slave={}", originalMasterId, promotedMasterId);
    }

    public synchronized boolean restoreReplicationToPrimary(DataSourceWrapper primaryNode,
                                                            DataSourceWrapper rejoinNode,
                                                            String reason) {
        if (primaryNode == null || rejoinNode == null) {
            log.warn("恢复复制关系缺少节点: primaryNode={}, rejoinNode={}", safeNodeId(primaryNode), safeNodeId(rejoinNode));
            return false;
        }
        String primaryNodeId = primaryNode.getId();
        String rejoinNodeId = rejoinNode.getId();
        currentMasterId = primaryNodeId;
        moveToRecoveryStage(ClusterStatus.RESTORING_REPLICATION, primaryNodeId, rejoinNodeId, reason + "_replication_restoring");
        if (!restoreOriginalReplication(primaryNode, rejoinNode)) {
            log.warn("恢复复制关系未完成，保持恢复阶段: primaryNode={}, rejoinNode={}", primaryNodeId, rejoinNodeId);
            return false;
        }
        finishOriginalMasterRecovery(reason, primaryNode, rejoinNode);
        return true;
    }

    public synchronized boolean recoverMasterFromContaminatedSlave(String contaminatedSlaveId,
                                                                  String gtidDecision,
                                                                  String reason) {
        if (runtimeStateManager == null) {
            return false;
        }
        ClusterRuntimeState state = runtimeStateManager.current();
        if (state == null || ClusterStatus.SLAVE_PROMOTED.equals(state.getStatus())) {
            log.warn("当前处于从库升主窗口，跳过 GTID 污染恢复: slaveId={}", contaminatedSlaveId);
            return false;
        }
        DataSourceWrapper originalMaster = resolveCurrentMaster();
        if (originalMaster == null && state.getActiveMasterId() != null) {
            originalMaster = resolveNodeById(state.getActiveMasterId());
        }
        DataSourceWrapper contaminatedSlave = resolveNodeById(contaminatedSlaveId);
        if (originalMaster == null || contaminatedSlave == null) {
            log.warn("GTID 污染恢复缺少节点: master={}, slave={}",
                    safeNodeId(originalMaster),
                    contaminatedSlaveId);
            return false;
        }
        if (isBlank(replicationUser) || isBlank(replicationPassword)) {
            log.warn("未配置复制用户，无法执行 GTID 污染恢复: slaveId={}", contaminatedSlaveId);
            return false;
        }

        runtimeStateManager.markContamination(
                contaminatedSlave.getId(),
                true,
                true,
                gtidDecision,
                "gtid_extra_transactions_detected",
                reason + "_contamination_detected"
        );
        log.warn("检测到从库存在额外 GTID，开始主库追赶从库: master={}, slave={}, gtidDecision={}",
                originalMaster.getId(), contaminatedSlave.getId(), gtidDecision);

        DatabaseDialect dialect = resolveDialect(originalMaster);
        if (dialect == null || dialect.getStartReplicationSql() == null) {
            log.warn("当前方言不支持 GTID 污染恢复: master={}", originalMaster.getId());
            return false;
        }
        try (Connection connection = originalMaster.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            enableReadOnly(originalMaster, "GTID 污染恢复阶段已冻结主库写入");
            stopSlaveReplication(statement, dialect);
            String sql = dialect.buildChangeReplicationSourceSql(
                    dialect.getHostFromJdbcUrl(contaminatedSlave.getUrl()),
                    dialect.getPortFromJdbcUrl(contaminatedSlave.getUrl()),
                    replicationUser,
                    replicationPassword);
            statement.execute(sql);
            statement.execute(dialect.getStartReplicationSql());
            log.info("主库已开始从被污染从库追赶事务: master={}, slave={}", originalMaster.getId(), contaminatedSlave.getId());
        } catch (SQLException ex) {
            log.error("GTID 污染恢复启动失败: {}", ex.getMessage(), ex);
            disableReadOnly(originalMaster, "GTID 污染恢复失败，已恢复主库可写");
            return false;
        }

        waitForCatchupOnMaster(originalMaster);

        try (Connection connection = originalMaster.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            stopSlaveReplication(statement, dialect);
        } catch (SQLException ex) {
            log.debug("GTID 污染恢复结束时停止主库复制失败: {}", ex.getMessage());
        }
        disableReadOnly(originalMaster, "GTID 污染恢复完成，主库已恢复可写");
        restoreOriginalReplication(originalMaster, contaminatedSlave);
        logReplicationStatus(contaminatedSlave, "gtid_contamination_recovery");
        runtimeStateManager.markMasterActive(originalMaster.getId(), contaminatedSlave.getId(), reason + "_consistency_restored");
        log.warn("GTID 污染恢复完成: master={}, slave={}", originalMaster.getId(), contaminatedSlave.getId());
        return true;
    }

    private void executeFailover(String failedMasterId, DataSourceWrapper newMaster) {
        boolean orderedGtid = resolveDialect(newMaster) instanceof GtidDatabaseDialect;
        String oldMasterId = orderedGtid || currentMasterId == null ? failedMasterId : currentMasterId;
        try {
            promoteToMaster(newMaster);
            // The failed MariaDB node will be fenced and rejoined on recovery. Do not
            // wait for its connection pool before routing traffic to the new primary.
            if (!orderedGtid) {
                notifySlavesNewMaster(newMaster);
            }
            currentMasterId = newMaster.getId();
            if (demoteOnFailover && !orderedGtid) {
                demoteToSlave(failedMasterId);
            }
            if (runtimeStateManager != null) {
                runtimeStateManager.markSlavePromoted(newMaster.getId(), failedMasterId, "master_failure_detected");
            }
            if (coordinationService != null) {
                coordinationService.broadcastFailoverEvent(oldMasterId, newMaster.getId());
                if (resolveDialect(newMaster) instanceof GtidDatabaseDialect) {
                    coordinationService.setMasterStatus(DatasourceCoordinationService.STATUS_SLAVE_PROMOTED);
                }
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
        DatabaseDialect dialect = resolveDialect(slave);
        if (dialect instanceof GtidDatabaseDialect) {
            if (!GtidReplication.rejoin(slave, newMaster, (GtidDatabaseDialect) dialect, replicationUser, replicationPassword)) {
                throw new SQLException("GTID replica cannot safely follow the selected primary");
            }
            return;
        }
        try (Connection conn = slave.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
            stopSlaveReplication(stmt, dialect);
            if (replicationUser != null && !replicationUser.isEmpty() && dialect != null && dialect.getStartReplicationSql() != null) {
                String sql = dialect.buildChangeReplicationSourceSql(
                        dialect.getHostFromJdbcUrl(newMaster.getUrl()), dialect.getPortFromJdbcUrl(newMaster.getUrl()), replicationUser, replicationPassword);
                stmt.execute(sql);
                stmt.execute(dialect.getStartReplicationSql());
            }
            enableReadOnlyStatements(stmt, dialect);
        }
    }

    private void promoteToMaster(DataSourceWrapper newMaster) throws SQLException {
        DatabaseDialect dialect = resolveDialect(newMaster);
        try (Connection conn = newMaster.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
            if (isPostgreSqlDialect(dialect) && isPostgreSqlPrimary(stmt)) {
                log.info("PostgreSQL 节点已处于主库角色，跳过重复提升: nodeId={}", newMaster.getId());
                disableReadOnlyStatements(stmt, dialect);
                return;
            }
            if (dialect instanceof GtidDatabaseDialect) {
                GtidDatabaseDialect gtidDialect = (GtidDatabaseDialect) dialect;
                String position = GtidReplication.readPosition(stmt, gtidDialect);
                if (gtidDialect.comparePositions(position, position) == PositionOrder.UNKNOWN) {
                    throw new SQLException("Cannot promote a node with an unknown GTID position");
                }
                GtidReplication.detach(stmt, gtidDialect);
            } else {
                stopSlaveReplication(stmt, dialect);
            }
            executeIfPresent(stmt, dialect == null ? null : dialect.getPromoteToPrimarySql());
            if (isPostgreSqlDialect(dialect) && !waitForPostgreSqlPrimary(stmt)) {
                throw new SQLException("PostgreSQL 升主超时，节点仍处于 recovery 状态");
            }
            disableReadOnlyStatements(stmt, dialect);
        }
    }

    private boolean isPostgreSqlPrimary(Statement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery("SELECT pg_is_in_recovery()")) {
            return rs.next() && !rs.getBoolean(1);
        }
    }

    private boolean waitForPostgreSqlPrimary(Statement statement) throws SQLException {
        long deadline = System.currentTimeMillis() + Math.max(10L, catchupTimeoutSeconds) * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (isPostgreSqlPrimary(statement)) {
                return true;
            }
            try {
                Thread.sleep(Math.max(100L, catchupCheckIntervalSeconds * 1000L));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return isPostgreSqlPrimary(statement);
    }

    private void demoteToSlave(String demotedMasterId) {
        DataSourceWrapper demoted = registry.getMaster(demotedMasterId);
        DataSourceWrapper currentMaster = resolveCurrentMaster();
        if (demoted == null || currentMaster == null) {
            return;
        }
        DatabaseDialect dialect = resolveDialect(demoted);
        if (dialect instanceof GtidDatabaseDialect) {
            try {
                GtidReplication.rejoin(demoted, currentMaster, (GtidDatabaseDialect) dialect, replicationUser, replicationPassword);
            } catch (SQLException ex) {
                log.warn("GTID 原主降级未完成: nodeId={}, reason={}", demotedMasterId, ex.getMessage());
            }
            return;
        }
        try (Connection conn = demoted.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
            enableReadOnlyStatements(stmt, dialect);
            if (replicationUser != null && !replicationUser.isEmpty() && dialect != null && dialect.getStartReplicationSql() != null) {
                String sql = dialect.buildChangeReplicationSourceSql(
                        dialect.getHostFromJdbcUrl(currentMaster.getUrl()), dialect.getPortFromJdbcUrl(currentMaster.getUrl()), replicationUser, replicationPassword);
                stopSlaveReplication(stmt, dialect);
                stmt.execute(sql);
                stmt.execute(dialect.getStartReplicationSql());
            }
        } catch (SQLException ex) {
            log.error("降级节点 {} 失败: {}", demotedMasterId, ex.getMessage());
        }
    }

    private boolean recoverMasterAsSlave(DataSourceWrapper originalMaster, DataSourceWrapper promotedMaster) {
        log.info("开始执行原主恢复为从库: originalMaster={}, promotedMaster={}", originalMaster.getId(), promotedMaster.getId());
        if (isBlank(replicationUser) || isBlank(replicationPassword)) {
            log.info("未配置复制用户，跳过原主恢复为从库");
            return false;
        }
        DatabaseDialect dialect = resolveDialect(originalMaster);
        if (dialect == null) {
            log.warn("原主恢复为从库失败: originalMaster={}, reason=dialect_not_found", originalMaster.getId());
            return false;
        }
        if (dialect.getStartReplicationSql() == null) {
            DatabaseRecoveryHandler recoveryHandler = resolveRecoveryHandler(dialect);
            if (recoveryHandler != null) {
                log.info("原主恢复为从库进入方言恢复处理器: originalMaster={}, promotedMaster={}, dialect={}",
                        originalMaster.getId(),
                        promotedMaster.getId(),
                        dialect.getDatabaseType());
                boolean recovered = recoveryHandler.recoverMasterAsSlave(buildRecoveryContext(originalMaster, promotedMaster, dialect));
                log.info("原主恢复为从库方言处理器执行完成: originalMaster={}, promotedMaster={}, recovered={}",
                        originalMaster.getId(),
                        promotedMaster.getId(),
                        recovered);
                return recovered;
            }
            log.info("当前方言不支持原主恢复为从库: nodeId={}", originalMaster.getId());
            return false;
        }
        try (Connection connection = originalMaster.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            stopSlaveReplication(statement, dialect);
            enableReadOnly(originalMaster, "原主恢复阶段已启用只读模式");
            String sql = dialect.buildChangeReplicationSourceSql(
                    dialect.getHostFromJdbcUrl(promotedMaster.getUrl()),
                    dialect.getPortFromJdbcUrl(promotedMaster.getUrl()),
                    replicationUser,
                    replicationPassword);
            statement.execute(sql);
            statement.execute(dialect.getStartReplicationSql());
            log.info("原主已开始作为从库追赶临时主库: originalMaster={}, promotedMaster={}", originalMaster.getId(), promotedMaster.getId());
            return true;
        } catch (SQLException ex) {
            log.warn("原主恢复为从库失败: {}", ex.getMessage());
            return false;
        }
    }

    private void waitForCatchupOnMaster(DataSourceWrapper originalMaster) {
        log.info("开始等待原主追赶完成: nodeId={}, timeoutSeconds={}, checkIntervalSeconds={}",
                originalMaster.getId(),
                catchupTimeoutSeconds,
                catchupCheckIntervalSeconds);
        int waited = 0;
        while (waited < catchupTimeoutSeconds) {
            try {
                Thread.sleep(catchupCheckIntervalSeconds * 1000L);
                waited += catchupCheckIntervalSeconds;
                DatabaseDialect dialect = resolveDialect(originalMaster);
                if (dialect == null || dialect.getShowReplicaStatusSql() == null) {
                    log.warn("原主追赶状态检查提前结束: nodeId={}, reason=replica_status_sql_not_supported", originalMaster.getId());
                    return;
                }
                try (Connection connection = originalMaster.getDataSource().getConnection();
                     Statement statement = connection.createStatement();
                     ResultSet rs = statement.executeQuery(dialect.getShowReplicaStatusSql())) {
                    if (!rs.next()) {
                        continue;
                    }
                    String ioRunning = getNullableColumn(rs, dialect.getReplicaIoRunningColumn());
                    String sqlRunning = getNullableColumn(rs, dialect.getReplicaSqlRunningColumn());
                    String lag = getNullableColumn(rs, dialect.getReplicaLagSecondsColumn());
                    String replayPosition = getNullableColumn(rs, dialect.getExecutedPositionColumn());
                    if (isPostgreSqlDialect(dialect)) {
                        String currentMasterPosition = readCurrentMasterPosition(dialect);
                        boolean replicaStreaming = dialect.isReplicaRunning(ioRunning, sqlRunning);
                        boolean caughtUp = replicaStreaming
                                && !isBlank(replayPosition)
                                && replayPosition.equals(currentMasterPosition);
                        if (caughtUp) {
                            log.info("原主追赶完成: nodeId={}, replayPosition={}, replicaIoState={}, replicaSqlState={}, lag={}s",
                                    originalMaster.getId(), replayPosition, ioRunning, sqlRunning,
                                    lag == null ? "UNKNOWN" : lag);
                            return;
                        }
                        log.info("原主追赶未完成: nodeId={}, replayPosition={}, currentMasterPosition={}, replicaStreaming={}, replicaIoState={}, replicaSqlState={}, lag={}s",
                                originalMaster.getId(),
                                replayPosition == null ? "UNKNOWN" : replayPosition,
                                currentMasterPosition == null ? "UNKNOWN" : currentMasterPosition,
                                Boolean.valueOf(replicaStreaming),
                                ioRunning == null ? "UNKNOWN" : ioRunning,
                                sqlRunning == null ? "UNKNOWN" : sqlRunning,
                                lag == null ? "UNKNOWN" : lag);
                        continue;
                    }
                    boolean caughtUp = dialect.isReplicaRunning(ioRunning, sqlRunning)
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

    private boolean isPostgreSqlDialect(DatabaseDialect dialect) {
        return dialect != null && "postgresql".equalsIgnoreCase(dialect.getDatabaseType());
    }

    private boolean isPostgreSqlPrimary(DataSourceWrapper wrapper) {
        return readPostgreSqlRecoveryMode(wrapper, false);
    }

    private boolean isPostgreSqlStandby(DataSourceWrapper wrapper) {
        return readPostgreSqlRecoveryMode(wrapper, true);
    }

    private boolean readPostgreSqlRecoveryMode(DataSourceWrapper wrapper, boolean expectedRecoveryMode) {
        if (wrapper == null || !isPostgreSqlDialect(resolveDialect(wrapper))) {
            return false;
        }
        try (Connection connection = wrapper.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT pg_is_in_recovery()")) {
            return rs.next() && rs.getBoolean(1) == expectedRecoveryMode;
        } catch (SQLException ex) {
            log.debug("读取 PostgreSQL 节点角色失败: nodeId={}, reason={}", wrapper.getId(), ex.getMessage());
            return false;
        }
    }

    private String readCurrentMasterPosition(DatabaseDialect dialect) {
        DataSourceWrapper currentMaster = resolveCurrentMaster();
        if (currentMaster == null || dialect == null || isBlank(dialect.getShowPrimaryStatusSql())) {
            return null;
        }
        try (Connection connection = currentMaster.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(dialect.getShowPrimaryStatusSql())) {
            if (!rs.next()) {
                return null;
            }
            return getNullableColumn(rs, dialect.getExecutedPositionColumn());
        } catch (Exception ex) {
            log.debug("读取当前主库位置失败: nodeId={}, reason={}", currentMaster.getId(), ex.getMessage());
            return null;
        }
    }

    private boolean restoreOriginalMasterRole(DataSourceWrapper originalMaster) {
        log.info("开始恢复原主主库身份: nodeId={}", originalMaster.getId());
        DatabaseDialect dialect = resolveDialect(originalMaster);
        DatabaseRecoveryHandler recoveryHandler = resolveRecoveryHandler(dialect);
        if (recoveryHandler != null) {
            log.info("恢复原主主库身份进入方言恢复处理器: nodeId={}, dialect={}",
                    originalMaster.getId(),
                    safeDialectType(dialect));
            boolean restored = recoveryHandler.restoreOriginalMasterRole(buildRecoveryContext(originalMaster, resolveCurrentMaster(), dialect));
            log.info("恢复原主主库身份方言处理器执行完成: nodeId={}, restored={}", originalMaster.getId(), restored);
            return restored;
        }
        try {
            promoteToMaster(originalMaster);
            log.info("原主已恢复主库身份: nodeId={}", originalMaster.getId());
            return true;
        } catch (SQLException ex) {
            log.warn("恢复原主主角色失败: nodeId={}, reason={}", originalMaster.getId(), ex.getMessage());
            return false;
        }
    }

    private boolean restoreOriginalReplication(DataSourceWrapper originalMaster, DataSourceWrapper promotedMaster) {
        log.info("开始恢复原始复制关系: currentMaster={}, rejoinNode={}", originalMaster.getId(), promotedMaster.getId());
        DatabaseDialect promotedDialect = resolveDialect(promotedMaster);
        if (promotedDialect instanceof GtidDatabaseDialect) {
            try {
                return GtidReplication.rejoin(promotedMaster, originalMaster, (GtidDatabaseDialect) promotedDialect,
                        replicationUser, replicationPassword);
            } catch (SQLException ex) {
                log.warn("GTID 从库复制重挂失败: nodeId={}, reason={}", promotedMaster.getId(), ex.getMessage());
                return false;
            }
        }
        DatabaseRecoveryHandler recoveryHandler = resolveRecoveryHandler(promotedDialect);
        if (recoveryHandler != null) {
            boolean restored = recoveryHandler.restoreOriginalReplication(buildRecoveryContext(promotedMaster, originalMaster, promotedDialect));
            if (restored) {
                log.info("原始复制关系已通过方言恢复处理器恢复: currentMaster={}, rejoinNode={}", originalMaster.getId(), promotedMaster.getId());
                logReplicationStatus(promotedMaster, "dialect_failback_rejoin");
                return true;
            }
            if (isPostgreSqlDialect(promotedDialect)) {
                log.warn("PostgreSQL 方言恢复处理器未完成复制重挂，停止回退到通用 SQL 重挂: currentMaster={}, rejoinNode={}",
                        originalMaster.getId(),
                        promotedMaster.getId());
                return false;
            }
        }
        enableReadOnly(promotedMaster, "临时主已切回从库只读模式");
        try {
            reconfigureSlave(promotedMaster, originalMaster);
            log.info("原始复制关系已通过通用复制重挂恢复: currentMaster={}, rejoinNode={}", originalMaster.getId(), promotedMaster.getId());
            return true;
        } catch (SQLException ex) {
            log.warn("原从库重新挂回原主失败: {}", ex.getMessage());
            return false;
        }
    }

    public void handleSlaveFailure(String failedSlaveId) {
        registry.updateSlaveAvailability(failedSlaveId, false);
        if (runtimeStateManager != null) {
            runtimeStateManager.updateSlaveStatus(failedSlaveId, false, false, 0L, "slave_health_check_failed");
            fallbackSlaveReadsToMaster(failedSlaveId);
        }
        if (coordinationService != null) {
            coordinationService.broadcastSlaveStatus(failedSlaveId, false);
        }
        for (FailoverListener listener : listeners) {
            listener.onSlaveDown(failedSlaveId);
        }
    }

    public synchronized boolean recoverSlaveReplication(String recoveredSlaveId) {
        DataSourceWrapper recoveredSlave = registry.getSlave(recoveredSlaveId);
        DataSourceWrapper currentMaster = resolveActiveMaster();
        if (recoveredSlave == null) {
            DataSourceWrapper configuredMaster = registry.getMaster(recoveredSlaveId);
            if (resolveDialect(configuredMaster) instanceof GtidDatabaseDialect) {
                recoveredSlave = configuredMaster;
            }
        }
        if (recoveredSlave == null || currentMaster == null || recoveredSlave.getId().equals(currentMaster.getId())) {
            return false;
        }
        DatabaseDialect dialect = resolveDialect(recoveredSlave);
        if (dialect == null || dialect.getDatabaseType() == null) {
            return false;
        }
        if (dialect instanceof GtidDatabaseDialect || "mysql".equalsIgnoreCase(dialect.getDatabaseType())) {
            return restoreOriginalReplication(currentMaster, recoveredSlave);
        }
        if (!"postgresql".equalsIgnoreCase(dialect.getDatabaseType())) {
            return false;
        }

        DatabaseRecoveryHandler recoveryHandler = resolveRecoveryHandler(dialect);
        if (recoveryHandler == null) {
            log.warn("PostgreSQL 从库复制重建跳过: nodeId={}, reason=recovery_handler_not_configured", recoveredSlaveId);
            return false;
        }
        boolean rebuilt = rebuildPostgreSqlSlaveReplication(recoveredSlave, currentMaster, dialect, recoveryHandler);
        if (!rebuilt) {
            log.warn("PostgreSQL 从库复制重建失败: slave={}, primary={}", recoveredSlaveId, currentMaster.getId());
            return false;
        }
        log.info("PostgreSQL 从库复制重建已完成，等待后续健康检查确认流复制: slave={}, primary={}",
                recoveredSlaveId, currentMaster.getId());
        return true;
    }

    protected boolean rebuildPostgreSqlSlaveReplication(DataSourceWrapper recoveredSlave,
                                                        DataSourceWrapper currentMaster,
                                                        DatabaseDialect dialect,
                                                        DatabaseRecoveryHandler recoveryHandler) {
        return recoveryHandler.recoverReplicaFromPrimary(
                buildRecoveryContext(recoveredSlave, currentMaster, dialect)
        );
    }

    public void handleSlaveRecovery(String recoveredSlaveId) {
        registry.updateSlaveAvailability(recoveredSlaveId, true);
        if (runtimeStateManager != null) {
            runtimeStateManager.updateSlaveStatus(recoveredSlaveId, true, true, 0L, "slave_health_check_recovered");
            restoreSlaveReads(recoveredSlaveId);
        }
        if (coordinationService != null) {
            coordinationService.broadcastSlaveStatus(recoveredSlaveId, true);
        }
        for (FailoverListener listener : listeners) {
            listener.onSlaveUp(recoveredSlaveId);
        }
    }

    private void fallbackSlaveReadsToMaster(String failedSlaveId) {
        ClusterRuntimeState state = runtimeStateManager.current();
        if (state == null
                || !ClusterStatus.MASTER_ACTIVE.equals(state.getStatus())
                || !failedSlaveId.equals(state.getActiveReadNodeId())) {
            return;
        }
        DataSourceWrapper master = resolveActiveMaster();
        if (master != null && master.isAvailable()) {
            runtimeStateManager.markSlaveReadsFallbackToMaster(master.getId(), "slave_health_check_failed");
        }
    }

    private void restoreSlaveReads(String recoveredSlaveId) {
        ClusterRuntimeState state = runtimeStateManager.current();
        DataSourceWrapper master = resolveActiveMaster();
        if (state == null
                || master == null
                || recoveredSlaveId.equals(master.getId())
                || !ClusterStatus.MASTER_ACTIVE.equals(state.getStatus())
                || !state.isSlaveReadsBlocked()) {
            return;
        }
        runtimeStateManager.restoreSlaveReads(master.getId(), recoveredSlaveId, "slave_health_check_recovered");
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

    public void setRecoverySettings(DatabaseRecoverySettings recoverySettings) {
        this.recoverySettings = recoverySettings == null ? new DatabaseRecoverySettings() : recoverySettings;
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

    private DataSourceWrapper resolveActiveMaster() {
        if (runtimeStateManager != null && runtimeStateManager.current().getActiveMasterId() != null) {
            DataSourceWrapper activeMaster = resolveNodeById(runtimeStateManager.current().getActiveMasterId());
            if (activeMaster != null) {
                return activeMaster;
            }
        }
        DataSourceWrapper configuredMaster = resolveCurrentMaster();
        if (configuredMaster != null) {
            return configuredMaster;
        }
        return registry.getAvailableMasters().isEmpty() ? null : registry.getAvailableMasters().get(0);
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
        DatabaseDialect dialect = resolveDialect(slave);
        if (dialect == null || dialect.getShowReplicaStatusSql() == null) {
            return;
        }
        try (Connection connection = slave.getDataSource().getConnection();
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

    private void disableReadOnly(DataSourceWrapper wrapper, String successLog) {
        if (wrapper == null) {
            return;
        }
        DatabaseDialect dialect = resolveDialect(wrapper);
        try (Connection connection = wrapper.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            disableReadOnlyStatements(statement, dialect);
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
        try (Connection connection = wrapper.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            enableReadOnlyStatements(statement, dialect);
            log.info(successLog);
        } catch (SQLException ex) {
            log.debug("开启只读模式失败: nodeId={}, reason={}", wrapper.getId(), ex.getMessage());
        }
    }

    private void stopSlaveReplication(Statement stmt, DatabaseDialect dialect) {
        String stopReplicationSql = null;
        String resetReplicationSql = null;
        if (dialect != null) {
            stopReplicationSql = dialect.getStopReplicationSql();
            resetReplicationSql = dialect.getResetReplicationSql();
        }
        try {
            executeIfPresent(stmt, stopReplicationSql);
            executeIfPresent(stmt, resetReplicationSql);
        } catch (SQLException ignored) {
        }
    }

    private void enableReadOnlyStatements(Statement statement, DatabaseDialect dialect) throws SQLException {
        String enableReadOnlySql = null;
        String enableStrictReadOnlySql = null;
        if (dialect != null) {
            enableReadOnlySql = dialect.getEnableReadOnlySql();
            enableStrictReadOnlySql = dialect.getEnableStrictReadOnlySql();
        }
        executeIfPresent(statement, enableReadOnlySql);
        executeIfPresent(statement, enableStrictReadOnlySql);
    }

    private void disableReadOnlyStatements(Statement statement, DatabaseDialect dialect) throws SQLException {
        String disableReadOnlySql = null;
        String disableStrictReadOnlySql = null;
        if (dialect != null) {
            disableReadOnlySql = dialect.getDisableReadOnlySql();
            disableStrictReadOnlySql = dialect.getDisableStrictReadOnlySql();
        }
        executeIfPresent(statement, disableReadOnlySql);
        executeIfPresent(statement, disableStrictReadOnlySql);
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

    private ClusterStatus safeClusterStatus(ClusterRuntimeState state) {
        if (state == null) {
            return null;
        }
        return state.getStatus();
    }

    private String safeActiveMasterId(ClusterRuntimeState state) {
        if (state == null) {
            return null;
        }
        return state.getActiveMasterId();
    }

    private String safePendingOriginalMasterId(ClusterRuntimeState state) {
        if (state == null) {
            return null;
        }
        return state.getPendingOriginalMasterId();
    }

    private String safeDialectType(DatabaseDialect dialect) {
        if (dialect == null) {
            return null;
        }
        return dialect.getDatabaseType();
    }

    private String safeNodeId(DataSourceWrapper wrapper) {
        if (wrapper == null) {
            return null;
        }
        return wrapper.getId();
    }

    private boolean shouldKeepPromotedPrimaryAfterRecovery(DatabaseDialect dialect) {
        DatabaseRecoveryHandler recoveryHandler = resolveRecoveryHandler(dialect);
        return recoveryHandler != null
                && recoverySettings != null
                && recoverySettings.isKeepPromotedPrimary();
    }

    private DatabaseRecoveryHandler resolveRecoveryHandler(DatabaseDialect dialect) {
        if (dialect == null || recoverySettings == null) {
            return null;
        }
        for (DatabaseRecoveryHandler handler : ServiceLoader.load(DatabaseRecoveryHandler.class)) {
            if (handler.supports(dialect, recoverySettings)) {
                return handler;
            }
        }
        return null;
    }

    private DialectRecoveryContext buildRecoveryContext(DataSourceWrapper localNode,
                                                        DataSourceWrapper targetPrimary,
                                                        DatabaseDialect dialect) {
        return new DialectRecoveryContext(
                recoverySettings,
                localNode,
                targetPrimary,
                dialect,
                replicationUser,
                replicationPassword,
                catchupTimeoutSeconds
        );
    }

    private String extractUsername(DataSourceWrapper wrapper) {
        if (wrapper == null) {
            return null;
        }
        if (!isBlank(wrapper.getUsername())) {
            return wrapper.getUsername();
        }
        try (Connection connection = wrapper.getDataSource().getConnection()) {
            return connection.getMetaData().getUserName();
        } catch (Exception ex) {
            log.debug("读取节点用户名失败: nodeId={}, reason={}", wrapper.getId(), ex.getMessage());
            return null;
        }
    }

    private String extractPassword(DataSourceWrapper wrapper) {
        if (wrapper == null) {
            return null;
        }
        return wrapper.getPassword();
    }

    public interface FailoverListener {
        void onFailoverCompleted(String oldMasterId, String newMasterId);

        void onFailoverFailed(String failedMasterId, String reason);

        void onSlaveDown(String slaveId);

        void onSlaveUp(String slaveId);
    }
}