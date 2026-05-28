package io.dbguardian.core;

import io.dbguardian.model.ClusterModel;
import io.dbguardian.model.NodeModel;
import io.dbguardian.runtime.ClusterRuntimeState;
import io.dbguardian.runtime.ClusterStatus;
import io.dbguardian.runtime.DataSourceRole;
import io.dbguardian.runtime.NodeRuntimeState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TopologyRegistry {

    private ClusterModel clusterModel = new ClusterModel();
    private final List<NodeModel> nodes = new ArrayList<NodeModel>();
    private ClusterRuntimeState runtimeState = new ClusterRuntimeState();

    public synchronized void replace(ClusterModel clusterModel) {
        this.clusterModel = clusterModel == null ? new ClusterModel() : clusterModel;
        this.nodes.clear();
        this.nodes.addAll(this.clusterModel.getNodes());
        this.runtimeState = createRuntimeState(this.clusterModel);
    }

    public synchronized ClusterModel getClusterModel() {
        return clusterModel;
    }

    public synchronized List<NodeModel> getNodes() {
        return Collections.unmodifiableList(new ArrayList<NodeModel>(nodes));
    }

    public synchronized ClusterRuntimeState getRuntimeState() {
        return runtimeState;
    }

    public synchronized void updateRuntimeState(ClusterRuntimeState runtimeState) {
        if (runtimeState != null) {
            this.runtimeState = runtimeState;
        }
    }

    private ClusterRuntimeState createRuntimeState(ClusterModel clusterModel) {
        ClusterRuntimeState state = new ClusterRuntimeState();
        state.setClusterId(clusterModel.getClusterId());
        state.setStatus(ClusterStatus.MASTER_ACTIVE);

        for (NodeModel node : clusterModel.getNodes()) {
            NodeRuntimeState nodeState = new NodeRuntimeState();
            nodeState.setNodeId(node.getId());
            nodeState.setConfiguredRole(resolveRole(node.getRole()));
            nodeState.setActiveRole(resolveRole(node.getRole()));
            nodeState.setAvailable(node.isEnabled());
            nodeState.setHealthy(node.isEnabled());
            nodeState.setMasterRef(node.getMasterRef());
            state.putNodeState(nodeState);
            if (DataSourceRole.MASTER.equals(nodeState.getActiveRole()) && state.getActiveMasterId() == null) {
                state.setActiveMasterId(node.getId());
            }
            if (DataSourceRole.SLAVE.equals(nodeState.getActiveRole()) && state.getActiveReadNodeId() == null) {
                state.setActiveReadNodeId(node.getId());
            }
        }

        return state;
    }

    private DataSourceRole resolveRole(String role) {
        return "slave".equalsIgnoreCase(role) ? DataSourceRole.SLAVE : DataSourceRole.MASTER;
    }
}