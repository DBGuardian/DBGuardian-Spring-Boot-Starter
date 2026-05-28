package io.dbguardian.runtime;

public enum ClusterStatus {
    MASTER_ACTIVE,
    SLAVE_PROMOTED,
    DEGRADED,
    RECOVERING_ORIGINAL_MASTER,
    CATCHING_UP_ORIGINAL_MASTER,
    RESTORING_ORIGINAL_MASTER,
    RESTORING_REPLICATION,
    CONTAMINATION_RECOVERY,
    VERIFYING_CONSISTENCY;

    public boolean isRecoveryStage() {
        return this == RECOVERING_ORIGINAL_MASTER
                || this == CATCHING_UP_ORIGINAL_MASTER
                || this == RESTORING_ORIGINAL_MASTER
                || this == RESTORING_REPLICATION
                || this == CONTAMINATION_RECOVERY
                || this == VERIFYING_CONSISTENCY;
    }

    public boolean shouldForceMasterReads() {
        return this == DEGRADED || isRecoveryStage() || this == SLAVE_PROMOTED;
    }
}
