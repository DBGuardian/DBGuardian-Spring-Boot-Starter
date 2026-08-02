package io.dbguardian.core.recovery;

import io.dbguardian.spi.DatabaseDialect;

public interface DatabaseRecoveryHandler {

    boolean supports(DatabaseDialect dialect, DatabaseRecoverySettings settings);

    boolean recoverMasterAsSlave(DialectRecoveryContext context);

    boolean restoreOriginalMasterRole(DialectRecoveryContext context);

    boolean restoreOriginalReplication(DialectRecoveryContext context);

    default boolean recoverReplicaFromPrimary(DialectRecoveryContext context) {
        return restoreOriginalReplication(context);
    }
}