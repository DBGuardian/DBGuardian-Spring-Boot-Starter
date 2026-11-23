package io.dbguardian.spring.failover;

import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.core.GtidConsistencyInspector;
import io.dbguardian.core.registry.DataSourceRegistry;
import io.dbguardian.dialect.mariadb.MariaDbDatabaseDialect;
import io.dbguardian.runtime.ClusterStatus;
import io.dbguardian.spring.runtime.ClusterRuntimeStateManager;
import org.junit.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class MariaDbFailoverTest {

    /** Requires two disposable MariaDB nodes initialized as described in the MariaDB guide. */
    @Test
    public void reattachesReplicaPromotesItAndSafelyFailsBack() throws Exception {
        String masterUrl = System.getProperty("mariadb.test.master-url");
        String slaveUrl = System.getProperty("mariadb.test.slave-url");
        assumeTrue("Requires a disposable MariaDB pair", masterUrl != null && slaveUrl != null);
        String user = System.getProperty("mariadb.test.user", "root");
        String password = System.getProperty("mariadb.test.password", "");
        DataSourceWrapper master = node("master", masterUrl, user, password, true);
        DataSourceWrapper slave = node("slave", slaveUrl, user, password, false);
        MariaDbDatabaseDialect dialect = new MariaDbDatabaseDialect();
        DataSourceRegistry registry = new DataSourceRegistry();
        registry.registerMaster(master.getId(), master);
        registry.registerSlave(slave.getId(), slave);
        ClusterRuntimeStateManager state = new ClusterRuntimeStateManager();
        state.initialize("mariadb-test", "master", "master");
        FailoverController controller = new FailoverController(registry, null);
        controller.setRuntimeStateManager(state);
        controller.setReplicationUser("dbguardian_repl");
        controller.setReplicationPassword("repl-test-password");
        controller.setCurrentMasterId("master");
        controller.setDemoteOnFailover(false);
        controller.setCatchupTimeoutSeconds(10);
        controller.setCatchupCheckIntervalSeconds(1);

        for (DataSourceWrapper node : new DataSourceWrapper[]{master, slave}) {
            execute(node, "SET SESSION sql_log_bin=OFF",
                    "CREATE USER IF NOT EXISTS 'dbguardian_repl'@'127.0.0.1' IDENTIFIED BY 'repl-test-password'",
                    "GRANT REPLICATION SLAVE ON *.* TO 'dbguardian_repl'@'127.0.0.1'");
        }
        execute(master, "CREATE DATABASE IF NOT EXISTS dbguardian_test",
                "CREATE TABLE dbguardian_test.events (id INT PRIMARY KEY)",
                "CREATE USER IF NOT EXISTS 'dbguardian_writer'@'127.0.0.1' IDENTIFIED BY 'writer-test-password'",
                "GRANT SELECT, INSERT ON dbguardian_test.events TO 'dbguardian_writer'@'127.0.0.1'",
                "INSERT INTO dbguardian_test.events VALUES (1)");
        controller.reconcileGtidTopology(master, slave, dialect, true);
        awaitCount(slave, 1);
        controller.reconcileGtidTopology(master, slave, dialect, true);
        assertEquals("slave", state.resolveLookupKey(false, true));
        assertEquals("master", state.resolveLookupKey(false, false));

        execute(slave, "STOP SLAVE", "RESET SLAVE ALL");
        DataSourceHealthChecker checker = new DataSourceHealthChecker(registry, controller);
        checker.setRuntimeStateManager(state);
        checker.setGtidProtectionEnabled(true);
        GtidConsistencyInspector inspector = new GtidConsistencyInspector();
        inspector.setMaxAllowedLagSeconds(10);
        checker.setGtidConsistencyInspector(inspector);
        checker.setMasterCheckIntervalSeconds(3600);
        checker.setSlaveCheckIntervalSeconds(3600);
        try {
            checker.startHealthChecksAfterInitialization();
            checker.triggerHealthCheck();
            assertEquals("master", state.resolveLookupKey(false, true));
            execute(master, "INSERT INTO dbguardian_test.events VALUES (2)");
            awaitCount(slave, 2);
            checker.triggerHealthCheck();
            assertEquals("slave", state.resolveLookupKey(false, true));

            // Replication is still running when an administrator clears the global write fence.
            execute(slave, "SET GLOBAL read_only=OFF");
            assertEquals(0, queryInt(slave, "SELECT @@GLOBAL.read_only"));
            checker.triggerHealthCheck();
            assertEquals(1, queryInt(slave, "SELECT @@GLOBAL.read_only"));
            assertEquals(0, queryInt(master, "SELECT @@GLOBAL.read_only"));
            assertEquals("slave", state.resolveLookupKey(false, true));
            DataSourceWrapper writer = node("writer", slaveUrl, "dbguardian_writer", "writer-test-password", false);
            assertEquals(2, queryInt(writer, "SELECT COUNT(*) FROM dbguardian_test.events"));
            try {
                execute(writer, "INSERT INTO dbguardian_test.events VALUES (99)");
                fail("Expected a regular writer to be blocked by replica read_only");
            } catch (java.sql.SQLException ex) {
                assertEquals(1290, ex.getErrorCode());
            }
        } finally {
            checker.stopHealthChecks();
        }

        execute(master, "SET GLOBAL read_only=ON");
        registry.updateMasterAvailability("master", false);
        controller.handleMasterFailure("master");
        assertEquals(ClusterStatus.SLAVE_PROMOTED, state.current().getStatus());
        assertEquals("slave", state.resolveLookupKey(true, false));
        assertEquals(0, queryInt(slave, "SELECT @@GLOBAL.read_only"));
        execute(slave, "INSERT INTO dbguardian_test.events VALUES (3)");

        registry.updateMasterAvailability("master", true);
        controller.handleOriginalMasterRecovery("master");
        assertEquals("slave", state.resolveLookupKey(true, false));

        // A failed catch-up must keep the promoted node writable and the original fenced.
        controller.setReplicationPassword("incorrect-password");
        controller.setCatchupTimeoutSeconds(0);
        assertFalse(controller.recoverOriginalMasterAndRestoreReplication("mariadb_timeout_test"));
        assertEquals(ClusterStatus.CATCHING_UP_ORIGINAL_MASTER, state.current().getStatus());
        assertEquals("slave", state.resolveLookupKey(true, false));
        assertEquals(0, queryInt(slave, "SELECT @@GLOBAL.read_only"));
        assertEquals(1, queryInt(master, "SELECT @@GLOBAL.read_only"));
        controller.setReplicationPassword("repl-test-password");
        controller.setCatchupTimeoutSeconds(10);
        assertTrue(controller.recoverOriginalMasterAndRestoreReplication("mariadb_rejoin_only", false));
        assertEquals(ClusterStatus.MASTER_ACTIVE, state.current().getStatus());
        assertEquals("slave", state.resolveLookupKey(true, false));
        assertEquals("master", state.resolveLookupKey(false, true));
        assertEquals(1, queryInt(master, "SELECT @@GLOBAL.read_only"));
        assertEquals(0, queryInt(slave, "SELECT @@GLOBAL.read_only"));
        assertEquals(3, queryInt(master, "SELECT COUNT(*) FROM dbguardian_test.events"));

        // A new process sees equal positions but must preserve the established writer if auto-failback is off.
        state.initialize("mariadb-test", "master", "master");
        controller.reconcileGtidTopology(master, slave, dialect, true, false);
        assertEquals("slave", state.resolveLookupKey(true, false));
        assertEquals("master", state.resolveLookupKey(false, true));

        // Reconciliation with auto-failback enabled can restore the configured topology safely.
        state.initialize("mariadb-test", "master", "master");
        controller.reconcileGtidTopology(master, slave, dialect, true, true);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        boolean recovered;
        do {
            recovered = controller.recoverOriginalMasterAndRestoreReplication("mariadb_test");
            if (!recovered) {
                Thread.sleep(200);
            }
        } while (!recovered && System.nanoTime() < deadline);
        assertTrue("Expected original primary to be restored", recovered);
        assertEquals(ClusterStatus.MASTER_ACTIVE, state.current().getStatus());
        assertEquals("master", state.resolveLookupKey(true, false));
        assertEquals("slave", state.resolveLookupKey(false, true));
        assertEquals(0, queryInt(master, "SELECT @@GLOBAL.read_only"));
        assertEquals(1, queryInt(slave, "SELECT @@GLOBAL.read_only"));
        assertEquals(3, queryInt(master, "SELECT COUNT(*) FROM dbguardian_test.events"));
        execute(master, "INSERT INTO dbguardian_test.events VALUES (4)");
        awaitCount(slave, 4);

        // After failback, health checks must still monitor the restored replica and update read routing.
        DataSourceHealthChecker postFailbackChecker = new DataSourceHealthChecker(registry, controller);
        postFailbackChecker.setRuntimeStateManager(state);
        postFailbackChecker.setMasterCheckIntervalSeconds(3600);
        postFailbackChecker.setSlaveCheckIntervalSeconds(3600);
        try {
            postFailbackChecker.startHealthChecksAfterInitialization();
            execute(slave, "STOP SLAVE");
            postFailbackChecker.triggerHealthCheck();
            assertFalse(state.current().getNodeStates().get("slave").isReplicationHealthy());
            assertEquals("master", state.resolveLookupKey(false, true));
            execute(master, "INSERT INTO dbguardian_test.events VALUES (7)");
            awaitCount(slave, 5);
            postFailbackChecker.triggerHealthCheck();
            assertTrue(state.current().getNodeStates().get("slave").isReplicationHealthy());
            assertEquals("slave", state.resolveLookupKey(false, true));
        } finally {
            postFailbackChecker.stopHealthChecks();
        }

        // Independent writes in different domains must never trigger an automatic role switch.
        execute(slave, "STOP SLAVE", "SET SESSION gtid_domain_id=42", "INSERT INTO dbguardian_test.events VALUES (5)");
        execute(master, "INSERT INTO dbguardian_test.events VALUES (6)");
        controller.reconcileGtidTopology(master, slave, dialect, true);
        assertEquals(ClusterStatus.DEGRADED, state.current().getStatus());
        assertEquals("startup_gtid_DIVERGED", state.current().getLastReason());
        assertEquals(0, queryInt(master, "SELECT @@GLOBAL.read_only"));
        assertEquals(1, queryInt(slave, "SELECT @@GLOBAL.read_only"));
    }

    @Test
    public void replaysPhysicalRoutingKeysAcrossRecovery() {
        ClusterRuntimeStateManager state = new ClusterRuntimeStateManager();
        state.initialize("mariadb-test", "master", "slave");
        state.replayStatusMessage("FAILOVER:master:slave");
        state.replayStatusMessage("RECOVERY_STAGE:CATCHING_UP_ORIGINAL_MASTER:master:slave:test");
        assertEquals("slave", state.resolveLookupKey(true, false));
        state.replayStatusMessage("RECOVERY_STAGE:RESTORING_REPLICATION:master:slave:test");
        assertEquals("slave", state.resolveLookupKey(true, false));
        state.replayStatusMessage("FAILBACK:master:slave");
        assertEquals("master", state.resolveLookupKey(true, false));
        assertEquals("slave", state.resolveLookupKey(false, true));

        state.replayStatusMessage("FAILBACK:slave:master");
        assertEquals("slave", state.resolveLookupKey(true, false));
        assertEquals("master", state.resolveLookupKey(false, true));
        state.replayStatusMessage("FAILOVER:slave:master");
        assertEquals("master", state.resolveLookupKey(true, false));
        assertEquals("master", state.resolveLookupKey(false, true));
    }

    private static DataSourceWrapper node(String id, String url, String user, String password, boolean master) {
        return new DataSourceWrapper(id, url, user, password, MariaDbDatabaseDialect.DRIVER_CLASS_NAME, 100, 100,
                master, new DriverManagerDataSource(url, user, password));
    }

    private static void execute(DataSourceWrapper node, String... commands) throws Exception {
        try (Connection connection = node.getDataSource().getConnection(); Statement statement = connection.createStatement()) {
            for (String command : commands) {
                statement.execute(command);
            }
        }
    }

    private static int queryInt(DataSourceWrapper node, String query) throws Exception {
        try (Connection connection = node.getDataSource().getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(query)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static void awaitCount(DataSourceWrapper node, int expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        do {
            try {
                if (queryInt(node, "SELECT COUNT(*) FROM dbguardian_test.events") == expected) {
                    return;
                }
            } catch (java.sql.SQLException ex) {
                if (!"42S02".equals(ex.getSQLState()) && ex.getErrorCode() != 1049) {
                    throw ex;
                }
            }
            Thread.sleep(200);
        } while (System.nanoTime() < deadline);
        assertEquals(expected, queryInt(node, "SELECT COUNT(*) FROM dbguardian_test.events"));
    }
}
