package io.dbguardian.spring.runtime;

import io.dbguardian.core.ClusterStateMachine;
import io.dbguardian.runtime.ClusterRuntimeState;
import io.dbguardian.runtime.ClusterStatus;
import io.dbguardian.runtime.DataSourceRole;
import io.dbguardian.runtime.FailoverEvent;
import io.dbguardian.runtime.FailoverEventType;
import io.dbguardian.runtime.SlaveStatusEvent;

public class ClusterRuntimeStateManager {

    private final ClusterStateMachine stateMachine = new ClusterStateMachine();

    public ClusterRuntimeStateManager() {
        stateMachine.initialize(new ClusterRuntimeState());
    }

    public synchronized void initialize(String clusterId, String masterNodeId, String readNodeId) {
        ClusterRuntimeState state = current();
        state.setClusterId(clusterId == null ? "default" : clusterId);
        stateMachine.markMasterActive(masterNodeId, readNodeId, "startup_initialized");
    }

    public synchronized ClusterRuntimeState current() {
        return stateMachine.current();
    }

    public synchronized void markMasterActive(String masterNodeId, String readNodeId, String reason) {
        stateMachine.markMasterActive(masterNodeId, readNodeId, reason);
    }

    public synchronized void markSlavePromoted(String promotedNodeId, String reason) {
        stateMachine.markSlavePromoted(promotedNodeId, reason);
    }

    public synchronized void markSlavePromoted(String promotedNodeId, String pendingOriginalMasterId, String reason) {
        stateMachine.markSlavePromoted(promotedNodeId, reason);
        stateMachine.markPendingOriginalMaster(pendingOriginalMasterId);
    }

    public synchronized void markDegraded(String reason) {
        stateMachine.markDegraded(reason);
    }

    public synchronized void moveToRecoveryStage(ClusterStatus status,
                                                 String sourceNodeId,
                                                 String targetNodeId,
                                                 String reason) {
        stateMachine.moveToRecoveryStage(status, sourceNodeId, targetNodeId, reason);
    }

    public synchronized void markContamination(String nodeId,
                                               boolean contaminated,
                                               boolean slaveReadsBlocked,
                                               String gtidDecision,
                                               String replicationError,
                                               String reason) {
        stateMachine.markContamination(nodeId, contaminated, slaveReadsBlocked, gtidDecision, replicationError, reason);
    }

    public synchronized void applyEvent(FailoverEvent event) {
        stateMachine.applyFailover(event);
    }

    public synchronized void updateSlaveStatus(String slaveNodeId,
                                               boolean healthy,
                                               boolean replicationHealthy,
                                               long lagSeconds,
                                               String detail) {
        SlaveStatusEvent event = new SlaveStatusEvent();
        event.setClusterId(current().getClusterId());
        event.setNodeId(slaveNodeId);
        event.setHealthy(healthy);
        event.setReplicationHealthy(replicationHealthy);
        event.setReplicationLagSeconds(lagSeconds);
        event.setDetail(detail);
        stateMachine.applySlaveStatus(event);
    }

