package com.dbguardian.test;

import io.dbguardian.boot2.config.DbGuardianBoot2AutoConfiguration;
import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.core.registry.DataSourceRegistry;
import io.dbguardian.runtime.ClusterStatus;
import io.dbguardian.runtime.NodeRuntimeState;
import io.dbguardian.spring.coordination.DatasourceCoordinationService;
import io.dbguardian.spring.failover.DataSourceHealthChecker;
import io.dbguardian.spring.failover.FailoverController;
import io.dbguardian.spring.runtime.ClusterRuntimeStateManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 故障转移测试
 * 技术栈: [JAVA8] [SPRING_BOOT_27] [MYBATIS_PLUS] [MYSQL]
 */
@SpringBootTest(classes = {Application.class, DbGuardianBoot2AutoConfiguration.class})
@ActiveProfiles("test")
public class FailoverTest {

    @Autowired(required = false)
    private DatasourceCoordinationService coordinationService;

    @Test
    public void testCurrentDataSourceStatus() {
        assertNotNull(coordinationService, "协调服务应被 starter 注入");

        DatasourceCoordinationService.CoordinationStatus status = coordinationService.getCoordinationStatus();
        assertNotNull(status, "协调状态不应为空");

        String masterStatus = status.getMasterStatus();
        boolean validStatus = DatasourceCoordinationService.STATUS_NORMAL.equals(masterStatus)
                || DatasourceCoordinationService.STATUS_SLAVE_PROMOTED.equals(masterStatus);
        assertTrue(validStatus, "主库状态应该来自 starter 定义的状态集合");
    }

    @Test
    public void testFailoverConfiguration() {
        assertNotNull(coordinationService, "DBGuardian starter 注入结果不应为空");
        if (coordinationService != null) {
            assertNotNull(coordinationService.getInstanceId(), "协调服务实例ID应存在");
        }
    }

    @Test
    public void testStatusTransitions() {
        assertEquals("NORMAL", DatasourceCoordinationService.STATUS_NORMAL);
        assertEquals("SLAVE_PROMOTED", DatasourceCoordinationService.STATUS_SLAVE_PROMOTED);
    }

    @Test
    public void testSlaveRemainsPromotionCandidateWhenMasterIsDown() throws Exception {
        DataSourceRegistry registry = new DataSourceRegistry();
        ClusterRuntimeStateManager runtimeStateManager = new ClusterRuntimeStateManager();
        runtimeStateManager.initialize("failover-test", "master", "slave");
        FailoverController failoverController = new FailoverController(registry, null);
        failoverController.setRuntimeStateManager(runtimeStateManager);
        failoverController.setReplicationUser("repl");
        failoverController.setReplicationPassword("repl-pass");

        DataSourceHealthChecker checker = new DataSourceHealthChecker(registry, failoverController);
        checker.setRuntimeStateManager(runtimeStateManager);
        checker.setFailoverEnabled(false);

        ScriptedDataSource masterDataSource = ScriptedDataSource.masterDown();
        ScriptedDataSource slaveDataSource = ScriptedDataSource.slaveHealthyButReplicationBroken();
        registry.registerMaster("master", wrapper("master", true, masterDataSource));
        registry.registerSlave("slave", wrapper("slave", false, slaveDataSource));
        registry.updateMasterAvailability("master", false);

        invokePrivate(checker, "checkSlaves");

        assertEquals(1, registry.getAvailableSlaveCount(), "主挂时，复制断开的健康从库仍应留在候选池");
        NodeRuntimeState slaveState = runtimeStateManager.current().getNodeState("slave");
        assertNotNull(slaveState, "从库运行态应存在");
        assertTrue(slaveState.isHealthy(), "从库连接应保持健康");
        assertFalse(slaveState.isReplicationHealthy(), "主挂后复制状态应视为中断");
        assertEquals("slave_promotion_candidate", slaveState.getLastError(), "从库应被标成升主候选");
    }

