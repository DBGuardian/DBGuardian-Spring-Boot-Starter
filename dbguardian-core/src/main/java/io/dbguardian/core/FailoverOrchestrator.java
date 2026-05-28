package io.dbguardian.core;

import io.dbguardian.model.ClusterModel;
import io.dbguardian.runtime.ClusterRuntimeState;
import io.dbguardian.runtime.FailoverEvent;
import io.dbguardian.runtime.SlaveStatusEvent;
import io.dbguardian.spi.FailoverStrategy;

public class FailoverOrchestrator {

    private final CapabilityRegistry capabilityRegistry;
    private final ClusterStateMachine clusterStateMachine;
    private final TopologyRegistry topologyRegistry;

    public FailoverOrchestrator(CapabilityRegistry capabilityRegistry) {
        this(capabilityRegistry, new ClusterStateMachine(), new TopologyRegistry());
    }

    public FailoverOrchestrator(CapabilityRegistry capabilityRegistry,
                                ClusterStateMachine clusterStateMachine,
                                TopologyRegistry topologyRegistry) {
        this.capabilityRegistry = capabilityRegistry;
        this.clusterStateMachine = clusterStateMachine;
        this.topologyRegistry = topologyRegistry;
    }

    public void initialize(ClusterModel clusterModel) {
        topologyRegistry.replace(clusterModel);
        clusterStateMachine.initialize(topologyRegistry.getRuntimeState());
    }

    public ClusterRuntimeState getRuntimeState() {
        return clusterStateMachine.current();
    }

    public ClusterRuntimeState trigger(ClusterModel clusterModel, String failedNodeId) {
        initialize(clusterModel);
        for (FailoverStrategy strategy : capabilityRegistry.getFailoverStrategies()) {
            strategy.failover(clusterModel, failedNodeId);
        }
        FailoverEvent event = new FailoverEvent();
        event.setClusterId(clusterModel.getClusterId());
        event.setSourceNodeId(failedNodeId);
        event.setTargetNodeId(selectPromotedNodeId(clusterModel, failedNodeId));
        event.setReason("failover_triggered");
        event.setType(event.getTargetNodeId() == null ? io.dbguardian.runtime.FailoverEventType.DEGRADED_ENTERED : io.dbguardian.runtime.FailoverEventType.SLAVE_PROMOTED);
        ClusterRuntimeState state = clusterStateMachine.applyFailover(event);
        topologyRegistry.updateRuntimeState(state);
        return state;
    }

    public ClusterRuntimeState recordSlaveStatus(SlaveStatusEvent event) {
        ClusterRuntimeState state = clusterStateMachine.applySlaveStatus(event);
        topologyRegistry.updateRuntimeState(state);
        return state;
    }

    private String selectPromotedNodeId(ClusterModel clusterModel, String failedNodeId) {
        for (io.dbguardian.model.NodeModel node : clusterModel.getNodes()) {
            if (!node.isEnabled()) {
                continue;
            }
            if (node.getId() == null || node.getId().equals(failedNodeId)) {
                continue;
            }
            if ("slave".equalsIgnoreCase(node.getRole())) {
                return node.getId();
            }
        }
        return null;
    }
}