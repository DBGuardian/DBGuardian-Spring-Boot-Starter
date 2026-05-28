package io.dbguardian.boot2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.datasource")
public class SpringDataSourceProperties {

    private boolean enabled = true;
    private boolean allowDegradedStartup = true;
    private final NodeProperties master = new NodeProperties();
    private final NodeProperties slave = new NodeProperties();
    private final ReplicationProperties replication = new ReplicationProperties();
    private final RuntimeProperties runtime = new RuntimeProperties();
    private final RecoveryProperties recovery = new RecoveryProperties();
    private final GtidProtectionProperties gtidProtection = new GtidProtectionProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllowDegradedStartup() {
        return allowDegradedStartup;
    }

    public void setAllowDegradedStartup(boolean allowDegradedStartup) {
        this.allowDegradedStartup = allowDegradedStartup;
    }

    public NodeProperties getMaster() {
        return master;
    }

    public NodeProperties getSlave() {
        return slave;
    }

    public ReplicationProperties getReplication() {
        return replication;
    }

    public RuntimeProperties getRuntime() {
        return runtime;
    }

    public RecoveryProperties getRecovery() {
        return recovery;
    }

    public GtidProtectionProperties getGtidProtection() {
        return gtidProtection;
    }

    public static class NodeProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private final HikariProperties hikari = new HikariProperties();

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
    }

    public static class HikariProperties {
        private int maximumPoolSize = 20;
        private int minimumIdle = 5;
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
        private String masterUser = "repl";
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

    public static class RuntimeProperties {
        private int masterCheckIntervalSeconds = 30;
        private int slaveCheckIntervalSeconds = 5;
        private long orchestrationIntervalMs = 30000L;
        private boolean periodicHealthCheckEnabled = true;

        public int getMasterCheckIntervalSeconds() {
            return masterCheckIntervalSeconds;
        }

        public void setMasterCheckIntervalSeconds(int masterCheckIntervalSeconds) {
            this.masterCheckIntervalSeconds = masterCheckIntervalSeconds;
        }

        public int getSlaveCheckIntervalSeconds() {
            return slaveCheckIntervalSeconds;
        }

        public void setSlaveCheckIntervalSeconds(int slaveCheckIntervalSeconds) {
            this.slaveCheckIntervalSeconds = slaveCheckIntervalSeconds;
        }

        public long getOrchestrationIntervalMs() {
            return orchestrationIntervalMs;
        }

        public void setOrchestrationIntervalMs(long orchestrationIntervalMs) {
            this.orchestrationIntervalMs = orchestrationIntervalMs;
        }

        public boolean isPeriodicHealthCheckEnabled() {
            return periodicHealthCheckEnabled;
        }

        public void setPeriodicHealthCheckEnabled(boolean periodicHealthCheckEnabled) {
            this.periodicHealthCheckEnabled = periodicHealthCheckEnabled;
        }
    }

    public static class RecoveryProperties {
        private boolean enabled = true;
        private boolean autoFailback = true;
        private int catchupTimeoutSeconds = 300;
        private int catchupCheckIntervalSeconds = 2;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAutoFailback() {
            return autoFailback;
        }

        public void setAutoFailback(boolean autoFailback) {
            this.autoFailback = autoFailback;
        }

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

    public static class GtidProtectionProperties {
        private boolean enabled;
        private boolean blockSlaveReadsOnRisk = true;
        private long maxAllowedLagSeconds;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isBlockSlaveReadsOnRisk() {
            return blockSlaveReadsOnRisk;
        }

        public void setBlockSlaveReadsOnRisk(boolean blockSlaveReadsOnRisk) {
            this.blockSlaveReadsOnRisk = blockSlaveReadsOnRisk;
        }

        public long getMaxAllowedLagSeconds() {
            return maxAllowedLagSeconds;
        }

        public void setMaxAllowedLagSeconds(long maxAllowedLagSeconds) {
            this.maxAllowedLagSeconds = maxAllowedLagSeconds;
        }
    }
}