    @Test
    public void testPromoteSlaveWhenMasterFailsAfterSlaveCheck() throws Exception {
        DataSourceRegistry registry = new DataSourceRegistry();
        ClusterRuntimeStateManager runtimeStateManager = new ClusterRuntimeStateManager();
        runtimeStateManager.initialize("failover-test", "master", "slave");
        FailoverController failoverController = new FailoverController(registry, null);
        failoverController.setRuntimeStateManager(runtimeStateManager);
        failoverController.setReplicationUser("repl");
        failoverController.setReplicationPassword("repl-pass");

        DataSourceHealthChecker checker = new DataSourceHealthChecker(registry, failoverController);
        checker.setRuntimeStateManager(runtimeStateManager);
        checker.setFailoverEnabled(false);

        ScriptedDataSource masterDataSource = ScriptedDataSource.masterDown();
        ScriptedDataSource slaveDataSource = ScriptedDataSource.slaveHealthyButReplicationBroken();
        registry.registerMaster("master", wrapper("master", true, masterDataSource));
        registry.registerSlave("slave", wrapper("slave", false, slaveDataSource));

        invokePrivate(checker, "checkSlaves");
        invokePrivate(checker, "checkMasters");

        assertEquals(ClusterStatus.SLAVE_PROMOTED, runtimeStateManager.current().getStatus(), "主挂后应进入从库升主状态");
        assertEquals("slave", runtimeStateManager.current().getActiveMasterId(), "当前主应切到从库");
        assertEquals("master", runtimeStateManager.current().getPendingOriginalMasterId(), "旧主应被标记为待恢复原主");
        assertTrue(runtimeStateManager.current().isSlaveReadsBlocked(), "故障转移窗口应阻断从读");

        assertPromotedMasterWritable(slaveDataSource);
        assertPromotedMasterDetachedFromReplication(slaveDataSource);
    }

    @Test
    public void testOriginalMasterRecoveryRestoresReplicationLoop() throws Exception {
        DataSourceRegistry registry = new DataSourceRegistry();
        ClusterRuntimeStateManager runtimeStateManager = new ClusterRuntimeStateManager();
        runtimeStateManager.initialize("failover-test", "master", "slave");
        FailoverController failoverController = new FailoverController(registry, null);
        failoverController.setRuntimeStateManager(runtimeStateManager);
        failoverController.setReplicationUser("repl");
        failoverController.setReplicationPassword("repl-pass");
        failoverController.setCatchupTimeoutSeconds(1);
        failoverController.setCatchupCheckIntervalSeconds(1);

        DataSourceHealthChecker checker = new DataSourceHealthChecker(registry, failoverController);
        checker.setRuntimeStateManager(runtimeStateManager);
        checker.setFailoverEnabled(false);

        ScriptedDataSource masterDataSource = ScriptedDataSource.masterDownThenRecovered();
        ScriptedDataSource slaveDataSource = ScriptedDataSource.slaveHealthyThenPromotedThenReplicated();
        registry.registerMaster("master", wrapper("master", true, masterDataSource));
        registry.registerSlave("slave", wrapper("slave", false, slaveDataSource));

        invokePrivate(checker, "checkSlaves");
        invokePrivate(checker, "checkMasters");

        assertEquals(ClusterStatus.SLAVE_PROMOTED, runtimeStateManager.current().getStatus(), "故障后应先完成升主");

        failoverController.handleOriginalMasterRecovery("master");
        assertEquals(ClusterStatus.RECOVERING_ORIGINAL_MASTER, runtimeStateManager.current().getStatus(), "检测到原主恢复后应进入恢复窗口");

        boolean recovered = failoverController.recoverOriginalMasterAndRestoreReplication("manual");
        assertTrue(recovered, "原主恢复闭环应执行成功");
        assertEquals(ClusterStatus.MASTER_ACTIVE, runtimeStateManager.current().getStatus(), "恢复完成后应回到主从稳定态");
        assertEquals("master", runtimeStateManager.current().getActiveMasterId(), "原主应重新成为主库");

        assertRestoredMasterWritable(masterDataSource);
        assertPromotedSlaveReplicatingFromOriginalMaster(slaveDataSource);
    }

    private static DataSourceWrapper wrapper(String id, boolean master, DataSource dataSource) {
        return new DataSourceWrapper(
                id,
                "jdbc:mysql://127.0.0.1:3306/dbguardian_test",
                "root",
                "root",
                "com.mysql.cj.jdbc.Driver",
                100,
                100,
                master,
                dataSource
        );
    }

    private static void invokePrivate(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }

