package io.dbguardian.model;

import java.util.LinkedHashSet;
import java.util.Set;

public class NodeModel {

    private String id;
    private String role;
    private String databaseType;
    private String jdbcUrl;
    private String username;
    private String password;
    private int weight = 100;
    private int priority = 100;
    private String masterRef;
    private boolean enabled = true;
    private final Set<String> tags = new LinkedHashSet<String>();

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

    public Set<String> getTags() {
        return tags;
    }
}