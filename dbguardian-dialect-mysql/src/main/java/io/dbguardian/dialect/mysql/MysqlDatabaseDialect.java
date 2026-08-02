package io.dbguardian.dialect.mysql;

import io.dbguardian.spi.DatabaseDialect;

public class MysqlDatabaseDialect implements DatabaseDialect {

    public static final String DATABASE_TYPE = "mysql";
    public static final String NAME = "MySQL";
    public static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
    public static final String URL_PREFIX = "jdbc:mysql://";
    public static final int DEFAULT_PORT = 3306;

    public static final String HEALTH_CHECK_SQL = "SELECT 1";
    public static final String SHOW_MASTER_STATUS_SQL = "SHOW MASTER STATUS";
    public static final String SHOW_SLAVE_STATUS_SQL = "SHOW SLAVE STATUS";
    public static final String START_SLAVE_SQL = "START SLAVE";
    public static final String STOP_SLAVE_SQL = "STOP SLAVE";
    public static final String RESET_SLAVE_ALL_SQL = "RESET SLAVE ALL";
    public static final String ENABLE_READ_ONLY_SQL = "SET GLOBAL READ_ONLY=ON";
    public static final String ENABLE_SUPER_READ_ONLY_SQL = "SET GLOBAL SUPER_READ_ONLY=ON";
    public static final String DISABLE_READ_ONLY_SQL = "SET GLOBAL READ_ONLY=OFF";
    public static final String DISABLE_SUPER_READ_ONLY_SQL = "SET GLOBAL SUPER_READ_ONLY=OFF";

    public static final String COLUMN_EXECUTED_GTID_SET = "Executed_Gtid_Set";
    public static final String COLUMN_SLAVE_IO_RUNNING = "Slave_IO_Running";
    public static final String COLUMN_SLAVE_SQL_RUNNING = "Slave_SQL_Running";
    public static final String COLUMN_SECONDS_BEHIND_MASTER = "Seconds_Behind_Master";
    public static final String REPLICATION_RUNNING_VALUE = "Yes";

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
    public String buildChangeReplicationSourceSql(String sourceHost,
                                                  int sourcePort,
                                                  String replicationUser,
                                                  String replicationPassword) {
        return buildChangeMasterSql(sourceHost, sourcePort, replicationUser, replicationPassword);
    }

    @Override
    public String getStartReplicationSql() {
        return START_SLAVE_SQL;
    }

    @Override
    public String getStopReplicationSql() {
        return STOP_SLAVE_SQL;
    }

    @Override
    public String getResetReplicationSql() {
        return RESET_SLAVE_ALL_SQL;
    }

    @Override
    public String getShowReplicaStatusSql() {
        return SHOW_SLAVE_STATUS_SQL;
    }

    @Override
    public String getShowPrimaryStatusSql() {
        return SHOW_MASTER_STATUS_SQL;
    }

    @Override
    public String getEnableReadOnlySql() {
        return ENABLE_READ_ONLY_SQL;
    }

    @Override
    public String getEnableStrictReadOnlySql() {
        return ENABLE_SUPER_READ_ONLY_SQL;
    }

    @Override
    public String getDisableReadOnlySql() {
        return DISABLE_READ_ONLY_SQL;
    }

    @Override
    public String getDisableStrictReadOnlySql() {
        return DISABLE_SUPER_READ_ONLY_SQL;
    }

    @Override
    public String getReplicaIoRunningColumn() {
        return COLUMN_SLAVE_IO_RUNNING;
    }

    @Override
    public String getReplicaSqlRunningColumn() {
        return COLUMN_SLAVE_SQL_RUNNING;
    }

    @Override
    public String getReplicaLagSecondsColumn() {
        return COLUMN_SECONDS_BEHIND_MASTER;
    }

    @Override
    public String getExecutedPositionColumn() {
        return COLUMN_EXECUTED_GTID_SET;
    }

    @Override
    public boolean isReplicaRunning(String ioRunning, String sqlRunning) {
        return REPLICATION_RUNNING_VALUE.equalsIgnoreCase(ioRunning)
                && REPLICATION_RUNNING_VALUE.equalsIgnoreCase(sqlRunning);
    }

    public static String buildChangeMasterSql(String masterHost,
                                             int masterPort,
                                             String replicationUser,
                                             String replicationPassword) {
        return String.format(
                "CHANGE MASTER TO MASTER_HOST='%s', MASTER_PORT=%d, MASTER_USER='%s', MASTER_PASSWORD='%s', MASTER_AUTO_POSITION=1",
                masterHost,
                Integer.valueOf(masterPort),
                replicationUser,
                replicationPassword);
    }

    public static boolean isReplicationRunning(String ioRunning, String sqlRunning) {
        return REPLICATION_RUNNING_VALUE.equalsIgnoreCase(ioRunning)
                && REPLICATION_RUNNING_VALUE.equalsIgnoreCase(sqlRunning);
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