package io.dbguardian.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库适配器配置
 * 统一管理不同数据库的特定配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.datasource.dbguardian.adapters")
public class DatabaseAdapterProperties {

    /** 是否启用数据库适配 */
    private boolean enabled = true;

    /** 当前数据库类型: mysql, postgresql, oracle, etc. */
    private String currentDatabase;

    /** 数据库适配器配置 */
    private Map<String, DatabaseAdapter> adapters = new HashMap<>();

    /**
     * 数据库适配器配置
     */
    @Data
    public static class DatabaseAdapter {
        /** 适配器名称 */
        private String name;

        /** 驱动类名 */
        private String driverClassName;

        /** 默认端口 */
        private int defaultPort = 3306;

        /** 连接URL前缀 */
        private String urlPrefix;

        /** 分页SQL格式 */
        private PaginationFormat pagination = new PaginationFormat();

        /** 健康检查SQL */
        private String healthCheckSql = "SELECT 1";

        /** 只读模式设置SQL */
        private ReadOnlySettings readOnlySettings = new ReadOnlySettings();

        /** 主从复制相关SQL */
        private ReplicationSettings replicationSettings = new ReplicationSettings();
    }

    /**
     * 分页格式配置
     */
    @Data
    public static class PaginationFormat {
        /** LIMIT OFFSET 格式 (MySQL) */
        private String limitOffsetTemplate = "LIMIT %d OFFSET %d";

        /** LIMIT 格式 (标准) */
        private String limitTemplate = "LIMIT %d";

        /** TOP 格式 (SQL Server) */
        private String topTemplate = "TOP %d";

        /** FETCH NEXT 格式 (PostgreSQL/Oracle) */
        private String fetchNextTemplate = "FETCH NEXT %d ROWS ONLY";

        /** OFFSET 格式 (PostgreSQL/Oracle) */
        private String offsetTemplate = "OFFSET %d ROWS";
    }

    /**
     * 只读模式设置
     */
    @Data
    public static class ReadOnlySettings {
        /** 开启只读SQL */
        private String setReadOnlyOn = "SET GLOBAL READ_ONLY=ON";

        /** 关闭只读SQL */
        private String setReadOnlyOff = "SET GLOBAL READ_ONLY=OFF";

        /** 开启超级只读SQL */
        private String setSuperReadOnlyOn = "SET GLOBAL SUPER_READ_ONLY=ON";

        /** 关闭超级只读SQL */
        private String setSuperReadOnlyOff = "SET GLOBAL SUPER_READ_ONLY=OFF";
    }

    /**
     * 主从复制设置
     */
    @Data
    public static class ReplicationSettings {
        /** 停止复制SQL */
        private String stopSlave = "STOP SLAVE";

        /** 重置复制SQL */
        private String resetSlave = "RESET SLAVE ALL";

        /** 启动复制SQL */
        private String startSlave = "START SLAVE";

        /** 变更主库SQL模板 */
        private String changeMasterToTemplate = "CHANGE MASTER TO MASTER_HOST='%s', MASTER_PORT=%d, MASTER_USER='%s', MASTER_PASSWORD='%s', MASTER_AUTO_POSITION=1";

        /** 获取GTID位置SQL */
        private String showMasterStatus = "SHOW MASTER STATUS";

        /** 获取从库状态SQL */
        private String showSlaveStatus = "SHOW SLAVE STATUS";
    }

    /**
     * 获取当前数据库的适配器配置
     */
    public DatabaseAdapter getCurrentAdapter() {
        if (currentDatabase == null) {
            return adapters.get("mysql"); // 默认返回MySQL
        }
        return adapters.getOrDefault(currentDatabase, adapters.get("mysql"));
    }

    /**
     * 根据数据库类型获取适配器配置
     */
    public DatabaseAdapter getAdapter(String databaseType) {
        return adapters.getOrDefault(databaseType, adapters.get("mysql"));
    }

    /**
     * 预定义的数据库适配器
     */
    public static Map<String, DatabaseAdapter> getDefaultAdapters() {
        Map<String, DatabaseAdapter> defaults = new HashMap<>();

        // MySQL
        defaults.put("mysql", createMySQLAdapter());

        // PostgreSQL
        defaults.put("postgresql", createPostgreSQLAdapter());

        // Oracle
        defaults.put("oracle", createOracleAdapter());

        // SQL Server
        defaults.put("sqlserver", createSQLServerAdapter());

        // MariaDB
        defaults.put("mariadb", createMariaDBAdapter());

        return defaults;
    }

