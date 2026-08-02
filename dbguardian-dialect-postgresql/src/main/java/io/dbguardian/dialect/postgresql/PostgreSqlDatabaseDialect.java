package io.dbguardian.dialect.postgresql;

import io.dbguardian.spi.DatabaseDialect;

public class PostgreSqlDatabaseDialect implements DatabaseDialect {

    public static final String DATABASE_TYPE = "postgresql";
    public static final String NAME = "PostgreSQL";
    public static final String DRIVER_CLASS_NAME = "org.postgresql.Driver";
    public static final String URL_PREFIX = "jdbc:postgresql://";
    public static final int DEFAULT_PORT = 5432;

    public static final String HEALTH_CHECK_SQL = "SELECT 1";
    public static final String IS_IN_RECOVERY_SQL = "SELECT pg_is_in_recovery()";
    public static final String CURRENT_WAL_LSN_SQL = "SELECT pg_current_wal_lsn()";
    public static final String LAST_WAL_REPLAY_LSN_SQL = "SELECT pg_last_wal_replay_lsn()";
    public static final String REPLICATION_LAG_SECONDS_SQL = "SELECT EXTRACT(EPOCH FROM now() - pg_last_xact_replay_timestamp())";
    public static final String PROMOTE_SQL = "SELECT pg_promote()";
    public static final String WAL_RECEIVER_STATUS_SQL = "SELECT status FROM pg_stat_wal_receiver";
    public static final String REPLICATION_CLIENTS_SQL = "SELECT application_name, client_addr, state, sync_state, replay_lsn FROM pg_stat_replication";
    public static final String REPLICA_STATUS_SQL = "SELECT CASE WHEN pg_is_in_recovery() THEN 'streaming' ELSE 'primary' END AS replica_io_running, CASE WHEN pg_is_in_recovery() THEN 'streaming' ELSE 'primary' END AS replica_sql_running, COALESCE(EXTRACT(EPOCH FROM now() - pg_last_xact_replay_timestamp()), 0)::bigint AS replay_lag_seconds, (CASE WHEN pg_is_in_recovery() THEN pg_last_wal_replay_lsn() ELSE pg_current_wal_lsn() END)::text AS executed_position";
    public static final String PRIMARY_STATUS_SQL = "SELECT (CASE WHEN pg_is_in_recovery() THEN pg_last_wal_replay_lsn() ELSE pg_current_wal_lsn() END)::text AS executed_position";
    public static final String ENABLE_READ_ONLY_SQL = "ALTER SYSTEM SET default_transaction_read_only = on";
    public static final String DISABLE_READ_ONLY_SQL = "ALTER SYSTEM SET default_transaction_read_only = off";
    public static final String RELOAD_CONFIG_SQL = "SELECT pg_reload_conf()";
    public static final String WAL_LSN_DIFF_SQL = "SELECT pg_wal_lsn_diff('%s', '%s')";

    public static final String COLUMN_PG_IS_IN_RECOVERY = "pg_is_in_recovery";
    public static final String COLUMN_PG_CURRENT_WAL_LSN = "pg_current_wal_lsn";
    public static final String COLUMN_PG_LAST_WAL_REPLAY_LSN = "pg_last_wal_replay_lsn";
    public static final String COLUMN_EXTRACT = "extract";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_REPLICA_IO_RUNNING = "replica_io_running";
    public static final String COLUMN_REPLICA_SQL_RUNNING = "replica_sql_running";
    public static final String COLUMN_REPLAY_LAG_SECONDS = "replay_lag_seconds";
    public static final String COLUMN_EXECUTED_POSITION = "executed_position";
    public static final String COLUMN_PG_PROMOTE = "pg_promote";

    public static final String WAL_RECEIVER_STREAMING_VALUE = "streaming";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDatabaseType() {
        return DATABASE_TYPE;
    }

    @Override
    public String getHealthCheckSql() {
        return HEALTH_CHECK_SQL;
    }

    @Override
    public String getDriverClassName() {
        return DRIVER_CLASS_NAME;
    }

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public String getStopReplicationSql() {
        return null;
    }

    @Override
    public String getResetReplicationSql() {
        return null;
    }

    @Override
    public String getShowReplicaStatusSql() {
        return REPLICA_STATUS_SQL;
    }

    @Override
    public String getShowPrimaryStatusSql() {
        return PRIMARY_STATUS_SQL;
    }

    @Override
    public String getEnableReadOnlySql() {
        return null;
    }

    @Override
    public String getEnableStrictReadOnlySql() {
        return null;
    }

    @Override
    public String getDisableReadOnlySql() {
        return null;
    }

    @Override
    public String getDisableStrictReadOnlySql() {
        return null;
    }

    @Override
    public String getPromoteToPrimarySql() {
        return PROMOTE_SQL;
    }

    @Override
    public String getReplicaIoRunningColumn() {
        return COLUMN_REPLICA_IO_RUNNING;
    }

    @Override
    public String getReplicaSqlRunningColumn() {
        return COLUMN_REPLICA_SQL_RUNNING;
    }

    @Override
    public String getReplicaLagSecondsColumn() {
        return COLUMN_REPLAY_LAG_SECONDS;
    }

    @Override
    public String getExecutedPositionColumn() {
        return COLUMN_EXECUTED_POSITION;
    }

    @Override
    public boolean isReplicaRunning(String ioRunning, String sqlRunning) {
        return isWalReceiverStreaming(ioRunning) || isWalReceiverStreaming(sqlRunning);
    }

    public static boolean isWalReceiverStreaming(String status) {
        return WAL_RECEIVER_STREAMING_VALUE.equalsIgnoreCase(status);
    }

    public static String buildWalLsnDiffSql(String currentLsn, String replayLsn) {
        return String.format(WAL_LSN_DIFF_SQL, currentLsn, replayLsn);
    }

    @Override
    public String getHostFromJdbcUrl(String jdbcUrl) {
        return extractHost(jdbcUrl);
    }

    @Override
    public int getPortFromJdbcUrl(String jdbcUrl) {
        return extractPort(jdbcUrl);
    }

    public static String extractHost(String jdbcUrl) {
        String hostPort = extractHostPort(jdbcUrl);
        int colon = hostPort.indexOf(':');
        return colon >= 0 ? hostPort.substring(0, colon) : hostPort;
    }

    public static int extractPort(String jdbcUrl) {
        String hostPort = extractHostPort(jdbcUrl);
        int colon = hostPort.indexOf(':');
        if (colon < 0) {
            return DEFAULT_PORT;
        }
        return Integer.parseInt(hostPort.substring(colon + 1));
    }

    private static String extractHostPort(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "";
        }
        String body = jdbcUrl.startsWith(URL_PREFIX) ? jdbcUrl.substring(URL_PREFIX.length()) : jdbcUrl;
        int slash = body.indexOf('/');
        return slash >= 0 ? body.substring(0, slash) : body;
    }
}