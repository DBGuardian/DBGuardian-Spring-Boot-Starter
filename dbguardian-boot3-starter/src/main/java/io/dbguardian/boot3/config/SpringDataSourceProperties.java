package io.dbguardian.boot3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring DataSource 属性配置
 * 支持 spring.datasource.master 和 spring.datasource.slave 配置
 */
@ConfigurationProperties(prefix = "spring.datasource")
public class SpringDataSourceProperties {

    private boolean enabled = true;
    private boolean allowDegradedStartup = true;
    private NodeProperties master = new NodeProperties();
    private NodeProperties slave = new NodeProperties();
    private ReplicationProperties replication = new ReplicationProperties();
    private RuntimeProperties runtime = new RuntimeProperties();
    private RecoveryProperties recovery = new RecoveryProperties();
    private GtidProtectionProperties gtidProtection = new GtidProtectionProperties();

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

    public RuntimeProperties getRuntime() {
        return runtime;
    }

    public GtidProtectionProperties getGtidProtection() {
        return gtidProtection;
    }

    public static class NodeProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
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
        private boolean enabled = true;
        private boolean autoFailback = true;
        private int catchupTimeoutSeconds = 300;
        private int catchupCheckIntervalSeconds = 2;
        private PostgreSqlRecoveryProperties postgresql = new PostgreSqlRecoveryProperties();

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

        public PostgreSqlRecoveryProperties getPostgresql() {
            return postgresql;
        }

