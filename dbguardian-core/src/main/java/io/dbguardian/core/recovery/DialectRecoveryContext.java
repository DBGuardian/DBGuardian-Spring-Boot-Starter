package io.dbguardian.core.recovery;

import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.spi.DatabaseDialect;

public class DialectRecoveryContext {

    private final DatabaseRecoverySettings settings;
    private final DataSourceWrapper localNode;
    private final DataSourceWrapper targetPrimary;
    private final DatabaseDialect dialect;
    private final String replicationUser;
    private final String replicationPassword;
    private final int catchupTimeoutSeconds;

    public DialectRecoveryContext(DatabaseRecoverySettings settings,
                                  DataSourceWrapper localNode,
                                  DataSourceWrapper targetPrimary,
                                  DatabaseDialect dialect,
                                  String replicationUser,
                                  String replicationPassword,
                                  int catchupTimeoutSeconds) {
        this.settings = settings;
        this.localNode = localNode;
        this.targetPrimary = targetPrimary;
        this.dialect = dialect;
        this.replicationUser = replicationUser;
        this.replicationPassword = replicationPassword;
        this.catchupTimeoutSeconds = catchupTimeoutSeconds;
    }

    public DatabaseRecoverySettings getSettings() {
        return settings;
    }

    public DataSourceWrapper getLocalNode() {
        return localNode;
    }

    public DataSourceWrapper getTargetPrimary() {
        return targetPrimary;
    }

    public DatabaseDialect getDialect() {
        return dialect;
    }

    public String getReplicationUser() {
        return replicationUser;
    }

    public String getReplicationPassword() {
        return replicationPassword;
    }

    public int getCatchupTimeoutSeconds() {
        return catchupTimeoutSeconds;
    }
}