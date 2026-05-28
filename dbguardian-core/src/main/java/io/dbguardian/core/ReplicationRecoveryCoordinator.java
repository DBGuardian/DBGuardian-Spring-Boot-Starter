package io.dbguardian.core;

import io.dbguardian.runtime.ClusterRuntimeState;
import io.dbguardian.runtime.ClusterStatus;
import io.dbguardian.runtime.NodeRuntimeState;

public class ReplicationRecoveryCoordinator {

    public boolean shouldRecover(ClusterRuntimeState runtimeState) {
        if (runtimeState == null) {
            return false;
        }
        return ClusterStatus.SLAVE_PROMOTED.equals(runtimeState.getStatus())
                || ClusterStatus.RECOVERING_ORIGINAL_MASTER.equals(runtimeState.getStatus())
                || ClusterStatus.CATCHING_UP_ORIGINAL_MASTER.equals(runtimeState.getStatus())
                || ClusterStatus.RESTORING_ORIGINAL_MASTER.equals(runtimeState.getStatus())
                || ClusterStatus.RESTORING_REPLICATION.equals(runtimeState.getStatus())
                || runtimeState.isRecoveryInProgress();
    }

    public boolean shouldStartOriginalMasterRecovery(ClusterRuntimeState runtimeState) {
        return runtimeState != null
                && ClusterStatus.RECOVERING_ORIGINAL_MASTER.equals(runtimeState.getStatus())
                && runtimeState.getPendingOriginalMasterId() != null
                && runtimeState.getActiveMasterId() != null;
    }

    public String resolveRecoverySource(ClusterRuntimeState runtimeState) {
        if (runtimeState == null) {
            return null;
        }
        if (runtimeState.getPendingOriginalMasterId() != null) {
            return runtimeState.getPendingOriginalMasterId();
        }
        if (runtimeState.getRecoverySourceNodeId() != null) {
            return runtimeState.getRecoverySourceNodeId();
        }
        return runtimeState.getActiveMasterId();
    }

    public String resolveRecoveryTarget(ClusterRuntimeState runtimeState) {
        if (runtimeState == null) {
            return null;
        }
        if (runtimeState.getRecoveryTargetNodeId() != null) {
            return runtimeState.getRecoveryTargetNodeId();
        }
        for (NodeRuntimeState nodeState : runtimeState.getNodes()) {
            if (nodeState.isPromoted()) {
                return nodeState.getNodeId();
            }
        }
        return runtimeState.getActiveMasterId();
    }

    public boolean shouldRebuildReplication(NodeRuntimeState nodeState) {
        return nodeState != null
                && nodeState.isAvailable()
                && !nodeState.isReplicationHealthy();
    }

    public boolean shouldWaitForCatchup(NodeRuntimeState nodeState) {
        return nodeState != null
                && nodeState.isRecovering()
                && nodeState.isCatchupRequired();
    }

    public boolean shouldRestoreOriginalMasterRole(ClusterRuntimeState runtimeState) {
        return runtimeState != null
                && ClusterStatus.CATCHING_UP_ORIGINAL_MASTER.equals(runtimeState.getStatus())
                && runtimeState.getPendingOriginalMasterId() != null
                && runtimeState.getActiveMasterId() != null;
    }
}