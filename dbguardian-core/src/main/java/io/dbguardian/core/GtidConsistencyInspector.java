package io.dbguardian.core;

import io.dbguardian.runtime.ClusterRuntimeState;
import io.dbguardian.runtime.DataSourceRole;
import io.dbguardian.runtime.NodeRuntimeState;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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

    public Set<String> detectExtraTransactions(String masterGtidSet, String slaveGtidSet) {
        if (isBlank(masterGtidSet) || isBlank(slaveGtidSet)) {
            return Collections.emptySet();
        }
        Set<String> masterTransactions = parseTransactions(masterGtidSet);
        Set<String> slaveTransactions = parseTransactions(slaveGtidSet);
        if (masterTransactions.isEmpty() || slaveTransactions.isEmpty()) {
            return Collections.emptySet();
        }
        slaveTransactions.removeAll(masterTransactions);
        return slaveTransactions;
    }

    public boolean hasExtraTransactions(String masterGtidSet, String slaveGtidSet) {
        return !detectExtraTransactions(masterGtidSet, slaveGtidSet).isEmpty();
    }

    public String describeExtraTransactions(Set<String> extraTransactions) {
        if (extraTransactions == null || extraTransactions.isEmpty()) {
            return "no_extra_gtid";
        }
        StringBuilder summary = new StringBuilder();
        int count = 0;
        for (String transaction : extraTransactions) {
            if (count++ >= 5) {
                break;
            }
            if (summary.length() > 0) {
                summary.append(",");
            }
            summary.append(transaction);
        }
        return summary.toString();
    }

    public void setMaxAllowedLagSeconds(long maxAllowedLagSeconds) {
        this.maxAllowedLagSeconds = Math.max(0L, maxAllowedLagSeconds);
    }

    private Set<String> parseTransactions(String gtidSet) {
        if (isBlank(gtidSet)) {
            return Collections.emptySet();
        }
        Set<String> transactions = new LinkedHashSet<String>();
        String[] parts = gtidSet.split(",");
        for (String part : parts) {
            String normalized = part == null ? "" : part.trim();
            if (!normalized.isEmpty()) {
                transactions.add(normalized);
            }
        }
        return transactions;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
