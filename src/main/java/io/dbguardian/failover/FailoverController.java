package io.dbguardian.failover;

import io.dbguardian.coordination.DatasourceCoordinationService;
import io.dbguardian.enums.DataSourceStatus;
import io.dbguardian.loadbalance.DataSourceWrapper;
import io.dbguardian.registry.DataSourceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 故障转移控制器
 * 负责检测故障并执行故障转移逻辑
 */
@Slf4j
@Component
public class FailoverController {

    @Autowired
    private DataSourceRegistry registry;

    @Autowired(required = false)
    private DatasourceCoordinationService coordinationService;

    // 故障转移锁，防止多实例同时执行
    private final AtomicBoolean failoverInProgress = new AtomicBoolean(false);

    // 当前主库ID
    private volatile String currentMasterId;

    // 配置参数
    private boolean demoteOnFailover = true;
    private String replicationUser;
    private String replicationPassword;

    // 故障转移监听器
    private final List<FailoverListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * 处理主库故障
     */
    public synchronized void handleMasterFailure(String failedMasterId) {
        if (!failoverInProgress.compareAndSet(false, true)) {
            log.info("故障转移已在进行中，跳过");
            return;
        }

        try {
            log.warn("检测到主库 {} 故障，开始故障转移流程", failedMasterId);

            // 1. 检查分布式锁
            if (coordinationService != null && !coordinationService.tryAcquireFailoverLock()) {
                log.info("其他实例正在处理故障转移，本实例跳过");
                return;
            }

            try {
                // 2. 查找备用主库
                List<DataSourceWrapper> availableMasters = registry.getAvailableMasters();
                availableMasters = availableMasters.stream()
                        .filter(w -> !w.getId().equals(failedMasterId))
                        .collect(java.util.stream.Collectors.toList());

                if (availableMasters.isEmpty()) {
                    log.error("没有可用备用主库，故障转移失败");
                    notifyFailoverFailed(failedMasterId, "无可用备用主库");
                    return;
                }

                // 3. 选择优先级最高的主库
                DataSourceWrapper newMaster = availableMasters.stream()
                        .max(Comparator.comparingInt(DataSourceWrapper::getPriority))
                        .orElse(availableMasters.get(0));

                // 4. 执行故障转移
                executeFailover(failedMasterId, newMaster);

            } finally {
                if (coordinationService != null) {
                    coordinationService.releaseFailoverLock();
                }
            }

        } finally {
            failoverInProgress.set(false);
        }
    }

    /**
     * 执行故障转移
     */
    private void executeFailover(String failedMasterId, DataSourceWrapper newMaster) {
        log.info("开始执行故障转移: {} -> {}", failedMasterId, newMaster.getId());

        String oldMasterId = currentMasterId;

        try {
            // 1. 通知所有从库切换主库
            notifySlavesNewMaster(newMaster);

            // 2. 升级新主库
            promoteToMaster(newMaster);

            // 3. 更新当前主库引用
            currentMasterId = newMaster.getId();

            // 4. 降级原主库（如果配置允许）
            if (demoteOnFailover) {
                demoteToSlave(failedMasterId);
            }

            // 5. 广播状态变更
            if (coordinationService != null) {
                coordinationService.broadcastFailoverEvent(oldMasterId, newMaster.getId());
            }

            // 6. 通知监听器
            notifyFailoverCompleted(oldMasterId, newMaster.getId());

            log.info("故障转移完成: {} -> {}", oldMasterId, newMaster.getId());

        } catch (Exception e) {
            log.error("故障转移执行失败: {}", e.getMessage(), e);
            notifyFailoverFailed(failedMasterId, e.getMessage());
        }
    }

    /**
     * 通知从库切换主库
     */
    private void notifySlavesNewMaster(DataSourceWrapper newMaster) {
        List<DataSourceWrapper> slaves = registry.getAllSlaves();
        int success = 0;
        int failed = 0;

        for (DataSourceWrapper slave : slaves) {
            try {
                reconfigureSlave(slave, newMaster);
                success++;
            } catch (Exception e) {
                log.error("重新配置从库 {} 失败: {}", slave.getId(), e.getMessage());
                failed++;
            }
        }

        log.info("从库重新配置完成: 成功={}, 失败={}", success, failed);
    }

