package io.dbguardian.runtime;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClusterRuntimeState {

    private String clusterId = "default";
    private ClusterStatus status = ClusterStatus.MASTER_ACTIVE;
    private String activeMasterId;
    private String activeReadNodeId;
    private boolean degraded;
    private boolean recoveryInProgress;
    private String recoverySourceNodeId;
    private String recoveryTargetNodeId;
    private String pendingOriginalMasterId;
    private boolean slaveReadsBlocked;
    private boolean contaminationDetected;
    private String lastReplicationError;
    private String lastGtidDecision;
    private String lastReason;
    private long initializedAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();
    private final Map<String, NodeRuntimeState> nodeStates = new LinkedHashMap<String, NodeRuntimeState>();

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public ClusterStatus getStatus() {
        return status;
    }

    public void setStatus(ClusterStatus status) {
        this.status = status;
        this.degraded = ClusterStatus.DEGRADED.equals(status);
        this.recoveryInProgress = status != null && status.isRecoveryStage();
        touch();
    }

    public String getActiveMasterId() {
        return activeMasterId;
    }

    public void setActiveMasterId(String activeMasterId) {
        this.activeMasterId = activeMasterId;
        touch();
    }

    public String getActiveReadNodeId() {
        return activeReadNodeId;
    }

    public void setActiveReadNodeId(String activeReadNodeId) {
        this.activeReadNodeId = activeReadNodeId;
        touch();
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
        if (degraded) {
            this.status = ClusterStatus.DEGRADED;
        }
        touch();
    }

    public boolean isRecoveryInProgress() {
        return recoveryInProgress;
    }

    public void setRecoveryInProgress(boolean recoveryInProgress) {
        this.recoveryInProgress = recoveryInProgress;
        touch();
    }

    public String getRecoverySourceNodeId() {
        return recoverySourceNodeId;
    }

    public void setRecoverySourceNodeId(String recoverySourceNodeId) {
        this.recoverySourceNodeId = recoverySourceNodeId;
        touch();
    }

    public String getRecoveryTargetNodeId() {
        return recoveryTargetNodeId;
    }

    public void setRecoveryTargetNodeId(String recoveryTargetNodeId) {
        this.recoveryTargetNodeId = recoveryTargetNodeId;
        touch();
    }

    public String getPendingOriginalMasterId() {
        return pendingOriginalMasterId;
    }

    public void setPendingOriginalMasterId(String pendingOriginalMasterId) {
        this.pendingOriginalMasterId = pendingOriginalMasterId;
        touch();
    }

    public boolean isSlaveReadsBlocked() {
        return slaveReadsBlocked;
    }

    public void setSlaveReadsBlocked(boolean slaveReadsBlocked) {
        this.slaveReadsBlocked = slaveReadsBlocked;
        touch();
    }

    public boolean isContaminationDetected() {
        return contaminationDetected;
    }

    public void setContaminationDetected(boolean contaminationDetected) {
        this.contaminationDetected = contaminationDetected;
        touch();
    }

    public String getLastReplicationError() {
        return lastReplicationError;
    }

    public void setLastReplicationError(String lastReplicationError) {
        this.lastReplicationError = lastReplicationError;
        touch();
    }

    public String getLastGtidDecision() {
        return lastGtidDecision;
    }

    public void setLastGtidDecision(String lastGtidDecision) {
        this.lastGtidDecision = lastGtidDecision;
        touch();
    }

    public String getLastReason() {
        return lastReason;
    }

    public void setLastReason(String lastReason) {
        this.lastReason = lastReason;
        touch();
    }

    public long getInitializedAt() {
        return initializedAt;
    }

    public void setInitializedAt(long initializedAt) {
        this.initializedAt = initializedAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, NodeRuntimeState> getNodeStates() {
        return nodeStates;
    }

    public Collection<NodeRuntimeState> getNodes() {
        return nodeStates.values();
    }

    public NodeRuntimeState getNodeState(String nodeId) {
        return nodeStates.get(nodeId);
    }

    public NodeRuntimeState ensureNodeState(String nodeId) {
        if (nodeId == null) {
            return null;
        }
        NodeRuntimeState nodeState = nodeStates.get(nodeId);
        if (nodeState != null) {
            return nodeState;
        }
        nodeState = new NodeRuntimeState();
        nodeState.setNodeId(nodeId);
        nodeStates.put(nodeId, nodeState);
        touch();
        return nodeState;
    }

    public void putNodeState(NodeRuntimeState nodeState) {
        if (nodeState == null || nodeState.getNodeId() == null) {
            return;
        }
        nodeStates.put(nodeState.getNodeId(), nodeState);
        touch();
    }

    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
}