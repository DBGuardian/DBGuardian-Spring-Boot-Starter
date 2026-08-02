package io.dbguardian.spi;

public interface DatabaseDialect {

    String getName();

    String getDatabaseType();

    String getHealthCheckSql();

    default String getDriverClassName() {
        return null;
    }

    default String buildChangeReplicationSourceSql(String sourceHost,
                                                   int sourcePort,
                                                   String replicationUser,
                                                   String replicationPassword) {
        return null;
    }

    default String getStartReplicationSql() {
        return null;
    }

    default String getStopReplicationSql() {
        return null;
    }

    default String getResetReplicationSql() {
        return null;
    }

    default String getShowReplicaStatusSql() {
        return null;
    }

    default String getShowPrimaryStatusSql() {
        return null;
    }

    default String getEnableReadOnlySql() {
        return null;
    }

    default String getEnableStrictReadOnlySql() {
        return null;
    }

    default String getDisableReadOnlySql() {
        return null;
    }

    default String getDisableStrictReadOnlySql() {
        return null;
    }

    default String getPromoteToPrimarySql() {
        return null;
    }

    default String getReplicaIoRunningColumn() {
        return null;
    }

    default String getReplicaSqlRunningColumn() {
        return null;
    }

    default String getReplicaLagSecondsColumn() {
        return null;
    }

    default String getExecutedPositionColumn() {
        return null;
    }

    default boolean isReplicaRunning(String ioRunning, String sqlRunning) {
        return false;
    }

    default String getHostFromJdbcUrl(String jdbcUrl) {
        return "";
    }

    default int getPortFromJdbcUrl(String jdbcUrl) {
        return 0;
    }
}