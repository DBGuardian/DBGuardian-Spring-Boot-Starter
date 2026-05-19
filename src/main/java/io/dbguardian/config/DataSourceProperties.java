package io.dbguardian.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DBGuardian 数据源配置属性
 * 对应 application.yml 中的 spring.datasource 配置
 */
@Data
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties {

    /** 是否启用 DBGuardian 读写分离 */
    private boolean enabled = true;

    /** 是否允许降级启动（数据库不可用时） */
    private boolean allowDegradedStartup = false;

    /** 主库配置 */
    private MasterProperties master = new MasterProperties();

    /** 从库配置 */
    private SlaveProperties slave = new SlaveProperties();

    /** 主从复制配置 */
    private ReplicationProperties replication = new ReplicationProperties();

    /**
     * 主库配置
     */
    @Data
    public static class MasterProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private HikariProperties hikari = new HikariProperties();
    }

    /**
     * 从库配置
     */
    @Data
    public static class SlaveProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private HikariProperties hikari = new HikariProperties();
    }

    /**
     * HikariCP 连接池配置
     */
    @Data
    public static class HikariProperties {
        private int maximumPoolSize = 20;
        private int minimumIdle = 5;
        private long connectionTimeout = 30000;
        private long idleTimeout = 600000;
        private long maxLifetime = 1800000;
        private String poolName;
    }

    /**
     * 主从复制配置
     */
    @Data
    public static class ReplicationProperties {
        /** 主库主机地址 */
        private String masterHost;
        /** 主库端口 */
        private int masterPort = 3306;
        /** 复制用户 */
        private String masterUser = "repl";
        /** 复制密码 */
        private String masterPassword;
        /** 是否自动重连 */
        private boolean autoReconnect = true;
    }
}
