package io.dbguardian.spi;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Optional SPI for dialects with ordered GTID positions and JDBC-based reparenting. */
public interface GtidDatabaseDialect extends DatabaseDialect {

    enum PositionOrder {
        EQUAL, AHEAD, BEHIND, DIVERGED, UNKNOWN
    }

    /** Compare the left position to the right one. Unknown is distinct from a known empty position. */
    PositionOrder comparePositions(String left, String right);

    /** Optional query returning the server's global write fence as a boolean in the first column. */
    default String getGlobalReadOnlyStatusSql() {
        return null;
    }

    /** Verify that the status row belongs to the expected source and uses GTID replication. */
    boolean followsSource(ResultSet status, String sourceHost, int sourcePort) throws SQLException;

    /** Change the source without discarding the local executed GTID position. */
    void changeReplicationSource(Connection connection, String sourceHost, int sourcePort,
                                 String replicationUser, String replicationPassword) throws SQLException;
}
