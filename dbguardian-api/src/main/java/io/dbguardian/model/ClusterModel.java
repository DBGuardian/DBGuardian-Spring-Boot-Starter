package io.dbguardian.model;

import java.util.ArrayList;
import java.util.List;

public class ClusterModel {

    private String clusterId = "default";
    private boolean enabled = true;
    private final List<NodeModel> nodes = new ArrayList<NodeModel>();

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<NodeModel> getNodes() {
        return nodes;
    }
}