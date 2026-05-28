package io.dbguardian.core;

import io.dbguardian.model.NodeModel;
import io.dbguardian.model.RoutingContext;
import io.dbguardian.runtime.ClusterRuntimeState;
import io.dbguardian.runtime.DataSourceRole;
import io.dbguardian.runtime.NodeRuntimeState;
import io.dbguardian.spi.RoutingPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RoutingEngine {

    private final CapabilityRegistry capabilityRegistry;

    public RoutingEngine(CapabilityRegistry capabilityRegistry) {
        this.capabilityRegistry = capabilityRegistry;
    }

    public NodeModel route(List<NodeModel> candidates, RoutingContext context) {
        return route(candidates, context, null);
    }

    public NodeModel route(List<NodeModel> candidates, RoutingContext context, ClusterRuntimeState runtimeState) {
        List<NodeModel> enabledCandidates = candidates.stream()
                .filter(NodeModel::isEnabled)
                .collect(Collectors.toList());
        List<NodeModel> runtimeCandidates = filterByRuntimeState(enabledCandidates, context, runtimeState);
        List<NodeModel> effectiveCandidates = runtimeCandidates.isEmpty() ? enabledCandidates : runtimeCandidates;

        for (RoutingPolicy policy : capabilityRegistry.getRoutingPolicies()) {
            NodeModel selected = policy.select(effectiveCandidates, context);
            if (selected != null) {
                return selected;
            }
        }
        return effectiveCandidates.isEmpty() ? null : effectiveCandidates.get(0);
    }

    private List<NodeModel> filterByRuntimeState(List<NodeModel> candidates,
                                                 RoutingContext context,
                                                 ClusterRuntimeState runtimeState) {
        if (runtimeState == null) {
            return candidates;
        }

        boolean readOperation = context != null && "read".equalsIgnoreCase(context.getOperation())
                && !context.isForceMaster()
                && !context.isReadOnlyTransaction();
        boolean forceMasterReads = runtimeState.getStatus() != null
                && runtimeState.getStatus().shouldForceMasterReads();
        String preferredNodeId = readOperation && !forceMasterReads && !runtimeState.isSlaveReadsBlocked()
                ? runtimeState.getActiveReadNodeId()
                : runtimeState.getActiveMasterId();
        DataSourceRole expectedRole = readOperation && !forceMasterReads && !runtimeState.isSlaveReadsBlocked()
                ? DataSourceRole.SLAVE
                : DataSourceRole.MASTER;

        List<NodeModel> filtered = new ArrayList<NodeModel>();
        for (NodeModel candidate : candidates) {
            NodeRuntimeState nodeState = runtimeState.getNodeState(candidate.getId());
            if (nodeState == null || !nodeState.isAvailable() || !nodeState.isHealthy()) {
                continue;
            }
            if (readOperation && (forceMasterReads || runtimeState.isSlaveReadsBlocked())
                    && !DataSourceRole.MASTER.equals(nodeState.getActiveRole())) {
                continue;
            }
            if (preferredNodeId != null && preferredNodeId.equals(candidate.getId())) {
                filtered.add(candidate);
                continue;
            }
            if (expectedRole.equals(nodeState.getActiveRole())) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }
}