    private static void assertPromotedMasterWritable(ScriptedDataSource dataSource) throws SQLException {
        assertTrue(dataSource.executedSqlContains("SET GLOBAL READ_ONLY=OFF"), "升主时应关闭新主只读");
        assertTrue(dataSource.executedSqlContains("SET GLOBAL SUPER_READ_ONLY=OFF"), "升主时应关闭新主超级只读");
        assertEquals("OFF", queryVariable(dataSource, "read_only"), "升主后 read_only 应为 OFF");
        assertEquals("OFF", queryVariable(dataSource, "super_read_only"), "升主后 super_read_only 应为 OFF");
    }

    private static void assertPromotedMasterDetachedFromReplication(ScriptedDataSource dataSource) throws SQLException {
        assertFalse(hasSlaveStatus(dataSource), "升主后不应继续保留 SHOW SLAVE STATUS 结果");
    }

    private static void assertRestoredMasterWritable(ScriptedDataSource dataSource) throws SQLException {
        assertTrue(dataSource.executedSqlContains("CHANGE MASTER TO MASTER_HOST='127.0.0.1', MASTER_PORT=3306, MASTER_USER='repl', MASTER_PASSWORD='repl-pass', MASTER_AUTO_POSITION=1"), "原主恢复阶段应重新挂到临时主进行追赶");
        assertTrue(dataSource.executedSqlContains("START SLAVE"), "原主恢复阶段应启动复制追赶");
        assertEquals("OFF", queryVariable(dataSource, "read_only"), "原主恢复为主后 read_only 应为 OFF");
        assertEquals("OFF", queryVariable(dataSource, "super_read_only"), "原主恢复为主后 super_read_only 应为 OFF");
        assertFalse(hasSlaveStatus(dataSource), "原主恢复为主后不应继续保留从库复制状态");
    }

    private static void assertPromotedSlaveReplicatingFromOriginalMaster(ScriptedDataSource dataSource) throws SQLException {
        assertTrue(dataSource.executedSqlContains("CHANGE MASTER TO MASTER_HOST='127.0.0.1', MASTER_PORT=3306, MASTER_USER='repl', MASTER_PASSWORD='repl-pass', MASTER_AUTO_POSITION=1"), "切回后原从库应重新指向原主");
        assertEquals("ON", queryVariable(dataSource, "read_only"), "切回后原从库 read_only 应为 ON");
        assertEquals("ON", queryVariable(dataSource, "super_read_only"), "切回后原从库 super_read_only 应为 ON");
        assertTrue(hasSlaveStatus(dataSource), "切回后原从库应重新带有复制状态");
        assertEquals("Yes", querySlaveStatusField(dataSource, "Slave_IO_Running"), "切回后原从库 IO 线程应恢复");
        assertEquals("Yes", querySlaveStatusField(dataSource, "Slave_SQL_Running"), "切回后原从库 SQL 线程应恢复");
        assertEquals("0", querySlaveStatusField(dataSource, "Seconds_Behind_Master"), "切回后原从库应追平原主");
    }

