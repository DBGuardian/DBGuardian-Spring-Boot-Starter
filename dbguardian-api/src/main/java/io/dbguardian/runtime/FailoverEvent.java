package io.dbguardian.runtime;

public class FailoverEvent {

    private String clusterId;
    private FailoverEventType type;
    private String sourceNodeId;
    private String targetNodeId;
    private String readNodeId;
    private String pendingOriginalMasterId;
    private boolean slaveReadsBlocked;
    private boolean contaminationDetected;
    private String replicationError;
    private String gtidDecision;
    private String reason;
    private long occurredAt = System.currentTimeMillis();

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public FailoverEventType getType() {
        return type;
    }

    public void setType(FailoverEventType type) {
        this.type = type;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public String getReadNodeId() {
        return readNodeId;
    }

    public void setReadNodeId(String readNodeId) {
        this.readNodeId = readNodeId;
    }

    public String getPendingOriginalMasterId() {
        return pendingOriginalMasterId;
    }

    public void setPendingOriginalMasterId(String pendingOriginalMasterId) {
        this.pendingOriginalMasterId = pendingOriginalMasterId;
    }

    public boolean isSlaveReadsBlocked() {
        return slaveReadsBlocked;
    }

    public void setSlaveReadsBlocked(boolean slaveReadsBlocked) {
        this.slaveReadsBlocked = slaveReadsBlocked;
    }

    public boolean isContaminationDetected() {
        return contaminationDetected;
    }

    public void setContaminationDetected(boolean contaminationDetected) {
        this.contaminationDetected = contaminationDetected;
    }

    public String getReplicationError() {
        return replicationError;
    }

    public void setReplicationError(String replicationError) {
        this.replicationError = replicationError;
    }

    public String getGtidDecision() {
        return gtidDecision;
    }

    public void setGtidDecision(String gtidDecision) {
        this.gtidDecision = gtidDecision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public long getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(long occurredAt) {
        this.occurredAt = occurredAt;
    }
}