    private static DatabaseAdapter createMySQLAdapter() {
        DatabaseAdapter adapter = new DatabaseAdapter();
        adapter.setName("MySQL");
        adapter.setDriverClassName("com.mysql.cj.jdbc.Driver");
        adapter.setDefaultPort(3306);
        adapter.setUrlPrefix("jdbc:mysql://");
        adapter.setHealthCheckSql("SELECT 1");

        ReplicationSettings replication = new ReplicationSettings();
        replication.setStopSlave("STOP SLAVE");
        replication.setResetSlave("RESET SLAVE ALL");
        replication.setStartSlave("START SLAVE");
        replication.setChangeMasterToTemplate("CHANGE MASTER TO MASTER_HOST='%s', MASTER_PORT=%d, MASTER_USER='%s', MASTER_PASSWORD='%s', MASTER_AUTO_POSITION=1");
        replication.setShowMasterStatus("SHOW MASTER STATUS");
        replication.setShowSlaveStatus("SHOW SLAVE STATUS");
        adapter.setReplicationSettings(replication);

        ReadOnlySettings readOnly = new ReadOnlySettings();
        readOnly.setSetReadOnlyOn("SET GLOBAL READ_ONLY=ON");
        readOnly.setSetReadOnlyOff("SET GLOBAL READ_ONLY=OFF");
        readOnly.setSetSuperReadOnlyOn("SET GLOBAL SUPER_READ_ONLY=ON");
        readOnly.setSetSuperReadOnlyOff("SET GLOBAL SUPER_READ_ONLY=OFF");
        adapter.setReadOnlySettings(readOnly);

        return adapter;
    }

    private static DatabaseAdapter createPostgreSQLAdapter() {
        DatabaseAdapter adapter = new DatabaseAdapter();
        adapter.setName("PostgreSQL");
        adapter.setDriverClassName("org.postgresql.Driver");
        adapter.setDefaultPort(5432);
        adapter.setUrlPrefix("jdbc:postgresql://");
        adapter.setHealthCheckSql("SELECT 1");

        PaginationFormat pagination = new PaginationFormat();
        pagination.setLimitOffsetTemplate("LIMIT %d OFFSET %d");
        pagination.setFetchNextTemplate("FETCH NEXT %d ROWS ONLY");
        pagination.setOffsetTemplate("OFFSET %d ROWS");
        adapter.setPagination(pagination);

        // PostgreSQL 使用流复制，没有内置的读写分离命令
        ReplicationSettings replication = new ReplicationSettings();
        replication.setStopSlave("-- PostgreSQL uses streaming replication");
        replication.setResetSlave("-- PostgreSQL uses streaming replication");
        replication.setStartSlave("-- PostgreSQL uses streaming replication");
        adapter.setReplicationSettings(replication);

        return adapter;
    }

    private static DatabaseAdapter createOracleAdapter() {
        DatabaseAdapter adapter = new DatabaseAdapter();
        adapter.setName("Oracle");
        adapter.setDriverClassName("oracle.jdbc.OracleDriver");
        adapter.setDefaultPort(1521);
        adapter.setUrlPrefix("jdbc:oracle:thin:@");
        adapter.setHealthCheckSql("SELECT 1 FROM DUAL");

        PaginationFormat pagination = new PaginationFormat();
        pagination.setFetchNextTemplate("FETCH FIRST %d ROWS ONLY");
        pagination.setOffsetTemplate("OFFSET %d ROWS");
        adapter.setPagination(pagination);

        ReplicationSettings replication = new ReplicationSettings();
        // Oracle 使用 Data Guard / GoldenGate 进行复制
        replication.setStopSlave("-- Oracle uses Data Guard");
        replication.setResetSlave("-- Oracle uses Data Guard");
        replication.setStartSlave("-- Oracle uses Data Guard");
        adapter.setReplicationSettings(replication);

        return adapter;
    }

    private static DatabaseAdapter createSQLServerAdapter() {
        DatabaseAdapter adapter = new DatabaseAdapter();
        adapter.setName("SQL Server");
        adapter.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        adapter.setDefaultPort(1433);
        adapter.setUrlPrefix("jdbc:sqlserver://");
        adapter.setHealthCheckSql("SELECT 1");

        PaginationFormat pagination = new PaginationFormat();
        pagination.setTopTemplate("TOP %d");
        pagination.setOffsetTemplate("OFFSET %d ROWS");
        adapter.setPagination(pagination);

        ReplicationSettings replication = new ReplicationSettings();
        // SQL Server 使用 Always On
        replication.setStopSlave("-- SQL Server uses Always On");
        replication.setResetSlave("-- SQL Server uses Always On");
        replication.setStartSlave("-- SQL Server uses Always On");
        adapter.setReplicationSettings(replication);

        return adapter;
    }

    private static DatabaseAdapter createMariaDBAdapter() {
        DatabaseAdapter adapter = createMySQLAdapter();
        adapter.setName("MariaDB");
        adapter.setDriverClassName("org.mariadb.jdbc.Driver");
        adapter.setUrlPrefix("jdbc:mariadb://");
        return adapter;
    }
}
