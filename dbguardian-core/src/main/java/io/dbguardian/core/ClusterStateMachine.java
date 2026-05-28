package io.dbguardian.core;

import io.dbguardian.runtime.ClusterRuntimeState;
import io.dbguardian.runtime.ClusterStatus;
import io.dbguardian.runtime.DataSourceRole;
import io.dbguardian.runtime.FailoverEvent;
import io.dbguardian.runtime.FailoverEventType;
import io.dbguardian.runtime.NodeRuntimeState;
import io.dbguardian.runtime.SlaveStatusEvent;

import java.util.concurrent.atomic.AtomicReference;

public class ClusterStateMachine {

    private final AtomicReference<ClusterRuntimeState> currentState = new AtomicReference<ClusterRuntimeState>(new ClusterRuntimeState());

    public ClusterRuntimeState initialize(ClusterRuntimeState initialState) {
        ClusterRuntimeState state = initialState == null ? new ClusterRuntimeState() : initialState;
        currentState.set(state);
        return state;
    }

    public ClusterRuntimeState current() {
        return currentState.get();
    }

    public synchronized ClusterRuntimeState markMasterActive(String masterNodeId, String readNodeId, String reason) {
        ClusterRuntimeState state = current();
        state.setStatus(ClusterStatus.MASTER_ACTIVE);
        state.setRecoveryInProgress(false);
        state.setActiveMasterId(masterNodeId);
        state.setActiveReadNodeId(readNodeId);
        state.setRecoverySourceNodeId(null);
        state.setRecoveryTargetNodeId(null);
        state.setPendingOriginalMasterId(null);
        state.setSlaveReadsBlocked(false);
        state.setContaminationDetected(false);
        state.setLastReplicationError(null);
        state.setLastReason(reason);
        updateNodeRoles(state, masterNodeId, readNodeId, false);
        resetNodeRecoveryFlags(state);
        return state;
    }

    public synchronized ClusterRuntimeState markSlavePromoted(String promotedSlaveId, String reason) {
        ClusterRuntimeState state = current();
        state.setStatus(ClusterStatus.SLAVE_PROMOTED);
        state.setRecoveryInProgress(true);
        state.setActiveMasterId(promotedSlaveId);
        state.setActiveReadNodeId(promotedSlaveId);
        state.setRecoveryTargetNodeId(promotedSlaveId);
        state.setSlaveReadsBlocked(true);
        state.setLastReason(reason);
        updateNodeRoles(state, promotedSlaveId, promotedSlaveId, true);
        markNodeRecovering(state, promotedSlaveId, false, false);
        return state;
    }

    public synchronized ClusterRuntimeState markDegraded(String reason) {
        ClusterRuntimeState state = current();
        state.setStatus(ClusterStatus.DEGRADED);
        state.setRecoveryInProgress(false);
        state.setSlaveReadsBlocked(true);
        state.setLastReason(reason);
        return state;
    }

    public synchronized ClusterRuntimeState moveToRecoveryStage(ClusterStatus status,
                                                                String sourceNodeId,
                                                                String targetNodeId,
                                                                String reason) {
        ClusterRuntimeState state = current();
        state.setStatus(status);
        state.setRecoveryInProgress(status != null && status.isRecoveryStage());
        state.setRecoverySourceNodeId(sourceNodeId);
        state.setRecoveryTargetNodeId(targetNodeId);
        state.setSlaveReadsBlocked(status != null && status.shouldForceMasterReads());
        state.setLastReason(reason);
        markNodeRecovering(state, sourceNodeId, true, true);
        markNodeRecovering(state, targetNodeId, false, false);
        return state;
    }

    public synchronized ClusterRuntimeState markPendingOriginalMaster(String originalMasterNodeId) {
        ClusterRuntimeState state = current();
        state.setPendingOriginalMasterId(originalMasterNodeId);
        NodeRuntimeState nodeState = state.ensureNodeState(originalMasterNodeId);
        if (nodeState != null) {
            nodeState.setRecovering(true);
            nodeState.setCatchupRequired(true);
            nodeState.setReadOnly(true);
        }
        return state;
    }

