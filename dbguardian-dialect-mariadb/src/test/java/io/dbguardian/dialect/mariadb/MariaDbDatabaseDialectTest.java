package io.dbguardian.dialect.mariadb;

import io.dbguardian.spi.DatabaseDialect;
import io.dbguardian.spi.GtidDatabaseDialect.PositionOrder;
import org.junit.Test;

import java.util.ServiceLoader;

import static org.junit.Assert.*;

public class MariaDbDatabaseDialectTest {

    private final MariaDbDatabaseDialect dialect = new MariaDbDatabaseDialect();

    @Test
    public void discoversDialectThroughServiceLoader() {
        for (DatabaseDialect provider : ServiceLoader.load(DatabaseDialect.class)) {
            if (provider instanceof MariaDbDatabaseDialect) {
                assertEquals("mariadb", provider.getDatabaseType());
                assertEquals("org.mariadb.jdbc.Driver", provider.getDriverClassName());
                return;
            }
        }
        fail("MariaDB dialect not registered");
    }

    @Test
    public void ordersGtidPositionsByDomainAndUnsignedSequence() {
        assertEquals(PositionOrder.BEHIND, dialect.comparePositions("0-1-9", "0-1-10"));
        assertEquals(PositionOrder.AHEAD, dialect.comparePositions("0-2-11", "0-1-10"));
        assertEquals(PositionOrder.EQUAL, dialect.comparePositions("1-5-2, 0-3-4", "0-3-4,1-5-2"));
        assertEquals(PositionOrder.AHEAD, dialect.comparePositions("0-1-18446744073709551615", "0-1-9223372036854775807"));
        assertEquals(PositionOrder.BEHIND, dialect.comparePositions("", "0-1-1"));
        assertEquals(PositionOrder.EQUAL, dialect.comparePositions("", ""));
    }

    @Test
    public void rejectsUnknownAndDivergentPositions() {
        assertEquals(PositionOrder.DIVERGED, dialect.comparePositions("0-1-7", "0-2-7"));
        assertEquals(PositionOrder.DIVERGED, dialect.comparePositions("0-1-7,1-1-3", "0-1-8,1-1-2"));
        assertEquals(PositionOrder.DIVERGED, dialect.comparePositions("0-1-7", "1-1-7"));
        for (String invalid : new String[]{null, "0-1-4,", "0-1-1,0-1-2", "nonsense", "4294967296-1-1", "0-1-18446744073709551616"}) {
            assertEquals(PositionOrder.UNKNOWN, dialect.comparePositions(invalid, "0-1-1"));
        }
    }

    @Test
    public void usesMariaDbReplicationSqlAndSingleNodeUrls() {
        assertEquals("db.local", dialect.getHostFromJdbcUrl("jdbc:mariadb://db.local/schema?connectTimeout=200"));
        assertEquals(3306, dialect.getPortFromJdbcUrl("jdbc:mariadb://db.local/schema"));
        assertEquals("::1", dialect.getHostFromJdbcUrl("jdbc:mariadb://[::1]:3307/schema"));
        assertEquals(3307, dialect.getPortFromJdbcUrl("jdbc:mariadb://[::1]:3307/schema"));
        assertTrue(dialect.isReplicaRunning("Yes", "yes"));
        assertFalse(dialect.isReplicaRunning("Connecting", "Yes"));
        assertNull(dialect.getEnableStrictReadOnlySql());
        assertNull(dialect.getDisableStrictReadOnlySql());
        String sql = dialect.buildChangeReplicationSourceSql("db.local", 3306, "repl", "a\\b'c");
        assertTrue(sql.contains("MASTER_PASSWORD='a\\\\b''c'"));
        assertTrue(sql.endsWith("MASTER_USE_GTID=slave_pos, MASTER_DEMOTE_TO_SLAVE=1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMultiHostUrlForReplication() {
        dialect.getHostFromJdbcUrl("jdbc:mariadb://node1,node2/db");
    }
}
