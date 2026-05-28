package io.dbguardian.runtime;

public class NodeRuntimeState {

    private String nodeId;
    private DataSourceRole configuredRole;
    private DataSourceRole activeRole;
    private boolean healthy;
    private boolean available = true;
    private boolean replicationHealthy = true;
    private boolean promoted;
    private boolean readOnly;
    private long replicationLagSeconds;
    private String masterRef;
    private String gtidSnapshot;
    private boolean catchupRequired;
    private boolean contaminated;
    private boolean recovering;
    private String lastError;
    private String lastReplicationError;
    private long lastHeartbeatAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public DataSourceRole getConfiguredRole() {
        return configuredRole;
    }

    public void setConfiguredRole(DataSourceRole configuredRole) {
        this.configuredRole = configuredRole;
    }

    public DataSourceRole getActiveRole() {
        return activeRole;
    }

    public void setActiveRole(DataSourceRole activeRole) {
        this.activeRole = activeRole;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
        touch();
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
        touch();
    }

    public boolean isReplicationHealthy() {
        return replicationHealthy;
    }

    public void setReplicationHealthy(boolean replicationHealthy) {
        this.replicationHealthy = replicationHealthy;
        touch();
    }

    public boolean isPromoted() {
        return promoted;
    }

    public void setPromoted(boolean promoted) {
        this.promoted = promoted;
        touch();
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        touch();
    }

    public long getReplicationLagSeconds() {
        return replicationLagSeconds;
    }

    public void setReplicationLagSeconds(long replicationLagSeconds) {
        this.replicationLagSeconds = replicationLagSeconds;
        touch();
    }

    public String getMasterRef() {
        return masterRef;
    }

    public void setMasterRef(String masterRef) {
        this.masterRef = masterRef;
        touch();
    }

    public String getGtidSnapshot() {
        return gtidSnapshot;
    }

    public void setGtidSnapshot(String gtidSnapshot) {
        this.gtidSnapshot = gtidSnapshot;
        touch();
    }

    public boolean isCatchupRequired() {
        return catchupRequired;
    }

    public void setCatchupRequired(boolean catchupRequired) {
        this.catchupRequired = catchupRequired;
        touch();
    }

    public boolean isContaminated() {
        return contaminated;
    }

    public void setContaminated(boolean contaminated) {
        this.contaminated = contaminated;
        touch();
    }

    public boolean isRecovering() {
        return recovering;
    }

    public void setRecovering(boolean recovering) {
        this.recovering = recovering;
        touch();
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
        touch();
    }

    public String getLastReplicationError() {
        return lastReplicationError;
    }

    public void setLastReplicationError(String lastReplicationError) {
        this.lastReplicationError = lastReplicationError;
        touch();
    }

    public long getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(long lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
        touch();
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
}