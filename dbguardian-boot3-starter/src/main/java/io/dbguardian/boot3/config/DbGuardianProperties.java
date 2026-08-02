package io.dbguardian.boot3.config;

import io.dbguardian.model.ClusterModel;
import io.dbguardian.model.NodeModel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "dbguardian")
public class DbGuardianProperties {

    private boolean enabled = true;
    private String clusterId = "default";
    private final List<NodeProperties> nodes = new ArrayList<NodeProperties>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public List<NodeProperties> getNodes() {
        return nodes;
    }

    public ClusterModel toClusterModel() {
        ClusterModel model = new ClusterModel();
        model.setEnabled(enabled);
        model.setClusterId(clusterId);
        for (NodeProperties node : nodes) {
            NodeModel item = new NodeModel();
            item.setId(node.getId());
            item.setRole(node.getRole());
            item.setDatabaseType(node.getDatabaseType());
            item.setJdbcUrl(node.getJdbcUrl());
            item.setUsername(node.getUsername());
            item.setPassword(node.getPassword());
            item.setWeight(node.getWeight());
            item.setPriority(node.getPriority());
            item.setMasterRef(node.getMasterRef());
            item.setEnabled(node.isEnabled());
            item.getTags().addAll(node.getTags());
            model.getNodes().add(item);
        }
        return model;
    }

    public static class NodeProperties {
        private String id;
        private String role;
        private String databaseType = "mysql";
        private String jdbcUrl;
        private String username;
        private String password;
        private int weight = 100;
        private int priority = 100;
        private String masterRef;
        private boolean enabled = true;
        private final List<String> tags = new ArrayList<String>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getDatabaseType() {
            return databaseType;
        }

        public void setDatabaseType(String databaseType) {
            this.databaseType = databaseType;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public String getMasterRef() {
            return masterRef;
        }

        public void setMasterRef(String masterRef) {
            this.masterRef = masterRef;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getTags() {
            return tags;
        }
    }
}