package io.dbguardian.runtime;

public class SlaveStatusEvent {

    private String clusterId;
    private String nodeId;
    private boolean healthy;
    private boolean replicationHealthy = true;
    private long replicationLagSeconds;
    private String detail;
    private long observedAt = System.currentTimeMillis();

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public boolean isReplicationHealthy() {
        return replicationHealthy;
    }

    public void setReplicationHealthy(boolean replicationHealthy) {
        this.replicationHealthy = replicationHealthy;
    }

    public long getReplicationLagSeconds() {
        return replicationLagSeconds;
    }

    public void setReplicationLagSeconds(long replicationLagSeconds) {
        this.replicationLagSeconds = replicationLagSeconds;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public long getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(long observedAt) {
        this.observedAt = observedAt;
    }
}