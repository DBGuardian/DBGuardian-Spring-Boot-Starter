package io.dbguardian.dialect.mariadb;

import io.dbguardian.spi.GtidDatabaseDialect;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MariaDbDatabaseDialect implements GtidDatabaseDialect {

    public static final String DATABASE_TYPE = "mariadb";
    public static final String DRIVER_CLASS_NAME = "org.mariadb.jdbc.Driver";
    public static final int DEFAULT_PORT = 3306;
    private static final Pattern NODE_URL = Pattern.compile(
            "jdbc:mariadb://(?:\\[([0-9a-f:.]+)\\]|([a-z0-9_.-]+))(?::([0-9]{1,5}))?(?:/[^?]*)?(?:\\?.*)?",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String getName() {
        return "MariaDB";
    }

    @Override
    public String getDatabaseType() {
        return DATABASE_TYPE;
    }

    @Override
    public String getDriverClassName() {
        return DRIVER_CLASS_NAME;
    }

    @Override
    public String getHealthCheckSql() {
        return "SELECT 1";
    }

    @Override
    public String getShowReplicaStatusSql() {
        return "SHOW SLAVE STATUS";
    }

    @Override
    public String getShowPrimaryStatusSql() {
        // Gtid_IO_Pos in SHOW SLAVE STATUS includes events that have not been applied yet.
        return "SELECT @@GLOBAL.gtid_current_pos AS executed_position";
    }

    @Override
    public String getExecutedPositionColumn() {
        return "executed_position";
    }

    @Override
    public String getReplicaIoRunningColumn() {
        return "Slave_IO_Running";
    }

    @Override
    public String getReplicaSqlRunningColumn() {
        return "Slave_SQL_Running";
    }

    @Override
    public String getReplicaLagSecondsColumn() {
        return "Seconds_Behind_Master";
    }

    @Override
    public boolean isReplicaRunning(String ioRunning, String sqlRunning) {
        return "Yes".equalsIgnoreCase(ioRunning) && "Yes".equalsIgnoreCase(sqlRunning);
    }

    @Override
    public String getStartReplicationSql() {
        return "START SLAVE";
    }

    @Override
    public String getStopReplicationSql() {
        return "STOP SLAVE";
    }

    @Override
    public String getResetReplicationSql() {
        return "RESET SLAVE ALL";
    }

    @Override
    public String getGlobalReadOnlyStatusSql() {
        return "SELECT @@GLOBAL.read_only";
    }

    @Override
    public String getEnableReadOnlySql() {
        return "SET GLOBAL read_only = ON";
    }

    @Override
    public String getDisableReadOnlySql() {
        return "SET GLOBAL read_only = OFF";
    }

    @Override
    public String buildChangeReplicationSourceSql(String host, int port, String user, String password) {
        return changeSourceSql(host, port, user, password, false);
    }

    @Override
    public void changeReplicationSource(Connection connection, String host, int port, String user, String password)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            boolean noBackslashEscapes;
            try (ResultSet modes = statement.executeQuery(
                    "SELECT FIND_IN_SET('NO_BACKSLASH_ESCAPES', @@SESSION.sql_mode)")) {
                if (!modes.next()) {
                    throw new SQLException("Unable to determine MariaDB session SQL mode");
                }
                noBackslashEscapes = modes.getInt(1) > 0;
            }
            try {
                statement.execute(changeSourceSql(host, port, user, password, noBackslashEscapes));
            } catch (SQLException ex) {
                // Connector exceptions can contain the statement and the replication password.
                throw new SQLException("MariaDB CHANGE MASTER TO failed (error " + ex.getErrorCode() + ")",
                        ex.getSQLState(), ex.getErrorCode());
            }
        }
    }

    private String changeSourceSql(String host, int port, String user, String password, boolean noBackslashEscapes) {
        return "CHANGE MASTER TO MASTER_HOST=" + quote(host, noBackslashEscapes)
                + ", MASTER_PORT=" + port + ", MASTER_USER=" + quote(user, noBackslashEscapes)
                + ", MASTER_PASSWORD=" + quote(password, noBackslashEscapes)
                + ", MASTER_USE_GTID=slave_pos, MASTER_DEMOTE_TO_SLAVE=1";
    }

    private String quote(String value, boolean noBackslashEscapes) {
        String escaped = noBackslashEscapes ? value : value.replace("\\", "\\\\");
        return "'" + escaped.replace("'", "''") + "'";
    }

    @Override
    public PositionOrder comparePositions(String left, String right) {
        return MariaDbGtidPosition.compare(left, right);
    }

    @Override
    public boolean followsSource(ResultSet status, String sourceHost, int sourcePort) throws SQLException {
        String gtidMode = status.getString("Using_Gtid");
        return sourceHost.equalsIgnoreCase(status.getString("Master_Host"))
                && sourcePort == status.getInt("Master_Port")
                && ("Slave_Pos".equalsIgnoreCase(gtidMode) || "Current_Pos".equalsIgnoreCase(gtidMode));
    }

    @Override
    public String getHostFromJdbcUrl(String jdbcUrl) {
        Matcher node = matchNodeUrl(jdbcUrl);
        return node.group(1) != null ? node.group(1) : node.group(2);
    }

    @Override
    public int getPortFromJdbcUrl(String jdbcUrl) {
        Matcher node = matchNodeUrl(jdbcUrl);
        int port = node.group(3) == null ? DEFAULT_PORT : Integer.parseInt(node.group(3));
        if (port == 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid MariaDB node port");
        }
        return port;
    }

    private Matcher matchNodeUrl(String jdbcUrl) {
        Matcher node = NODE_URL.matcher(jdbcUrl == null ? "" : jdbcUrl);
        if (!node.matches()) {
            throw new IllegalArgumentException("Expected a single-node jdbc:mariadb://host[:port]/database URL");
        }
        return node;
    }
}