    private static String queryVariable(DataSource dataSource, String variableName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SHOW VARIABLES LIKE '" + variableName + "'")) {
            assertTrue(rs.next(), "应返回变量: " + variableName);
            return rs.getString("Value");
        }
    }

    private static boolean hasSlaveStatus(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SHOW SLAVE STATUS")) {
            return rs.next();
        }
    }

    private static String querySlaveStatusField(DataSource dataSource, String fieldName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SHOW SLAVE STATUS")) {
            assertTrue(rs.next(), "应返回复制状态");
            return rs.getString(fieldName);
        }
    }

    private static final class ScriptedDataSource implements DataSource {

        private final Deque<ConnectionPlan> plans = new ArrayDeque<ConnectionPlan>();
        private final List<String> executedSql = new ArrayList<String>();
        private boolean readOnly = true;
        private boolean superReadOnly = true;
        private boolean slaveStatusAvailable;
        private String slaveIoRunning = "No";
        private String slaveSqlRunning = "No";
        private String slaveLag;

        static ScriptedDataSource masterDown() {
            ScriptedDataSource dataSource = new ScriptedDataSource();
            dataSource.readOnly = false;
            dataSource.superReadOnly = false;
            dataSource.plans.add(ConnectionPlan.invalid());
            dataSource.plans.add(ConnectionPlan.invalid());
            dataSource.plans.add(ConnectionPlan.invalid());
            return dataSource;
        }

        static ScriptedDataSource masterDownThenRecovered() {
            ScriptedDataSource dataSource = new ScriptedDataSource();
            dataSource.readOnly = false;
            dataSource.superReadOnly = false;
            dataSource.plans.add(ConnectionPlan.invalid());
            dataSource.plans.add(ConnectionPlan.validOnly());
            dataSource.plans.add(ConnectionPlan.catchupComplete());
            dataSource.plans.add(ConnectionPlan.validOnly());
            dataSource.plans.add(ConnectionPlan.validOnly());
            dataSource.plans.add(ConnectionPlan.validOnly());
            return dataSource;
        }

        static ScriptedDataSource slaveHealthyButReplicationBroken() {
            ScriptedDataSource dataSource = new ScriptedDataSource();
            dataSource.slaveStatusAvailable = true;
            dataSource.slaveIoRunning = "No";
            dataSource.slaveSqlRunning = "No";
            dataSource.slaveLag = null;
            dataSource.plans.add(ConnectionPlan.replicationBroken());
            dataSource.plans.add(ConnectionPlan.promotionWritable());
            dataSource.plans.add(ConnectionPlan.validOnly());
            dataSource.plans.add(ConnectionPlan.validOnly());
            return dataSource;
        }

        static ScriptedDataSource slaveHealthyThenPromotedThenReplicated() {
            ScriptedDataSource dataSource = new ScriptedDataSource();
            dataSource.slaveStatusAvailable = true;
            dataSource.slaveIoRunning = "No";
            dataSource.slaveSqlRunning = "No";
            dataSource.slaveLag = null;
            dataSource.plans.add(ConnectionPlan.replicationBroken());
            dataSource.plans.add(ConnectionPlan.promotionWritable());
            dataSource.plans.add(ConnectionPlan.validOnly());
            dataSource.plans.add(ConnectionPlan.validOnly());
            dataSource.plans.add(ConnectionPlan.validOnly());
            dataSource.plans.add(ConnectionPlan.validOnly());
            dataSource.plans.add(ConnectionPlan.validOnly());
            return dataSource;
        }

        boolean executedSqlContains(String sql) {
            return executedSql.contains(sql);
        }

        @Override
        public Connection getConnection() {
            ConnectionPlan plan = plans.isEmpty() ? ConnectionPlan.validOnly() : plans.removeFirst();
            return plan.toConnection(this, executedSql);
        }

        @Override
        public Connection getConnection(String username, String password) {
            return getConnection();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("unsupported");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        @Override
        public java.io.PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getGlobal();
        }
    }

    private static final class ConnectionPlan {
        private final boolean valid;
        private final boolean slaveStatusAvailable;
        private final String ioRunning;
        private final String sqlRunning;
        private final String lag;

        private ConnectionPlan(boolean valid, boolean slaveStatusAvailable, String ioRunning, String sqlRunning, String lag) {
            this.valid = valid;
            this.slaveStatusAvailable = slaveStatusAvailable;
            this.ioRunning = ioRunning;
            this.sqlRunning = sqlRunning;
            this.lag = lag;
        }

        static ConnectionPlan invalid() {
            return new ConnectionPlan(false, false, null, null, null);
        }

        static ConnectionPlan validOnly() {
            return new ConnectionPlan(true, false, null, null, null);
        }

        static ConnectionPlan replicationBroken() {
            return new ConnectionPlan(true, true, "No", "No", null);
        }

        static ConnectionPlan catchupComplete() {
            return new ConnectionPlan(true, true, "Yes", "Yes", "0");
        }

        static ConnectionPlan promotionWritable() {
            return new ConnectionPlan(true, false, null, null, null);
        }

        Connection toConnection(ScriptedDataSource dataSource, List<String> executedSql) {
            return new Connection() {
                @Override
                public Statement createStatement() {
                    return new Statement() {
                        @Override
                        public ResultSet executeQuery(String sql) {
                            if ("SHOW SLAVE STATUS".equalsIgnoreCase(sql)) {
                                if (!dataSource.slaveStatusAvailable && !slaveStatusAvailable) {
                                    return new EmptyResultSet();
                                }
                                return new SlaveStatusResultSet(
                                        dataSource.slaveIoRunning == null ? ioRunning : dataSource.slaveIoRunning,
                                        dataSource.slaveSqlRunning == null ? sqlRunning : dataSource.slaveSqlRunning,
                                        dataSource.slaveLag == null ? lag : dataSource.slaveLag
                                );
                            }
                            if (sql.startsWith("SHOW VARIABLES LIKE '")) {
                                String variableName = sql.substring("SHOW VARIABLES LIKE '".length(), sql.length() - 1);
                                String value = "read_only".equalsIgnoreCase(variableName)
                                        ? (dataSource.readOnly ? "ON" : "OFF")
                                        : "super_read_only".equalsIgnoreCase(variableName)
                                        ? (dataSource.superReadOnly ? "ON" : "OFF")
                                        : null;
                                return value == null ? new EmptyResultSet() : new VariableResultSet(variableName, value);
                            }
                            return new EmptyResultSet();
                        }

                        @Override
                        public boolean execute(String sql) {
                            executedSql.add(sql);
                            if ("SET GLOBAL READ_ONLY=OFF".equalsIgnoreCase(sql)) {
                                dataSource.readOnly = false;
                            } else if ("SET GLOBAL READ_ONLY=ON".equalsIgnoreCase(sql)) {
                                dataSource.readOnly = true;
                            } else if ("SET GLOBAL SUPER_READ_ONLY=OFF".equalsIgnoreCase(sql)) {
                                dataSource.superReadOnly = false;
                            } else if ("SET GLOBAL SUPER_READ_ONLY=ON".equalsIgnoreCase(sql)) {
                                dataSource.superReadOnly = true;
                            } else if (sql.startsWith("CHANGE MASTER TO ")) {
                                dataSource.slaveStatusAvailable = true;
                                dataSource.slaveIoRunning = "Connecting";
                                dataSource.slaveSqlRunning = "Yes";
                                dataSource.slaveLag = null;
                            } else if ("START SLAVE".equalsIgnoreCase(sql)) {
                                dataSource.slaveStatusAvailable = true;
                                dataSource.slaveIoRunning = "Yes";
                                dataSource.slaveSqlRunning = "Yes";
                                dataSource.slaveLag = "0";
                            } else if ("STOP SLAVE".equalsIgnoreCase(sql) || "RESET SLAVE ALL".equalsIgnoreCase(sql)) {
                                dataSource.slaveStatusAvailable = false;
                                dataSource.slaveIoRunning = null;
                                dataSource.slaveSqlRunning = null;
                                dataSource.slaveLag = null;
                            }
                            return true;
                        }

                        @Override public void close() {}
                        @Override public int getMaxFieldSize() { return 0; }
                        @Override public void setMaxFieldSize(int max) {}
                        @Override public int getMaxRows() { return 0; }
                        @Override public void setMaxRows(int max) {}
                        @Override public void setEscapeProcessing(boolean enable) {}
                        @Override public int getQueryTimeout() { return 0; }
                        @Override public void setQueryTimeout(int seconds) {}
                        @Override public void cancel() {}
                        @Override public SQLWarning getWarnings() { return null; }
                        @Override public void clearWarnings() {}
                        @Override public void setCursorName(String name) {}
                        @Override public ResultSet getResultSet() { return null; }
                        @Override public int getUpdateCount() { return 0; }
                        @Override public boolean getMoreResults() { return false; }
                        @Override public void setFetchDirection(int direction) {}
                        @Override public int getFetchDirection() { return ResultSet.FETCH_FORWARD; }
                        @Override public void setFetchSize(int rows) {}
                        @Override public int getFetchSize() { return 0; }
                        @Override public int getResultSetConcurrency() { return ResultSet.CONCUR_READ_ONLY; }
                        @Override public int getResultSetType() { return ResultSet.TYPE_FORWARD_ONLY; }
                        @Override public void addBatch(String sql) {}
                        @Override public void clearBatch() {}
                        @Override public int[] executeBatch() { return new int[0]; }
                        @Override public Connection getConnection() { return null; }
                        @Override public boolean getMoreResults(int current) { return false; }
                        @Override public ResultSet getGeneratedKeys() { return null; }
                        @Override public int executeUpdate(String sql) { executedSql.add(sql); return 0; }
                        @Override public int executeUpdate(String sql, int autoGeneratedKeys) { executedSql.add(sql); return 0; }
                        @Override public int executeUpdate(String sql, int[] columnIndexes) { executedSql.add(sql); return 0; }
                        @Override public int executeUpdate(String sql, String[] columnNames) { executedSql.add(sql); return 0; }
                        @Override public boolean execute(String sql, int autoGeneratedKeys) { executedSql.add(sql); return true; }
                        @Override public boolean execute(String sql, int[] columnIndexes) { executedSql.add(sql); return true; }
                        @Override public boolean execute(String sql, String[] columnNames) { executedSql.add(sql); return true; }
                        @Override public int getResultSetHoldability() { return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
                        @Override public boolean isClosed() { return false; }
                        @Override public void setPoolable(boolean poolable) {}
                        @Override public boolean isPoolable() { return false; }
                        @Override public void closeOnCompletion() {}
                        @Override public boolean isCloseOnCompletion() { return false; }
                        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("unsupported"); }
                        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
                    };
                }

                @Override public boolean isValid(int timeout) { return valid; }
                @Override public void close() {}
                @Override public boolean isClosed() { return false; }
                @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("unsupported"); }
                @Override public boolean isWrapperFor(Class<?> iface) { return false; }
                @Override public PreparedStatement prepareStatement(String sql) throws SQLException { throw new SQLException("unsupported"); }
                @Override public CallableStatement prepareCall(String sql) throws SQLException { throw new SQLException("unsupported"); }
                @Override public String nativeSQL(String sql) { return sql; }
                @Override public void setAutoCommit(boolean autoCommit) {}
                @Override public boolean getAutoCommit() { return true; }
                @Override public void commit() {}
                @Override public void rollback() {}
                @Override public DatabaseMetaData getMetaData() { return null; }
                @Override public void setReadOnly(boolean readOnly) {}
                @Override public boolean isReadOnly() { return false; }
                @Override public void setCatalog(String catalog) {}
                @Override public String getCatalog() { return null; }
                @Override public void setTransactionIsolation(int level) {}
                @Override public int getTransactionIsolation() { return Connection.TRANSACTION_NONE; }
                @Override public SQLWarning getWarnings() { return null; }
                @Override public void clearWarnings() {}
                @Override public Statement createStatement(int resultSetType, int resultSetConcurrency) { return createStatement(); }
                @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { throw new SQLException("unsupported"); }
                @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { throw new SQLException("unsupported"); }
                @Override public Map<String, Class<?>> getTypeMap() { return null; }
                @Override public void setTypeMap(Map<String, Class<?>> map) {}
                @Override public void setHoldability(int holdability) {}
                @Override public int getHoldability() { return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
                @Override public Savepoint setSavepoint() { return null; }
                @Override public Savepoint setSavepoint(String name) { return null; }
                @Override public void rollback(Savepoint savepoint) {}
                @Override public void releaseSavepoint(Savepoint savepoint) {}
                @Override public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) { return createStatement(); }
                @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { throw new SQLException("unsupported"); }
                @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { throw new SQLException("unsupported"); }
                @Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { throw new SQLException("unsupported"); }
                @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { throw new SQLException("unsupported"); }
                @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { throw new SQLException("unsupported"); }
                @Override public Clob createClob() { return null; }
                @Override public Blob createBlob() { return null; }
                @Override public NClob createNClob() { return null; }
                @Override public SQLXML createSQLXML() { return null; }
                @Override public void setClientInfo(String name, String value) {}
                @Override public void setClientInfo(java.util.Properties properties) {}
                @Override public String getClientInfo(String name) { return null; }
                @Override public java.util.Properties getClientInfo() { return new java.util.Properties(); }
                @Override public Array createArrayOf(String typeName, Object[] elements) { return null; }
                @Override public Struct createStruct(String typeName, Object[] attributes) { return null; }
                @Override public void setSchema(String schema) {}
                @Override public String getSchema() { return null; }
                @Override public void abort(java.util.concurrent.Executor executor) {}
                @Override public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) {}
                @Override public int getNetworkTimeout() { return 0; }
            };
        }
    }

    private static class EmptyResultSet extends ResultSetAdapter {
        @Override
        public boolean next() {
            return false;
        }
    }

    private static class SlaveStatusResultSet extends ResultSetAdapter {
        private final List<String> columns = Arrays.asList("Slave_IO_Running", "Slave_SQL_Running", "Seconds_Behind_Master");
        private final List<String> values;
        private boolean advanced;

        private SlaveStatusResultSet(String ioRunning, String sqlRunning, String lag) {
            this.values = Arrays.asList(ioRunning, sqlRunning, lag);
        }

        @Override
        public boolean next() {
            if (advanced) {
                return false;
            }
            advanced = true;
            return true;
        }

        @Override
        public String getString(String columnLabel) {
            int index = columns.indexOf(columnLabel);
            return index < 0 ? null : values.get(index);
        }
    }

    private static class VariableResultSet extends ResultSetAdapter {
        private final String variableName;
        private final String variableValue;
        private boolean advanced;

        private VariableResultSet(String variableName, String variableValue) {
            this.variableName = variableName;
            this.variableValue = variableValue;
        }

        @Override
        public boolean next() {
            if (advanced) {
                return false;
            }
            advanced = true;
            return true;
        }

        @Override
        public String getString(String columnLabel) {
            if ("Variable_name".equalsIgnoreCase(columnLabel)) {
                return variableName;
            }
            if ("Value".equalsIgnoreCase(columnLabel)) {
                return variableValue;
            }
            return null;
        }
    }

    private abstract static class ResultSetAdapter implements ResultSet {
        @Override public boolean next() throws SQLException { return false; }
        @Override public void close() {}
        @Override public boolean wasNull() { return false; }
        @Override public String getString(String columnLabel) { return null; }
        @Override public boolean getBoolean(int columnIndex) { return false; }
        @Override public byte getByte(int columnIndex) { return 0; }
        @Override public short getShort(int columnIndex) { return 0; }
        @Override public int getInt(int columnIndex) { return 0; }
        @Override public long getLong(int columnIndex) { return 0; }
        @Override public float getFloat(int columnIndex) { return 0; }
        @Override public double getDouble(int columnIndex) { return 0; }
        @Override public java.math.BigDecimal getBigDecimal(int columnIndex, int scale) { return null; }
        @Override public byte[] getBytes(int columnIndex) { return new byte[0]; }
        @Override public java.sql.Date getDate(int columnIndex) { return null; }
        @Override public java.sql.Time getTime(int columnIndex) { return null; }
        @Override public java.sql.Timestamp getTimestamp(int columnIndex) { return null; }
        @Override public java.io.InputStream getAsciiStream(int columnIndex) { return null; }
        @Override public java.io.InputStream getUnicodeStream(int columnIndex) { return null; }
        @Override public java.io.InputStream getBinaryStream(int columnIndex) { return null; }
        @Override public String getString(int columnIndex) { return null; }
        @Override public boolean getBoolean(String columnLabel) { return false; }
        @Override public byte getByte(String columnLabel) { return 0; }
        @Override public short getShort(String columnLabel) { return 0; }
        @Override public int getInt(String columnLabel) { return 0; }
        @Override public long getLong(String columnLabel) { return 0; }
        @Override public float getFloat(String columnLabel) { return 0; }
        @Override public double getDouble(String columnLabel) { return 0; }
        @Override public java.math.BigDecimal getBigDecimal(String columnLabel, int scale) { return null; }
        @Override public byte[] getBytes(String columnLabel) { return new byte[0]; }
        @Override public java.sql.Date getDate(String columnLabel) { return null; }
        @Override public java.sql.Time getTime(String columnLabel) { return null; }
        @Override public java.sql.Timestamp getTimestamp(String columnLabel) { return null; }
        @Override public java.io.InputStream getAsciiStream(String columnLabel) { return null; }
        @Override public java.io.InputStream getUnicodeStream(String columnLabel) { return null; }
        @Override public java.io.InputStream getBinaryStream(String columnLabel) { return null; }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("unsupported"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
        @Override public java.sql.ResultSetMetaData getMetaData() { return null; }
        @Override public int findColumn(String columnLabel) { return 0; }
        @Override public boolean isBeforeFirst() { return false; }
        @Override public boolean isAfterLast() { return false; }
        @Override public boolean isFirst() { return false; }
        @Override public boolean isLast() { return false; }
        @Override public void beforeFirst() {}
        @Override public void afterLast() {}
        @Override public boolean first() { return false; }
        @Override public boolean last() { return false; }
        @Override public int getRow() { return 0; }
        @Override public boolean absolute(int row) { return false; }
        @Override public boolean relative(int rows) { return false; }
        @Override public boolean previous() { return false; }
        @Override public void setFetchDirection(int direction) {}
        @Override public int getFetchDirection() { return ResultSet.FETCH_FORWARD; }
        @Override public void setFetchSize(int rows) {}
        @Override public int getFetchSize() { return 0; }
        @Override public int getType() { return ResultSet.TYPE_FORWARD_ONLY; }
        @Override public int getConcurrency() { return ResultSet.CONCUR_READ_ONLY; }
        @Override public boolean rowUpdated() { return false; }
        @Override public boolean rowInserted() { return false; }
        @Override public boolean rowDeleted() { return false; }
        @Override public void updateNull(int columnIndex) {}
        @Override public void updateBoolean(int columnIndex, boolean x) {}
        @Override public void updateByte(int columnIndex, byte x) {}
        @Override public void updateShort(int columnIndex, short x) {}
        @Override public void updateInt(int columnIndex, int x) {}
        @Override public void updateLong(int columnIndex, long x) {}
        @Override public void updateFloat(int columnIndex, float x) {}
        @Override public void updateDouble(int columnIndex, double x) {}
        @Override public void updateBigDecimal(int columnIndex, java.math.BigDecimal x) {}
        @Override public void updateString(int columnIndex, String x) {}
        @Override public void updateBytes(int columnIndex, byte[] x) {}
        @Override public void updateDate(int columnIndex, java.sql.Date x) {}
        @Override public void updateTime(int columnIndex, java.sql.Time x) {}
        @Override public void updateTimestamp(int columnIndex, java.sql.Timestamp x) {}
        @Override public void updateAsciiStream(int columnIndex, java.io.InputStream x, int length) {}
        @Override public void updateBinaryStream(int columnIndex, java.io.InputStream x, int length) {}
        @Override public void updateCharacterStream(int columnIndex, java.io.Reader x, int length) {}
        @Override public void updateObject(int columnIndex, Object x, int scaleOrLength) {}
        @Override public void updateObject(int columnIndex, Object x) {}
        @Override public void updateNull(String columnLabel) {}
        @Override public void updateBoolean(String columnLabel, boolean x) {}
        @Override public void updateByte(String columnLabel, byte x) {}
        @Override public void updateShort(String columnLabel, short x) {}
        @Override public void updateInt(String columnLabel, int x) {}
        @Override public void updateLong(String columnLabel, long x) {}
        @Override public void updateFloat(String columnLabel, float x) {}
        @Override public void updateDouble(String columnLabel, double x) {}
        @Override public void updateBigDecimal(String columnLabel, java.math.BigDecimal x) {}
        @Override public void updateString(String columnLabel, String x) {}
        @Override public void updateBytes(String columnLabel, byte[] x) {}
        @Override public void updateDate(String columnLabel, java.sql.Date x) {}
        @Override public void updateTime(String columnLabel, java.sql.Time x) {}
        @Override public void updateTimestamp(String columnLabel, java.sql.Timestamp x) {}
        @Override public void updateAsciiStream(String columnLabel, java.io.InputStream x, int length) {}
        @Override public void updateBinaryStream(String columnLabel, java.io.InputStream x, int length) {}
        @Override public void updateCharacterStream(String columnLabel, java.io.Reader reader, int length) {}
        @Override public void updateObject(String columnLabel, Object x, int scaleOrLength) {}
        @Override public void updateObject(String columnLabel, Object x) {}
        @Override public void insertRow() {}
        @Override public void updateRow() {}
        @Override public void deleteRow() {}
        @Override public void refreshRow() {}
        @Override public void cancelRowUpdates() {}
        @Override public void moveToInsertRow() {}
        @Override public void moveToCurrentRow() {}
        @Override public Statement getStatement() { return null; }
        @Override public Object getObject(int columnIndex) { return null; }
        @Override public Object getObject(String columnLabel) { return null; }
        @Override public int getHoldability() { return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
        @Override public boolean isClosed() { return false; }
        @Override public void updateNString(int columnIndex, String nString) {}
        @Override public void updateNString(String columnLabel, String nString) {}
        @Override public <T> T getObject(int columnIndex, Class<T> type) { return null; }
        @Override public <T> T getObject(String columnLabel, Class<T> type) { return null; }
    }
}