    public synchronized void replayStatusMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        String normalized = sanitizeMessage(message);
        if ("NORMAL".equalsIgnoreCase(normalized)) {
            stateMachine.markMasterActive(current().getActiveMasterId(), current().getActiveReadNodeId(), "redis_status_replayed");
            return;
        }
        if ("SLAVE_PROMOTED".equalsIgnoreCase(normalized)) {
            String promotedNodeId = current().getActiveReadNodeId() == null ? current().getActiveMasterId() : current().getActiveReadNodeId();
            stateMachine.markSlavePromoted(promotedNodeId, "redis_status_replayed");
            return;
        }
        if (normalized.startsWith("FAILOVER:")) {
            String[] parts = normalized.split(":");
            if (parts.length >= 3) {
                FailoverEvent event = new FailoverEvent();
                event.setClusterId(current().getClusterId());
                event.setType(FailoverEventType.SLAVE_PROMOTED);
                event.setSourceNodeId(parts[1]);
                event.setTargetNodeId(parts[2]);
                event.setPendingOriginalMasterId(parts[1]);
                event.setSlaveReadsBlocked(true);
                event.setReason("redis_failover_replayed");
                stateMachine.applyFailover(event);
            }
            return;
        }
        if (normalized.startsWith("RECOVERY_STAGE:")) {
            String[] parts = normalized.split(":", 5);
            if (parts.length >= 5) {
                FailoverEvent event = new FailoverEvent();
                event.setClusterId(current().getClusterId());
                event.setType(resolveRecoveryEventType(parts[1]));
                event.setSourceNodeId(emptyToNull(parts[2]));
                event.setTargetNodeId(emptyToNull(parts[3]));
                event.setReason(parts[4]);
                stateMachine.applyFailover(event);
            }
            return;
        }
        if (normalized.startsWith("CONTAMINATION:")) {
            String[] parts = normalized.split(":", 5);
            if (parts.length >= 3) {
                FailoverEvent event = new FailoverEvent();
                event.setClusterId(current().getClusterId());
                event.setType(FailoverEventType.CONTAMINATION_DETECTED);
                event.setTargetNodeId(parts[1]);
                event.setGtidDecision(parts.length >= 4 ? emptyToNull(parts[3]) : null);
                event.setReplicationError(parts.length >= 5 ? emptyToNull(parts[4]) : null);
                event.setReason(parts.length >= 3 ? parts[2] : "redis_contamination_replayed");
                stateMachine.applyFailover(event);
            }
            return;
        }
        if (normalized.startsWith("SLAVE_STATUS:")) {
            String[] parts = normalized.split(":");
            if (parts.length >= 3) {
                updateSlaveStatus(parts[1], Boolean.parseBoolean(parts[2]), Boolean.parseBoolean(parts[2]), 0L, "redis_status_replayed");
            }
        }
    }

    public synchronized String resolveLookupKey(boolean forceMaster, boolean readOperation) {
        ClusterRuntimeState state = current();
        if (forceMaster || state.getStatus().shouldForceMasterReads() || state.isSlaveReadsBlocked()) {
            return resolveActiveWriteLookupKey(state);
        }
        if (readOperation && state.getActiveReadNodeId() != null) {
            return resolveNodeRole(state.getActiveReadNodeId());
        }
        if (state.getActiveMasterId() != null) {
            return resolveNodeRole(state.getActiveMasterId());
        }
        return readOperation ? "slave" : "master";
    }

    private String resolveActiveWriteLookupKey(ClusterRuntimeState state) {
        if (state == null || state.getActiveMasterId() == null) {
            return "master";
        }
        if (current().getNodeState(state.getActiveMasterId()) == null) {
            return "master";
        }
        return DataSourceRole.SLAVE.equals(current().getNodeState(state.getActiveMasterId()).getActiveRole()) ? "slave" : "master";
    }

    private String resolveNodeRole(String nodeId) {
        if (nodeId == null) {
            return "master";
        }
        if (current().getNodeState(nodeId) == null) {
            return "master";
        }
        return DataSourceRole.SLAVE.equals(current().getNodeState(nodeId).getActiveRole()) ? "slave" : "master";
    }

    private String sanitizeMessage(String message) {
        String normalized = message == null ? "" : message.trim();
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() > 1) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.trim();
    }

    private FailoverEventType resolveRecoveryEventType(String stage) {
        if (stage == null) {
            return FailoverEventType.REDIS_STATE_REPLAYED;
        }
        String normalized = stage.trim().toUpperCase();
        if ("RECOVERING_ORIGINAL_MASTER".equals(normalized)) {
            return FailoverEventType.ORIGINAL_MASTER_RECOVERY_STARTED;
        }
        if ("CATCHING_UP_ORIGINAL_MASTER".equals(normalized)) {
            return FailoverEventType.ORIGINAL_MASTER_CATCHUP_STARTED;
        }
        if ("RESTORING_ORIGINAL_MASTER".equals(normalized)) {
            return FailoverEventType.ORIGINAL_MASTER_CATCHUP_COMPLETED;
        }
        if ("RESTORING_REPLICATION".equals(normalized)) {
            return FailoverEventType.REPLICATION_RESTORE_STARTED;
        }
        if ("VERIFYING_CONSISTENCY".equals(normalized)) {
            return FailoverEventType.CONSISTENCY_VERIFIED;
        }
        return FailoverEventType.REDIS_STATE_REPLAYED;
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim()) ? null : value.trim();
    }
}