    /**
     * 重新配置从库连接到新主库
     */
    private void reconfigureSlave(DataSourceWrapper slave, DataSourceWrapper newMaster) throws SQLException {
        try (Connection conn = slave.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {

            // 停止现有复制
            try {
                stmt.execute("STOP SLAVE");
                stmt.execute("RESET SLAVE ALL");
            } catch (SQLException ignored) {
                // 可能不是从库
            }

            // 获取新主库的连接信息
            String masterHost = extractHost(newMaster.getUrl());
            int masterPort = extractPort(newMaster.getUrl());

            // 重新配置复制
            if (replicationUser != null && !replicationUser.isEmpty()) {
                String sql = String.format(
                        "CHANGE MASTER TO MASTER_HOST='%s', MASTER_PORT=%d, MASTER_USER='%s', MASTER_PASSWORD='%s', MASTER_AUTO_POSITION=1",
                        masterHost, masterPort, replicationUser, replicationPassword);
                stmt.execute(sql);
                stmt.execute("START SLAVE");
                log.info("从库 {} 已重新配置连接到新主库 {}", slave.getId(), newMaster.getId());
            }

            // 关闭只读模式
            try {
                stmt.execute("SET GLOBAL READ_ONLY=OFF");
                stmt.execute("SET GLOBAL SUPER_READ_ONLY=OFF");
            } catch (SQLException ignored) {
                // 可能已经是读写模式
            }
        }
    }

    /**
     * 升主
     */
    private void promoteToMaster(DataSourceWrapper newMaster) throws SQLException {
        try (Connection conn = newMaster.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {

            // 停止复制
            try {
                stmt.execute("STOP SLAVE");
                stmt.execute("RESET SLAVE ALL");
            } catch (SQLException ignored) {
                // 可能不是从库
            }

            // 关闭只读模式
            stmt.execute("SET GLOBAL READ_ONLY=OFF");
            stmt.execute("SET GLOBAL SUPER_READ_ONLY=OFF");

            log.info("节点 {} 已升级为主库", newMaster.getId());
        }
    }

    /**
     * 降级为从库
     */
    private void demoteToSlave(String demotedMasterId) {
        DataSourceWrapper demoted = registry.getMaster(demotedMasterId);
        if (demoted == null) {
            log.warn("找不到要降级的主库: {}", demotedMasterId);
            return;
        }

        try (Connection conn = demoted.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {

            // 设置为只读
            stmt.execute("SET GLOBAL READ_ONLY=ON");

            log.info("节点 {} 已降级为从库", demotedMasterId);

        } catch (SQLException e) {
            log.error("降级节点 {} 失败: {}", demotedMasterId, e.getMessage());
        }
    }

    /**
     * 处理从库故障
     */
    public void handleSlaveFailure(String failedSlaveId) {
        log.warn("检测到从库 {} 故障", failedSlaveId);
        registry.updateSlaveAvailability(failedSlaveId, false);

        if (coordinationService != null) {
            coordinationService.broadcastSlaveStatus(failedSlaveId, false);
        }

        // 从库故障不需要执行复杂的故障转移，只需要从可用列表移除
        notifySlaveDown(failedSlaveId);
    }

    /**
     * 处理从库恢复
     */
    public void handleSlaveRecovery(String recoveredSlaveId) {
        log.info("检测到从库 {} 恢复", recoveredSlaveId);
        registry.updateSlaveAvailability(recoveredSlaveId, true);

        if (coordinationService != null) {
            coordinationService.broadcastSlaveStatus(recoveredSlaveId, true);
        }

        notifySlaveUp(recoveredSlaveId);
    }

    /**
     * 提取主机名
     */
    private String extractHost(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        int start = jdbcUrl.indexOf("://") + 3;
        int end = jdbcUrl.indexOf(":", start);
        if (end == -1) end = jdbcUrl.indexOf("/", start);
        return jdbcUrl.substring(start, end);
    }

    /**
     * 提取端口
     */
    private int extractPort(String jdbcUrl) {
        if (jdbcUrl == null) return 3306;
        int start = jdbcUrl.indexOf("://") + 3;
        start = jdbcUrl.indexOf(":", start) + 1;
        int end = jdbcUrl.indexOf("/", start);
        return Integer.parseInt(jdbcUrl.substring(start, end));
    }

    /**
     * 添加故障转移监听器
     */
    public void addListener(FailoverListener listener) {
        listeners.add(listener);
    }

    /**
     * 移除故障转移监听器
     */
    public void removeListener(FailoverListener listener) {
        listeners.remove(listener);
    }

    private void notifyFailoverCompleted(String oldMasterId, String newMasterId) {
        for (FailoverListener listener : listeners) {
            try {
                listener.onFailoverCompleted(oldMasterId, newMasterId);
            } catch (Exception e) {
                log.error("通知监听器失败: {}", e.getMessage());
            }
        }
    }

    private void notifyFailoverFailed(String failedMasterId, String reason) {
        for (FailoverListener listener : listeners) {
            try {
                listener.onFailoverFailed(failedMasterId, reason);
            } catch (Exception e) {
                log.error("通知监听器失败: {}", e.getMessage());
            }
        }
    }

    private void notifySlaveDown(String slaveId) {
        for (FailoverListener listener : listeners) {
            try {
                listener.onSlaveDown(slaveId);
            } catch (Exception e) {
                log.error("通知监听器失败: {}", e.getMessage());
            }
        }
    }

    private void notifySlaveUp(String slaveId) {
        for (FailoverListener listener : listeners) {
            try {
                listener.onSlaveUp(slaveId);
            } catch (Exception e) {
                log.error("通知监听器失败: {}", e.getMessage());
            }
        }
    }

    // Setters
    public void setDemoteOnFailover(boolean demoteOnFailover) {
        this.demoteOnFailover = demoteOnFailover;
    }

    public void setReplicationUser(String replicationUser) {
        this.replicationUser = replicationUser;
    }

    public void setReplicationPassword(String replicationPassword) {
        this.replicationPassword = replicationPassword;
    }

    public void setCurrentMasterId(String currentMasterId) {
        this.currentMasterId = currentMasterId;
    }

    public String getCurrentMasterId() {
        return currentMasterId;
    }

    public boolean isFailoverInProgress() {
        return failoverInProgress.get();
    }

    /**
     * 故障转移监听器接口
     */
    public interface FailoverListener {
        void onFailoverCompleted(String oldMasterId, String newMasterId);
        void onFailoverFailed(String failedMasterId, String reason);
        void onSlaveDown(String slaveId);
        void onSlaveUp(String slaveId);
    }
}
