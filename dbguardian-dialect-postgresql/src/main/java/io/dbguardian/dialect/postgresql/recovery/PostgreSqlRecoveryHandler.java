package io.dbguardian.dialect.postgresql.recovery;

import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.core.recovery.DatabaseRecoveryHandler;
import io.dbguardian.core.recovery.DatabaseRecoverySettings;
import io.dbguardian.core.recovery.DialectRecoveryContext;
import io.dbguardian.dialect.postgresql.PostgreSqlDatabaseDialect;
import io.dbguardian.spi.DatabaseDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

public class PostgreSqlRecoveryHandler implements DatabaseRecoveryHandler {

    private static final Logger log = LoggerFactory.getLogger(PostgreSqlRecoveryHandler.class);

    private final PostgreSqlRewindExecutor rewindExecutor = new PostgreSqlRewindExecutor();

    @Override
    public boolean supports(DatabaseDialect dialect, DatabaseRecoverySettings settings) {
        return dialect != null
                && "postgresql".equals(dialect.getDatabaseType() == null ? null : dialect.getDatabaseType().toLowerCase(Locale.ENGLISH))
                && settings != null
                && settings.isEnabled()
                && settings.getDataDirectory() != null
                && !settings.getDataDirectory().trim().isEmpty();
    }

    @Override
    public boolean recoverMasterAsSlave(DialectRecoveryContext context) {
        log.info("PostgreSQL 恢复处理器开始执行原主追赶: localNode={}, targetPrimary={}, keepPromotedPrimary={}",
                context.getLocalNode() == null ? null : context.getLocalNode().getId(),
                context.getTargetPrimary() == null ? null : context.getTargetPrimary().getId(),
                context.getSettings() == null ? null : Boolean.valueOf(context.getSettings().isKeepPromotedPrimary()));
        boolean recovered = rewindExecutor.execute(
                context.getSettings(),
                context.getLocalNode(),
                context.getTargetPrimary(),
                context.getDialect(),
                context.getReplicationUser(),
                context.getReplicationPassword()
        );
        log.info("PostgreSQL 恢复处理器完成原主追赶: localNode={}, targetPrimary={}, recovered={}",
                context.getLocalNode() == null ? null : context.getLocalNode().getId(),
                context.getTargetPrimary() == null ? null : context.getTargetPrimary().getId(),
                recovered);
        return recovered;
    }

    @Override
    public boolean restoreOriginalMasterRole(DialectRecoveryContext context) {
        DataSourceWrapper localNode = context.getLocalNode();
        if (localNode == null) {
            return false;
        }
        if (isPrimary(localNode)) {
            log.info("PostgreSQL 原主已处于主库角色，跳过重复提升: nodeId={}", localNode.getId());
            return true;
        }
        try (Connection connection = localNode.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(PostgreSqlDatabaseDialect.PROMOTE_SQL);
        } catch (Exception ex) {
            log.warn("恢复 PostgreSQL 原主主角色失败: nodeId={}, reason={}", localNode.getId(), ex.getMessage());
            return false;
        }
        return waitForPrimaryWritable(localNode, context.getCatchupTimeoutSeconds());
    }

    private boolean isPrimary(DataSourceWrapper wrapper) {
        try (Connection connection = wrapper.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(PostgreSqlDatabaseDialect.IS_IN_RECOVERY_SQL)) {
            return rs.next() && !rs.getBoolean(1);
        } catch (Exception ex) {
            log.debug("读取 PostgreSQL 节点角色失败: nodeId={}, reason={}", wrapper.getId(), ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean recoverReplicaFromPrimary(DialectRecoveryContext context) {
        return rewindExecutor.recoverReplicaFromPrimary(
                context.getSettings(),
                context.getLocalNode(),
                context.getTargetPrimary(),
                context.getDialect(),
                context.getReplicationUser(),
                context.getReplicationPassword()
        );
    }

    @Override
    public boolean restoreOriginalReplication(DialectRecoveryContext context) {
        return rewindExecutor.executeFailback(
                context.getSettings(),
                context.getLocalNode(),
                context.getTargetPrimary(),
                context.getDialect(),
                context.getReplicationUser(),
                context.getReplicationPassword()
        );
    }

    private boolean waitForPrimaryWritable(DataSourceWrapper wrapper, int catchupTimeoutSeconds) {
        int waited = 0;
        while (waited < catchupTimeoutSeconds) {
            try (Connection connection = wrapper.getDataSource().getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(PostgreSqlDatabaseDialect.IS_IN_RECOVERY_SQL)) {
                if (rs.next() && !rs.getBoolean(1)) {
                    return true;
                }
            } catch (Exception ex) {
                log.debug("等待节点切回主库中: nodeId={}, reason={}", wrapper.getId(), ex.getMessage());
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
            waited += 1;
        }
        log.warn("等待节点切回主库超时: nodeId={}", wrapper.getId());
        return false;
    }
}