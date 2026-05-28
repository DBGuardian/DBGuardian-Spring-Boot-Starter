package io.dbguardian.core;

import io.dbguardian.runtime.ClusterRuntimeState;
import io.dbguardian.runtime.DataSourceRole;
import io.dbguardian.runtime.NodeRuntimeState;

public class GtidConsistencyInspector {

    private long maxAllowedLagSeconds;

    public boolean hasReplicationRisk(NodeRuntimeState nodeState) {
        return nodeState != null
                && nodeState.isAvailable()
                && DataSourceRole.SLAVE.equals(nodeState.getActiveRole())
                && (!nodeState.isReplicationHealthy() || nodeState.getReplicationLagSeconds() > maxAllowedLagSeconds);
    }

    public boolean shouldBlockSlaveTraffic(ClusterRuntimeState runtimeState) {
        if (runtimeState == null) {
            return false;
        }
        for (NodeRuntimeState nodeState : runtimeState.getNodes()) {
            if (hasReplicationRisk(nodeState)) {
                return true;
            }
        }
        return false;
    }

    public String findBlockedSlaveId(ClusterRuntimeState runtimeState) {
        if (runtimeState == null) {
            return null;
        }
        for (NodeRuntimeState nodeState : runtimeState.getNodes()) {
            if (hasReplicationRisk(nodeState)) {
                return nodeState.getNodeId();
            }
        }
        return null;
    }

    public void setMaxAllowedLagSeconds(long maxAllowedLagSeconds) {
        this.maxAllowedLagSeconds = Math.max(0L, maxAllowedLagSeconds);
    }
}