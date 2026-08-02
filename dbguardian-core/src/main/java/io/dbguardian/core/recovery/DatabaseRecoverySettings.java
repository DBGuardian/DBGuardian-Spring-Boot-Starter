package io.dbguardian.core.recovery;

public class DatabaseRecoverySettings {

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