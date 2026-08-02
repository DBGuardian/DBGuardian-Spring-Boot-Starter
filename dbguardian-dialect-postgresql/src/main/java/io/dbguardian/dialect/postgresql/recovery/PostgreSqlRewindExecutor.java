package io.dbguardian.dialect.postgresql.recovery;

import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.core.recovery.DatabaseRecoverySettings;
import io.dbguardian.spi.DatabaseDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PostgreSqlRewindExecutor {

    private static final Logger log = LoggerFactory.getLogger(PostgreSqlRewindExecutor.class);
    private static final String WINDOWS_SCRIPT_RESOURCE = "/io/dbguardian/dialect/postgresql/recovery/pg-rewind-recover.bat";
    private static final String UNIX_SCRIPT_RESOURCE = "/io/dbguardian/dialect/postgresql/recovery/pg-rewind-recover.sh";

    public boolean execute(DatabaseRecoverySettings settings,
                           DataSourceWrapper originalMaster,
                           DataSourceWrapper promotedMaster,
                           DatabaseDialect dialect,
                           String replicationUser,
                           String replicationPassword) {
        return executeInternal(
                settings,
                originalMaster,
                promotedMaster,
                dialect,
                replicationUser,
                replicationPassword,
                resolveUsername(originalMaster),
                resolvePassword(originalMaster),
                false,
                "PostgreSQL 自动追赶",
                originalMaster == null ? null : originalMaster.getId()
        );
    }

    public boolean recoverReplicaFromPrimary(DatabaseRecoverySettings settings,
                                             DataSourceWrapper recoveredReplica,
                                             DataSourceWrapper currentPrimary,
                                             DatabaseDialect dialect,
                                             String replicationUser,
                                             String replicationPassword) {
        return executeInternal(
                settings,
                recoveredReplica,
                currentPrimary,
                dialect,
                replicationUser,
                replicationPassword,
                resolveUsername(recoveredReplica),
                resolvePassword(recoveredReplica),
                true,
                "PostgreSQL 从库复制重建",
                recoveredReplica == null ? null : recoveredReplica.getId()
        );
    }

    public boolean executeFailback(DatabaseRecoverySettings settings,
                                   DataSourceWrapper promotedMaster,
                                   DataSourceWrapper restoredOriginalMaster,
                                   DatabaseDialect dialect,
                                   String replicationUser,
                                   String replicationPassword) {
        return executeInternal(
                settings,
                promotedMaster,
                restoredOriginalMaster,
                dialect,
                replicationUser,
                replicationPassword,
                resolveUsername(promotedMaster),
                resolvePassword(promotedMaster),
                true,
                "PostgreSQL 回切重挂",
                promotedMaster == null ? null : promotedMaster.getId()
        );
    }

    private boolean executeInternal(DatabaseRecoverySettings settings,
                                    DataSourceWrapper localNode,
                                    DataSourceWrapper targetPrimary,
                                    DatabaseDialect dialect,
                                    String replicationUser,
                                    String replicationPassword,
                                    String localUsername,
                                    String localPassword,
                                    boolean failback,
                                    String actionName,
                                    String nodeId) {
        log.info("{}执行器入口: nodeId={}, failback={}, localNode={}, targetPrimary={}",
                actionName,
                nodeId,
                failback,
                localNode == null ? null : localNode.getId(),
                targetPrimary == null ? null : targetPrimary.getId());
        if (settings == null || !settings.isEnabled()) {
            log.warn("{}跳过执行: nodeId={}, reason=recovery_settings_disabled", actionName, nodeId);
            return false;
        }
        if (localNode == null || targetPrimary == null || dialect == null) {
            log.warn("{}跳过执行: nodeId={}, reason=missing_context, localNodePresent={}, targetPrimaryPresent={}, dialectPresent={}",
                    actionName,
                    nodeId,
                    localNode != null,
                    targetPrimary != null,
                    dialect != null);
            return false;
        }
        String dataDirectory = resolveDataDirectory(settings, failback);
        if (isBlank(dataDirectory)) {
            log.warn("{}未配置 dataDirectory，跳过执行: nodeId={}", actionName, nodeId);
            return false;
        }
        if (isBlank(replicationUser) || isBlank(replicationPassword)) {
            log.warn("{}缺少复制账号，跳过执行: nodeId={}", actionName, nodeId);
            return false;
        }

        boolean remoteExecutionEnabled = isRemoteExecutionEnabled(settings, failback);
        Map<String, String> env = buildEnvironment(settings, failback, localNode, targetPrimary, dialect,
                replicationUser, replicationPassword, localUsername, localPassword, dataDirectory);
        int timeoutSeconds = resolveCommandTimeoutSeconds(settings, failback);
        log.info("{}执行器配置: nodeId={}, mode={}, dataDirectory={}, localHost={}, localPort={}, targetHost={}, targetPort={}, timeoutSeconds={}",
                actionName,
                nodeId,
                remoteExecutionEnabled ? "remote" : "local",
                dataDirectory,
                env.get("DBGUARDIAN_LOCAL_HOST"),
                env.get("DBGUARDIAN_LOCAL_PORT"),
                env.get("DBGUARDIAN_NEW_PRIMARY_HOST"),
                env.get("DBGUARDIAN_NEW_PRIMARY_PORT"),
                Integer.valueOf(timeoutSeconds));

        ProcessBuilder builder;
        if (remoteExecutionEnabled) {
            try {
                builder = buildRemoteProcessBuilder(settings, failback, env);
                log.info("{}使用远程代理执行: nodeId={}, scriptPath={}, remoteCommandTemplatePresent={}",
                        actionName,
                        nodeId,
                        resolveScriptPath(settings, failback),
                        !isBlank(resolveRemoteExecutorCommand(settings, failback)));
            } catch (IOException ex) {
                log.warn("准备{}远程命令失败: {}", actionName, ex.getMessage());
                return false;
            }
        } else {
            File scriptFile;
            try {
                scriptFile = resolveScriptFile(settings, failback);
            } catch (IOException ex) {
                log.warn("准备{}脚本失败: {}", actionName, ex.getMessage());
                return false;
            }
            builder = new ProcessBuilder(buildLocalCommand(scriptFile));
            builder.environment().putAll(env);
            log.info("{}使用本地脚本执行: nodeId={}, scriptPath={}", actionName, nodeId, scriptFile.getAbsolutePath());
        }
        builder.redirectErrorStream(true);

        Process process = null;
        StreamCollector collector = null;
        try {
            log.info("{}开始启动执行进程: nodeId={}", actionName, nodeId);
            process = builder.start();
            collector = new StreamCollector(process.getInputStream(), actionName, nodeId);
            collector.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                collector.join(2000L);
                log.warn("{}执行超时: nodeId={}, output={}", actionName, nodeId, collector.output());
                return false;
            }
            collector.join(2000L);
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.warn("{}执行失败: nodeId={}, exitCode={}, output={}", actionName, nodeId, Integer.valueOf(exitCode), collector.output());
                return false;
            }
            log.info("{}执行成功: nodeId={}, output={}", actionName, nodeId, collector.output());
            return true;
        } catch (Exception ex) {
            if (process != null) {
                process.destroyForcibly();
            }
            log.warn("{}执行异常: nodeId={}, reason={}", actionName, nodeId, ex.getMessage());
            return false;
        }
    }

    private Map<String, String> buildEnvironment(DatabaseRecoverySettings settings,
                                                 boolean failback,
                                                 DataSourceWrapper localNode,
                                                 DataSourceWrapper targetPrimary,
                                                 DatabaseDialect dialect,
                                                 String replicationUser,
                                                 String replicationPassword,
                                                 String localUsername,
                                                 String localPassword,
                                                 String dataDirectory) {
        Map<String, String> env = new LinkedHashMap<String, String>();
        env.put("DBGUARDIAN_OLD_PGDATA", dataDirectory);
        env.put("DBGUARDIAN_CONFIG_FILE", safe(resolveConfigFile(settings, failback)));
        env.put("DBGUARDIAN_NEW_PRIMARY_HOST", safe(dialect.getHostFromJdbcUrl(targetPrimary.getUrl())));
        env.put("DBGUARDIAN_NEW_PRIMARY_PORT", String.valueOf(dialect.getPortFromJdbcUrl(targetPrimary.getUrl())));
        env.put("DBGUARDIAN_REPL_USER", replicationUser);
        env.put("DBGUARDIAN_REPL_PASS", replicationPassword);
        env.put("DBGUARDIAN_PG_CTL", safe(resolvePgCtlCommand(settings, failback)));
        env.put("DBGUARDIAN_PSQL", safe(resolvePsqlCommand(settings, failback)));
        env.put("DBGUARDIAN_PG_REWIND", safe(resolvePgRewindCommand(settings, failback)));
        env.put("DBGUARDIAN_LOCAL_HOST", safe(resolveLocalHost(settings, failback)));
        env.put("DBGUARDIAN_LOCAL_PORT", String.valueOf(resolveLocalPort(settings, failback, dialect, localNode)));
        env.put("DBGUARDIAN_LOCAL_DB", safe(resolveLocalDatabase(settings, failback)));
        env.put("DBGUARDIAN_LOCAL_USER", safe(localUsername));
        env.put("DBGUARDIAN_LOCAL_PASS", safe(localPassword));
        env.put("DBGUARDIAN_SLOT_NAME", safe(resolvePrimarySlotName(settings, failback)));
        env.put("DBGUARDIAN_EXTRA_PRIMARY_CONNINFO", safe(resolveExtraPrimaryConnInfo(settings, failback)));
        env.put("DBGUARDIAN_STARTUP_WAIT_SECONDS", String.valueOf(resolveStartupWaitSeconds(settings, failback)));
        env.put("DBGUARDIAN_COMMAND_TIMEOUT_SECONDS", String.valueOf(resolveCommandTimeoutSeconds(settings, failback)));
        return env;
    }

    private ProcessBuilder buildRemoteProcessBuilder(DatabaseRecoverySettings settings,
                                                     boolean failback,
                                                     Map<String, String> env) throws IOException {
        String remoteCommandTemplate = resolveRemoteExecutorCommand(settings, failback);
        if (isBlank(remoteCommandTemplate)) {
            throw new IOException("remoteExecutorCommand 未配置");
        }
        String remoteScriptPath = resolveScriptPath(settings, failback);
        if (isBlank(remoteScriptPath)) {
            throw new IOException("远程执行模式要求显式配置 scriptPath 指向远程节点脚本");
        }
        String renderedCommand = renderRemoteCommand(settings, failback, remoteCommandTemplate, remoteScriptPath, env);
        return new ProcessBuilder(buildShellCommand(renderedCommand));
    }

    private List<String> buildLocalCommand(File scriptFile) {
        List<String> command = new ArrayList<String>();
        if (isWindows()) {
            command.add("cmd");
            command.add("/c");
            command.add(scriptFile.getAbsolutePath());
            return command;
        }
        command.add(scriptFile.getAbsolutePath());
        return command;
    }

    private List<String> buildShellCommand(String renderedCommand) {
        List<String> command = new ArrayList<String>();
        if (isWindows()) {
            command.add("cmd");
            command.add("/c");
            command.add(renderedCommand);
            return command;
        }
        command.add("sh");
        command.add("-lc");
        command.add(renderedCommand);
        return command;
    }

    private File resolveScriptFile(DatabaseRecoverySettings settings, boolean failback) throws IOException {
        String configuredScriptPath = resolveScriptPath(settings, failback);
        if (!isBlank(configuredScriptPath)) {
            File file = new File(configuredScriptPath);
            if (!file.isFile()) {
                throw new IOException("scriptPath 不存在: " + configuredScriptPath);
            }
            return file;
        }
        String resource = isWindows() ? WINDOWS_SCRIPT_RESOURCE : UNIX_SCRIPT_RESOURCE;
        InputStream input = PostgreSqlRewindExecutor.class.getResourceAsStream(resource);
        if (input == null) {
            throw new IOException("classpath 脚本不存在: " + resource);
        }
        String suffix = isWindows() ? ".bat" : ".sh";
        File temp = File.createTempFile("dbguardian-pg-rewind-", suffix);
        temp.deleteOnExit();
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(temp);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = input.read(buffer)) >= 0) {
                output.write(buffer, 0, len);
            }
        } finally {
            closeQuietly(output);
            closeQuietly(input);
        }
        if (!isWindows() && !temp.setExecutable(true)) {
            throw new IOException("无法设置脚本可执行权限: " + temp.getAbsolutePath());
        }
        return temp;
    }

    private String renderRemoteCommand(DatabaseRecoverySettings settings,
                                       boolean failback,
                                       String template,
                                       String scriptPath,
                                       Map<String, String> env) {
        String shellExports = buildShellExports(env);
        String scriptPathSh = quoteShellValue(scriptPath);
        String rendered = template
                .replace("{script}", quoteShellValue(scriptPath))
                .replace("{script_path}", scriptPath)
                .replace("{script_path_sh}", scriptPathSh)
                .replace("{env}", shellExports)
                .replace("{env_sh}", shellExports);
        String remoteExecutorPassword = resolveRemoteExecutorPassword(settings, failback);
        if (isBlank(remoteExecutorPassword)) {
            return rendered;
        }
        return wrapRemoteCommandWithPassword(rendered, remoteExecutorPassword);
    }

    private String wrapRemoteCommandWithPassword(String renderedCommand, String remoteExecutorPassword) {
        if (isWindows()) {
            return buildWindowsPasswordWrappedRemoteCommand(renderedCommand, remoteExecutorPassword);
        }
        return "sshpass -p " + quoteShellValue(remoteExecutorPassword) + " " + renderedCommand;
    }

    private String buildWindowsPasswordWrappedRemoteCommand(String renderedCommand, String remoteExecutorPassword) {
        String normalizedCommand = normalizeWindowsRemoteCommand(renderedCommand);
        return "plink -batch -pw " + quoteWindowsValue(remoteExecutorPassword) + " " + normalizedCommand;
    }

    private String normalizeWindowsRemoteCommand(String renderedCommand) {
        if (isBlank(renderedCommand)) {
            return renderedCommand;
        }
        String trimmed = renderedCommand.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("ssh ")) {
            return "-ssh " + trimmed.substring(4).trim();
        }
        if (lower.startsWith("ssh.exe ")) {
            return "-ssh " + trimmed.substring(8).trim();
        }
        return trimmed;
    }

    private String buildShellExports(Map<String, String> env) {
        StringBuilder exports = new StringBuilder();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            if (exports.length() > 0) {
                exports.append(' ');
            }
            exports.append(entry.getKey()).append('=').append(quoteShellValue(entry.getValue()));
        }
        return exports.toString();
    }

    private String quoteShellValue(String value) {
        String safe = value == null ? "" : value;
        if (isWindows()) {
            return '"' + safe.replace("\"", "\\\"") + '"';
        }
        return '\'' + safe.replace("'", "'\"'\"'") + '\'';
    }

    private String quoteWindowsValue(String value) {
        String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\\\"") + '"';
    }

    private String resolveDataDirectory(DatabaseRecoverySettings settings, boolean failback) {
        String value = failback ? settings.getFailbackDataDirectory() : settings.getDataDirectory();
        if (isBlank(value) && failback) {
            return settings.getDataDirectory();
        }
        return value;
    }

    private String resolveConfigFile(DatabaseRecoverySettings settings, boolean failback) {
        String value = failback ? settings.getFailbackConfigFile() : settings.getConfigFile();
        if (isBlank(value) && failback) {
            return settings.getConfigFile();
        }
        return value;
    }

    private String resolveScriptPath(DatabaseRecoverySettings settings, boolean failback) {
        String value = failback ? settings.getFailbackScriptPath() : settings.getScriptPath();
        if (isBlank(value) && failback) {
            return settings.getScriptPath();
        }
        return value;
    }

    private String resolvePgCtlCommand(DatabaseRecoverySettings settings, boolean failback) {
        String value = failback ? settings.getFailbackPgCtlCommand() : settings.getPgCtlCommand();
        if (isBlank(value) && failback) {
            return settings.getPgCtlCommand();
        }
        return value;
    }

    private String resolvePsqlCommand(DatabaseRecoverySettings settings, boolean failback) {
        String value = failback ? settings.getFailbackPsqlCommand() : settings.getPsqlCommand();
        if (isBlank(value) && failback) {
            return settings.getPsqlCommand();
        }
        return value;
    }

    private String resolvePgRewindCommand(DatabaseRecoverySettings settings, boolean failback) {
        String value = failback ? settings.getFailbackPgRewindCommand() : settings.getPgRewindCommand();
        if (isBlank(value) && failback) {
            return settings.getPgRewindCommand();
        }
        return value;
    }

    private String resolveLocalHost(DatabaseRecoverySettings settings, boolean failback) {
        String value = failback ? settings.getFailbackLocalHost() : settings.getLocalHost();
        if (isBlank(value) && failback) {
            return settings.getLocalHost();
        }
        return value;
    }

    private int resolveLocalPort(DatabaseRecoverySettings settings,
                                 boolean failback,
                                 DatabaseDialect dialect,
                                 DataSourceWrapper localNode) {
        int configured = failback ? settings.getFailbackLocalPort() : settings.getLocalPort();
        return configured > 0 ? configured : dialect.getPortFromJdbcUrl(localNode.getUrl());
    }

    private String resolveLocalDatabase(DatabaseRecoverySettings settings, boolean failback) {
        String value = failback ? settings.getFailbackLocalDatabase() : settings.getLocalDatabase();
        if (isBlank(value) && failback) {
            return settings.getLocalDatabase();
        }
        return value;
    }

    private String resolvePrimarySlotName(DatabaseRecoverySettings settings, boolean failback) {
        String value = failback ? settings.getFailbackPrimarySlotName() : settings.getPrimarySlotName();
        if (isBlank(value) && failback) {
            return settings.getPrimarySlotName();
        }
        return value;
    }

    private String resolveExtraPrimaryConnInfo(DatabaseRecoverySettings settings, boolean failback) {
        String value = failback ? settings.getFailbackExtraPrimaryConnInfo() : settings.getExtraPrimaryConnInfo();
        if (isBlank(value) && failback) {
            return settings.getExtraPrimaryConnInfo();
        }
        return value;
    }

    private int resolveStartupWaitSeconds(DatabaseRecoverySettings settings, boolean failback) {
        int value = failback ? settings.getFailbackStartupWaitSeconds() : settings.getStartupWaitSeconds();
        if (value <= 0 && failback) {
            return settings.getStartupWaitSeconds();
        }
        return value > 0 ? value : 60;
    }

    private int resolveCommandTimeoutSeconds(DatabaseRecoverySettings settings, boolean failback) {
        int value = failback ? settings.getFailbackCommandTimeoutSeconds() : settings.getCommandTimeoutSeconds();
        if (value <= 0 && failback) {
            return settings.getCommandTimeoutSeconds();
        }
        return value > 0 ? value : 900;
    }

    private boolean isRemoteExecutionEnabled(DatabaseRecoverySettings settings, boolean failback) {
        return failback ? settings.isFailbackRemoteExecutionEnabled() : settings.isRemoteExecutionEnabled();
    }

    private String resolveRemoteExecutorCommand(DatabaseRecoverySettings settings, boolean failback) {
        String value = failback ? settings.getFailbackRemoteExecutorCommand() : settings.getRemoteExecutorCommand();
        if (isBlank(value) && failback) {
            return settings.getRemoteExecutorCommand();
        }
        return value;
    }

    private String resolveRemoteExecutorPassword(DatabaseRecoverySettings settings, boolean failback) {
        String value = failback ? settings.getFailbackRemoteExecutorPassword() : settings.getRemoteExecutorPassword();
        if (isBlank(value) && failback) {
            return settings.getRemoteExecutorPassword();
        }
        return value;
    }

    private String resolveUsername(DataSourceWrapper wrapper) {
        if (wrapper == null) {
            return null;
        }
        if (!isBlank(wrapper.getUsername())) {
            return wrapper.getUsername();
        }
        try (Connection connection = wrapper.getDataSource().getConnection()) {
            return connection.getMetaData().getUserName();
        } catch (Exception ex) {
            log.debug("读取节点用户名失败: nodeId={}, reason={}", wrapper.getId(), ex.getMessage());
            return null;
        }
    }

    private String resolvePassword(DataSourceWrapper wrapper) {
        return wrapper == null ? null : wrapper.getPassword();
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);
        return os.contains("win");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void closeQuietly(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }
        try {
            inputStream.close();
        } catch (IOException ignore) {
        }
    }

    private void closeQuietly(FileOutputStream outputStream) {
        if (outputStream == null) {
            return;
        }
        try {
            outputStream.close();
        } catch (IOException ignore) {
        }
    }

    private static class StreamCollector extends Thread {

        private final InputStream inputStream;
        private final String actionName;
        private final String nodeId;
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        private StreamCollector(InputStream inputStream, String actionName, String nodeId) {
            this.inputStream = inputStream;
            this.actionName = actionName;
            this.nodeId = nodeId;
            setDaemon(true);
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String normalizedLine = line.replace('\r', ' ').trim();
                    if (!normalizedLine.isEmpty()) {
                        log.info("{}子进程输出: nodeId={}, line={}", actionName, nodeId, normalizedLine);
                    }
                    outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                    outputStream.write('\n');
                }
            } catch (IOException ignore) {
            }
        }

        private String output() {
            String text = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            return text.replace('\r', ' ').replace('\n', ' ').trim();
        }
    }
}