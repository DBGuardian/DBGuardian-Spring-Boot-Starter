package io.dbguardian.spring.failover;

import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.spi.GtidDatabaseDialect;
import io.dbguardian.spi.GtidDatabaseDialect.PositionOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class GtidReplication {

    private static final Logger log = LoggerFactory.getLogger(GtidReplication.class);
    static final String POSITION_RISK = "gtid_position_risk";

    private GtidReplication() {
    }

    static boolean hasSource(DataSourceWrapper replica, DataSourceWrapper source, GtidDatabaseDialect dialect)
            throws SQLException {
        try (Connection connection = replica.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet status = statement.executeQuery(dialect.getShowReplicaStatusSql())) {
            return status.next() && matchesSource(status, dialect, source);
        }
    }

    private static boolean matchesSource(ResultSet status, GtidDatabaseDialect dialect, DataSourceWrapper source)
            throws SQLException {
        return dialect.followsSource(status, dialect.getHostFromJdbcUrl(source.getUrl()),
                dialect.getPortFromJdbcUrl(source.getUrl()));
    }

    private static int errorCode(ResultSet status, String column) {
        try {
            return status.getInt(column);
        } catch (SQLException ex) {
            return 0;
        }
    }

    static ChannelStatus readChannelStatus(Connection connection, GtidDatabaseDialect dialect, DataSourceWrapper source)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet status = statement.executeQuery(dialect.getShowReplicaStatusSql())) {
            if (!status.next()) {
                return new ChannelStatus(false, false, false, null, null, 0, 0, Long.MAX_VALUE);
            }
            boolean expectedSource = matchesSource(status, dialect, source);
            String io = status.getString(dialect.getReplicaIoRunningColumn());
            String sql = status.getString(dialect.getReplicaSqlRunningColumn());
            boolean running = expectedSource && dialect.isReplicaRunning(io, sql);
            long lag = running ? status.getLong(dialect.getReplicaLagSecondsColumn()) : Long.MAX_VALUE;
            if (running && (status.wasNull() || lag < 0)) {
                lag = Long.MAX_VALUE;
            }
            return new ChannelStatus(true, expectedSource, running, io, sql,
                    errorCode(status, "Last_IO_Errno"), errorCode(status, "Last_SQL_Errno"), lag);
        }
    }

    static final class ChannelStatus {
        private final boolean configured;
        private final boolean expectedSource;
        final boolean running;
        private final String io;
        private final String sql;
        private final int ioError;
        private final int sqlError;
        final long lagSeconds;

        private ChannelStatus(boolean configured, boolean expectedSource, boolean running, String io, String sql,
                              int ioError, int sqlError, long lagSeconds) {
            this.configured = configured;
            this.expectedSource = expectedSource;
            this.running = running;
            this.io = io;
            this.sql = sql;
            this.ioError = ioError;
            this.sqlError = sqlError;
            this.lagSeconds = lagSeconds;
        }

        @Override
        public String toString() {
            if (!configured) {
                return "no replication channel";
            }
            return "IO=" + io + ", SQL=" + sql + ", sourceMatches=" + expectedSource
                    + ", lagSeconds=" + (lagSeconds == Long.MAX_VALUE ? "unknown" : Long.toString(lagSeconds))
                    + ", ioError=" + ioError + ", sqlError=" + sqlError;
        }
    }

    static boolean isGlobalReadOnly(Connection connection, GtidDatabaseDialect dialect) throws SQLException {
        String sql = dialect.getGlobalReadOnlyStatusSql();
        if (sql == null) {
            return true; // This dialect does not support observing the global setting.
        }
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            if (!result.next()) {
                throw new SQLException("Missing global read-only status");
            }
            boolean readOnly = result.getBoolean(1);
            if (result.wasNull()) {
                throw new SQLException("Unknown global read-only status");
            }
            return readOnly;
        }
    }

    static String readPosition(Statement statement, GtidDatabaseDialect dialect) throws SQLException {
        try (ResultSet result = statement.executeQuery(dialect.getShowPrimaryStatusSql())) {
            return result.next() ? result.getString(dialect.getExecutedPositionColumn()) : null;
        }
    }

    static String readPosition(DataSourceWrapper node, GtidDatabaseDialect dialect) throws SQLException {
        try (Connection connection = node.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            return readPosition(statement, dialect);
        }
    }

    static void detach(Statement statement, GtidDatabaseDialect dialect) throws SQLException {
        boolean configured;
        try (ResultSet status = statement.executeQuery(dialect.getShowReplicaStatusSql())) {
            configured = status.next();
        }
        if (configured) {
            // A failed STOP/RESET must prevent promotion. RESET does not clear gtid_slave_pos.
            statement.execute(dialect.getStopReplicationSql());
            statement.execute(dialect.getResetReplicationSql());
        }
    }

    static boolean rejoin(DataSourceWrapper replica, DataSourceWrapper source, GtidDatabaseDialect dialect,
                          String replicationUser, String replicationPassword) throws SQLException {
        try (Connection connection = replica.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(dialect.getEnableReadOnlySql());
            if (!isGlobalReadOnly(connection, dialect)) {
                throw new SQLException("Replica global read-only setting was not enabled");
            }
            String targetPosition = readPosition(statement, dialect);
            PositionOrder order = dialect.comparePositions(targetPosition, readPosition(source, dialect));
            if (order != PositionOrder.BEHIND && order != PositionOrder.EQUAL) {
                log.warn("GTID 重挂拒绝: replica={}, source={}, positionOrder={}", replica.getId(), source.getId(), order);
                return false;
            }
            boolean configured;
            boolean canResume = false;
            try (ResultSet status = statement.executeQuery(dialect.getShowReplicaStatusSql())) {
                configured = status.next();
                if (configured && matchesSource(status, dialect, source)) {
                    String io = status.getString(dialect.getReplicaIoRunningColumn());
                    String sql = status.getString(dialect.getReplicaSqlRunningColumn());
                    int ioError = errorCode(status, "Last_IO_Errno");
                    int sqlError = errorCode(status, "Last_SQL_Errno");
                    if (dialect.isReplicaRunning(io, sql)
                            || ("Connecting".equalsIgnoreCase(io) && "Yes".equalsIgnoreCase(sql) && ioError == 0)) {
                        return true;
                    }
                    // Retry a stopped or transiently disconnected channel without discarding its state.
                    // Authentication errors can be repaired by applying the configured credentials again.
                    if (sqlError != 0 || ioError == 1236) {
                        log.warn("GTID 复制需要人工检查: replica={}, source={}, ioError={}, sqlError={}",
                                replica.getId(), source.getId(), ioError, sqlError);
                        return false;
                    }
                    canResume = ioError != 1045;
                }
            }
            if (canResume) {
                statement.execute(dialect.getStartReplicationSql());
                return true;
            }
            if (replicationUser == null || replicationUser.trim().isEmpty() || replicationPassword == null) {
                log.warn("GTID 重挂缺少复制凭据: replica={}, source={}; 请检查 spring.datasource.replication.master-user/master-password",
                        replica.getId(), source.getId());
                return false;
            }
            // Keep connection options (e.g. replication TLS) when changing the source.
            if (configured) {
                statement.execute(dialect.getStopReplicationSql());
            }
            dialect.changeReplicationSource(connection, dialect.getHostFromJdbcUrl(source.getUrl()),
                    dialect.getPortFromJdbcUrl(source.getUrl()), replicationUser, replicationPassword);
            statement.execute(dialect.getStartReplicationSql());
            return true;
        } catch (IllegalArgumentException ex) {
            throw new SQLException(ex.getMessage(), ex);
        }
    }
}