    public synchronized ClusterRuntimeState markContamination(String nodeId,
                                                              boolean contaminated,
                                                              boolean slaveReadsBlocked,
                                                              String gtidDecision,
                                                              String replicationError,
                                                              String reason) {
        ClusterRuntimeState state = current();
        state.setContaminationDetected(contaminated);
        state.setSlaveReadsBlocked(slaveReadsBlocked);
        state.setLastGtidDecision(gtidDecision);
        state.setLastReplicationError(replicationError);
        state.setLastReason(reason);
        if (contaminated) {
            state.setStatus(ClusterStatus.CONTAMINATION_RECOVERY);
            state.setRecoveryInProgress(true);
        }
        NodeRuntimeState nodeState = state.ensureNodeState(nodeId);
        if (nodeState != null) {
            nodeState.setContaminated(contaminated);
            nodeState.setLastReplicationError(replicationError);
        }
        return state;
    }

    public synchronized ClusterRuntimeState applyFailover(FailoverEvent event) {
        if (event == null || event.getType() == null) {
            return current();
        }
        ClusterRuntimeState state = current();
        if (FailoverEventType.SLAVE_PROMOTED.equals(event.getType())) {
            markSlavePromoted(event.getTargetNodeId(), event.getReason());
            state.setPendingOriginalMasterId(event.getSourceNodeId());
            state.setRecoverySourceNodeId(event.getSourceNodeId());
            state.setRecoveryTargetNodeId(event.getTargetNodeId());
            state.setSlaveReadsBlocked(true);
            return state;
        }
        if (FailoverEventType.DEGRADED_ENTERED.equals(event.getType())) {
            return markDegraded(event.getReason());
        }
        if (FailoverEventType.MASTER_CONFIRMED.equals(event.getType()) || FailoverEventType.INITIALIZED.equals(event.getType())) {
            return markMasterActive(event.getTargetNodeId(), resolveReadNodeId(event), event.getReason());
        }
        if (FailoverEventType.ORIGINAL_MASTER_RECOVERY_STARTED.equals(event.getType())) {
            state.setPendingOriginalMasterId(event.getSourceNodeId());
            return moveToRecoveryStage(ClusterStatus.RECOVERING_ORIGINAL_MASTER, event.getSourceNodeId(), event.getTargetNodeId(), event.getReason());
        }
        if (FailoverEventType.ORIGINAL_MASTER_CATCHUP_STARTED.equals(event.getType())) {
            state.setPendingOriginalMasterId(event.getSourceNodeId());
            return moveToRecoveryStage(ClusterStatus.CATCHING_UP_ORIGINAL_MASTER, event.getSourceNodeId(), event.getTargetNodeId(), event.getReason());
        }
        if (FailoverEventType.ORIGINAL_MASTER_CATCHUP_COMPLETED.equals(event.getType())) {
            NodeRuntimeState nodeState = state.ensureNodeState(event.getSourceNodeId());
            if (nodeState != null) {
                nodeState.setCatchupRequired(false);
                nodeState.setRecovering(true);
            }
            return moveToRecoveryStage(ClusterStatus.RESTORING_ORIGINAL_MASTER, event.getSourceNodeId(), event.getTargetNodeId(), event.getReason());
        }
        if (FailoverEventType.ORIGINAL_MASTER_RESTORED.equals(event.getType())) {
            state.setActiveMasterId(event.getSourceNodeId());
            state.setActiveReadNodeId(resolveReadNodeId(event));
            return moveToRecoveryStage(ClusterStatus.RESTORING_REPLICATION, event.getSourceNodeId(), event.getTargetNodeId(), event.getReason());
        }
        if (FailoverEventType.REPLICATION_RESTORE_STARTED.equals(event.getType())) {
            return moveToRecoveryStage(ClusterStatus.RESTORING_REPLICATION, event.getSourceNodeId(), event.getTargetNodeId(), event.getReason());
        }
        if (FailoverEventType.REPLICATION_RESTORE_COMPLETED.equals(event.getType())
                || FailoverEventType.RECOVERY_COMPLETED.equals(event.getType())
                || FailoverEventType.REPLICATION_REBUILT.equals(event.getType())) {
            return markMasterActive(resolveMasterNodeId(event), resolveReadNodeId(event), event.getReason());
        }
        if (FailoverEventType.CONTAMINATION_DETECTED.equals(event.getType())
                || FailoverEventType.CONTAMINATION_RECOVERY_STARTED.equals(event.getType())) {
            return markContamination(event.getTargetNodeId(), true, true, event.getGtidDecision(), event.getReplicationError(), event.getReason());
        }
        if (FailoverEventType.CONSISTENCY_VERIFIED.equals(event.getType())) {
            state.setContaminationDetected(false);
            state.setSlaveReadsBlocked(false);
            state.setLastReplicationError(event.getReplicationError());
            state.setLastGtidDecision(event.getGtidDecision());
            state.setLastReason(event.getReason());
            return moveToRecoveryStage(ClusterStatus.VERIFYING_CONSISTENCY, event.getSourceNodeId(), event.getTargetNodeId(), event.getReason());
        }
        if (FailoverEventType.REDIS_STATE_REPLAYED.equals(event.getType()) || FailoverEventType.RECOVERY_STARTED.equals(event.getType())) {
            return moveToRecoveryStage(state.getStatus(), event.getSourceNodeId(), event.getTargetNodeId(), event.getReason());
        }
        return state;
    }

