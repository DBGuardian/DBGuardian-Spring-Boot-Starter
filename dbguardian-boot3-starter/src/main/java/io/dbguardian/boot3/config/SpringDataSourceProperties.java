package io.dbguardian.boot3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring DataSource 属性配置
 * 支持 spring.datasource.master 和 spring.datasource.slave 配置
 */
@ConfigurationProperties(prefix = "spring.datasource")
public class SpringDataSourceProperties {

    private NodeProperties master = new NodeProperties();
    private NodeProperties slave = new NodeProperties();
    private ReplicationProperties replication = new ReplicationProperties();
    private RecoveryProperties recovery = new RecoveryProperties();

    public NodeProperties getMaster() {
        return master;
    }

    public void setMaster(NodeProperties master) {
        this.master = master;
    }

    public NodeProperties getSlave() {
        return slave;
    }

    public void setSlave(NodeProperties slave) {
        this.slave = slave;
    }

    public ReplicationProperties getReplication() {
        return replication;
    }

    public void setReplication(ReplicationProperties replication) {
        this.replication = replication;
    }

    public RecoveryProperties getRecovery() {
        return recovery;
    }

    public void setRecovery(RecoveryProperties recovery) {
        this.recovery = recovery;
    }

    public static class NodeProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private HikariProperties hikari = new HikariProperties();

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public HikariProperties getHikari() {
            return hikari;
        }

        public void setHikari(HikariProperties hikari) {
            this.hikari = hikari;
        }
    }

    public static class HikariProperties {
        private int maximumPoolSize = 10;
        private int minimumIdle = 2;
        private long connectionTimeout = 30000;
        private long idleTimeout = 600000;
        private long maxLifetime = 1800000;
        private String poolName;

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }

        public long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public long getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(long idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public long getMaxLifetime() {
            return maxLifetime;
        }

        public void setMaxLifetime(long maxLifetime) {
            this.maxLifetime = maxLifetime;
        }

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }
    }

    public static class ReplicationProperties {
        private String masterHost;
        private int masterPort = 3306;
        private String masterUser;
        private String masterPassword;
        private boolean autoReconnect = true;

        public String getMasterHost() {
            return masterHost;
        }

        public void setMasterHost(String masterHost) {
            this.masterHost = masterHost;
        }

        public int getMasterPort() {
            return masterPort;
        }

        public void setMasterPort(int masterPort) {
            this.masterPort = masterPort;
        }

        public String getMasterUser() {
            return masterUser;
        }

        public void setMasterUser(String masterUser) {
            this.masterUser = masterUser;
        }

        public String getMasterPassword() {
            return masterPassword;
        }

        public void setMasterPassword(String masterPassword) {
            this.masterPassword = masterPassword;
        }

        public boolean isAutoReconnect() {
            return autoReconnect;
        }

        public void setAutoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
        }
    }

    public static class RecoveryProperties {
        private int catchupTimeoutSeconds = 300;
        private int catchupCheckIntervalSeconds = 10;

        public int getCatchupTimeoutSeconds() {
            return catchupTimeoutSeconds;
        }

        public void setCatchupTimeoutSeconds(int catchupTimeoutSeconds) {
            this.catchupTimeoutSeconds = catchupTimeoutSeconds;
        }

        public int getCatchupCheckIntervalSeconds() {
            return catchupCheckIntervalSeconds;
        }

        public void setCatchupCheckIntervalSeconds(int catchupCheckIntervalSeconds) {
            this.catchupCheckIntervalSeconds = catchupCheckIntervalSeconds;
        }
    }
}