        public void setPostgresql(PostgreSqlRecoveryProperties postgresql) {
            this.postgresql = postgresql;
        }
    }

    public static class PostgreSqlRecoveryProperties {
        private boolean enabled;
        private boolean keepPromotedPrimary = true;
        private String dataDirectory;
        private String configFile;
        private String scriptPath;
        private String pgCtlCommand = "pg_ctl";
        private String psqlCommand = "psql";
        private String pgRewindCommand = "pg_rewind";
        private String localHost = "127.0.0.1";
        private int localPort = 5432;
        private String localDatabase = "postgres";
        private String primarySlotName;
        private String extraPrimaryConnInfo;
        private int startupWaitSeconds = 60;
        private int commandTimeoutSeconds = 900;
        private boolean remoteExecutionEnabled;
        private String remoteExecutorCommand;
        private String remoteExecutorPassword;
        private String failbackDataDirectory;
        private String failbackConfigFile;
        private String failbackScriptPath;
        private String failbackPgCtlCommand;
        private String failbackPsqlCommand;
        private String failbackPgRewindCommand;
        private String failbackLocalHost;
        private int failbackLocalPort;
        private String failbackLocalDatabase;
        private String failbackPrimarySlotName;
        private String failbackExtraPrimaryConnInfo;
        private int failbackStartupWaitSeconds;
        private int failbackCommandTimeoutSeconds;
        private boolean failbackRemoteExecutionEnabled;
        private String failbackRemoteExecutorCommand;
        private String failbackRemoteExecutorPassword;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isKeepPromotedPrimary() {
            return keepPromotedPrimary;
        }

        public void setKeepPromotedPrimary(boolean keepPromotedPrimary) {
            this.keepPromotedPrimary = keepPromotedPrimary;
        }

        public String getDataDirectory() {
            return dataDirectory;
        }

        public void setDataDirectory(String dataDirectory) {
            this.dataDirectory = dataDirectory;
        }

        public String getConfigFile() {
            return configFile;
        }

        public void setConfigFile(String configFile) {
            this.configFile = configFile;
        }

        public String getScriptPath() {
            return scriptPath;
        }

        public void setScriptPath(String scriptPath) {
            this.scriptPath = scriptPath;
        }

        public String getPgCtlCommand() {
            return pgCtlCommand;
        }

        public void setPgCtlCommand(String pgCtlCommand) {
            this.pgCtlCommand = pgCtlCommand;
        }

        public String getPsqlCommand() {
            return psqlCommand;
        }

        public void setPsqlCommand(String psqlCommand) {
            this.psqlCommand = psqlCommand;
        }

        public String getPgRewindCommand() {
            return pgRewindCommand;
        }

        public void setPgRewindCommand(String pgRewindCommand) {
            this.pgRewindCommand = pgRewindCommand;
        }

        public String getLocalHost() {
            return localHost;
        }

        public void setLocalHost(String localHost) {
            this.localHost = localHost;
        }

        public int getLocalPort() {
            return localPort;
        }

        public void setLocalPort(int localPort) {
            this.localPort = localPort;
        }

        public String getLocalDatabase() {
            return localDatabase;
        }

        public void setLocalDatabase(String localDatabase) {
            this.localDatabase = localDatabase;
        }

        public String getPrimarySlotName() {
            return primarySlotName;
        }

        public void setPrimarySlotName(String primarySlotName) {
            this.primarySlotName = primarySlotName;
        }

        public String getExtraPrimaryConnInfo() {
            return extraPrimaryConnInfo;
        }

        public void setExtraPrimaryConnInfo(String extraPrimaryConnInfo) {
            this.extraPrimaryConnInfo = extraPrimaryConnInfo;
        }

        public int getStartupWaitSeconds() {
            return startupWaitSeconds;
        }

        public void setStartupWaitSeconds(int startupWaitSeconds) {
            this.startupWaitSeconds = startupWaitSeconds;
        }

        public int getCommandTimeoutSeconds() {
            return commandTimeoutSeconds;
        }

        public void setCommandTimeoutSeconds(int commandTimeoutSeconds) {
            this.commandTimeoutSeconds = commandTimeoutSeconds;
        }

        public boolean isRemoteExecutionEnabled() {
            return remoteExecutionEnabled;
        }

        public void setRemoteExecutionEnabled(boolean remoteExecutionEnabled) {
            this.remoteExecutionEnabled = remoteExecutionEnabled;
        }

        public String getRemoteExecutorCommand() {
            return remoteExecutorCommand;
        }

        public void setRemoteExecutorCommand(String remoteExecutorCommand) {
            this.remoteExecutorCommand = remoteExecutorCommand;
        }

        public String getRemoteExecutorPassword() {
            return remoteExecutorPassword;
        }

        public void setRemoteExecutorPassword(String remoteExecutorPassword) {
            this.remoteExecutorPassword = remoteExecutorPassword;
        }

        public String getFailbackDataDirectory() {
            return failbackDataDirectory;
        }

        public void setFailbackDataDirectory(String failbackDataDirectory) {
            this.failbackDataDirectory = failbackDataDirectory;
        }

        public String getFailbackConfigFile() {
            return failbackConfigFile;
        }

        public void setFailbackConfigFile(String failbackConfigFile) {
            this.failbackConfigFile = failbackConfigFile;
        }

        public String getFailbackScriptPath() {
            return failbackScriptPath;
        }

        public void setFailbackScriptPath(String failbackScriptPath) {
            this.failbackScriptPath = failbackScriptPath;
        }

        public String getFailbackPgCtlCommand() {
            return failbackPgCtlCommand;
        }

        public void setFailbackPgCtlCommand(String failbackPgCtlCommand) {
            this.failbackPgCtlCommand = failbackPgCtlCommand;
        }

        public String getFailbackPsqlCommand() {
            return failbackPsqlCommand;
        }

        public void setFailbackPsqlCommand(String failbackPsqlCommand) {
            this.failbackPsqlCommand = failbackPsqlCommand;
        }

        public String getFailbackPgRewindCommand() {
            return failbackPgRewindCommand;
        }

        public void setFailbackPgRewindCommand(String failbackPgRewindCommand) {
            this.failbackPgRewindCommand = failbackPgRewindCommand;
        }

        public String getFailbackLocalHost() {
            return failbackLocalHost;
        }

        public void setFailbackLocalHost(String failbackLocalHost) {
            this.failbackLocalHost = failbackLocalHost;
        }

        public int getFailbackLocalPort() {
            return failbackLocalPort;
        }

        public void setFailbackLocalPort(int failbackLocalPort) {
            this.failbackLocalPort = failbackLocalPort;
        }

        public String getFailbackLocalDatabase() {
            return failbackLocalDatabase;
        }

        public void setFailbackLocalDatabase(String failbackLocalDatabase) {
            this.failbackLocalDatabase = failbackLocalDatabase;
        }

        public String getFailbackPrimarySlotName() {
            return failbackPrimarySlotName;
        }

        public void setFailbackPrimarySlotName(String failbackPrimarySlotName) {
            this.failbackPrimarySlotName = failbackPrimarySlotName;
        }

        public String getFailbackExtraPrimaryConnInfo() {
            return failbackExtraPrimaryConnInfo;
        }

        public void setFailbackExtraPrimaryConnInfo(String failbackExtraPrimaryConnInfo) {
            this.failbackExtraPrimaryConnInfo = failbackExtraPrimaryConnInfo;
        }

        public int getFailbackStartupWaitSeconds() {
            return failbackStartupWaitSeconds;
        }

        public void setFailbackStartupWaitSeconds(int failbackStartupWaitSeconds) {
            this.failbackStartupWaitSeconds = failbackStartupWaitSeconds;
        }

        public int getFailbackCommandTimeoutSeconds() {
            return failbackCommandTimeoutSeconds;
        }

        public void setFailbackCommandTimeoutSeconds(int failbackCommandTimeoutSeconds) {
            this.failbackCommandTimeoutSeconds = failbackCommandTimeoutSeconds;
        }

        public boolean isFailbackRemoteExecutionEnabled() {
            return failbackRemoteExecutionEnabled;
        }

        public void setFailbackRemoteExecutionEnabled(boolean failbackRemoteExecutionEnabled) {
            this.failbackRemoteExecutionEnabled = failbackRemoteExecutionEnabled;
        }

        public String getFailbackRemoteExecutorCommand() {
            return failbackRemoteExecutorCommand;
        }

        public void setFailbackRemoteExecutorCommand(String failbackRemoteExecutorCommand) {
            this.failbackRemoteExecutorCommand = failbackRemoteExecutorCommand;
        }

        public String getFailbackRemoteExecutorPassword() {
            return failbackRemoteExecutorPassword;
        }

        public void setFailbackRemoteExecutorPassword(String failbackRemoteExecutorPassword) {
            this.failbackRemoteExecutorPassword = failbackRemoteExecutorPassword;
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