    public synchronized ClusterRuntimeState applySlaveStatus(SlaveStatusEvent event) {
        if (event == null || event.getNodeId() == null) {
            return current();
        }
        ClusterRuntimeState state = current();
        NodeRuntimeState nodeState = state.ensureNodeState(event.getNodeId());
        if (nodeState.getConfiguredRole() == null) {
            nodeState.setConfiguredRole(DataSourceRole.SLAVE);
        }
        if (nodeState.getActiveRole() == null) {
            nodeState.setActiveRole(DataSourceRole.SLAVE);
        }
        nodeState.setHealthy(event.isHealthy());
        nodeState.setAvailable(event.isHealthy());
        nodeState.setReplicationHealthy(event.isReplicationHealthy());
        nodeState.setReplicationLagSeconds(event.getReplicationLagSeconds());
        nodeState.setLastError(event.getDetail());
        nodeState.setLastReplicationError(event.getDetail());
        nodeState.setLastHeartbeatAt(event.getObservedAt());
        state.touch();
        return state;
    }

    private String resolveMasterNodeId(FailoverEvent event) {
        if (event != null && event.getSourceNodeId() != null) {
            return event.getSourceNodeId();
        }
        return current().getActiveMasterId();
    }

    private String resolveReadNodeId(FailoverEvent event) {
        if (event != null && event.getReadNodeId() != null) {
            return event.getReadNodeId();
        }
        if (event != null && event.getTargetNodeId() != null && !event.getTargetNodeId().equals(event.getSourceNodeId())) {
            return event.getTargetNodeId();
        }
        return current().getActiveReadNodeId();
    }

    private void updateNodeRoles(ClusterRuntimeState state, String masterNodeId, String readNodeId, boolean promoted) {
        for (NodeRuntimeState nodeState : state.getNodes()) {
            boolean masterNode = nodeState.getNodeId() != null && nodeState.getNodeId().equals(masterNodeId);
            nodeState.setActiveRole(masterNode ? DataSourceRole.MASTER : DataSourceRole.SLAVE);
            nodeState.setPromoted(masterNode && promoted);
            nodeState.setReadOnly(!masterNode);
            nodeState.setMasterRef(masterNode ? null : masterNodeId);
            nodeState.setRecovering(false);
            nodeState.setCatchupRequired(false);
            if (masterNode) {
                nodeState.setHealthy(true);
                nodeState.setAvailable(true);
            }
            if (nodeState.getNodeId() != null && nodeState.getNodeId().equals(readNodeId)) {
                state.setActiveReadNodeId(readNodeId);
            }
        }
        state.touch();
    }

    private void markNodeRecovering(ClusterRuntimeState state, String nodeId, boolean recovering, boolean catchupRequired) {
        NodeRuntimeState nodeState = state.ensureNodeState(nodeId);
        if (nodeState == null) {
            return;
        }
        nodeState.setRecovering(recovering);
        nodeState.setCatchupRequired(catchupRequired);
        if (recovering) {
            nodeState.setReadOnly(true);
        }
    }

    private void resetNodeRecoveryFlags(ClusterRuntimeState state) {
        for (NodeRuntimeState nodeState : state.getNodes()) {
            nodeState.setRecovering(false);
            nodeState.setCatchupRequired(false);
            nodeState.setContaminated(false);
        }
    }
}