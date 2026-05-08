package io.dbguardian.config;

import com.zaxxer.hikari.HikariDataSource;
import io.dbguardian.coordination.DatasourceCoordinationService;
import io.dbguardian.enums.DataSourceStatus;
import io.dbguardian.enums.DataSourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 读写分离数据源配置
 * 支持主从故障转移（从库升主库）
 * 
 * 基于 business-workflow-erp-java 项目的 DataSourceConfig 实现
 */
@Configuration
@EnableScheduling
@Slf4j
public class DbGuardianDataSourceConfig {

    @Value("${spring.datasource.master.url}")
    private String masterUrl;

    @Value("${spring.datasource.master.username}")
    private String masterUsername;

    @Value("${spring.datasource.master.password}")
    private String masterPassword;

    @Value("${spring.datasource.master.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String driverClassName;

    @Value("${spring.datasource.master.hikari.maximum-pool-size:20}")
    private int masterPoolSize;

    @Value("${spring.datasource.master.hikari.minimum-idle:5}")
    private int masterMinIdle;

    @Value("${spring.datasource.slave.url}")
    private String slaveUrl;

    @Value("${spring.datasource.slave.username}")
    private String slaveUsername;

    @Value("${spring.datasource.slave.password}")
    private String slavePassword;

    @Value("${spring.datasource.slave.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String slaveDriverClassName;

    @Value("${spring.datasource.slave.hikari.maximum-pool-size:20}")
    private int slavePoolSize;

    @Value("${spring.datasource.slave.hikari.minimum-idle:5}")
    private int slaveMinIdle;

    /**
     * 主从复制配置
     * 注意：此配置在 failover 后需要动态更新为当前新主库的地址
     */
    @Value("${spring.datasource.replication.master-host:106.55.104.229}")
    private String replicationMasterHost;

    @Value("${spring.datasource.replication.master-port:3306}")
    private int replicationMasterPort;

    @Value("${spring.datasource.replication.master-user:repl}")
    private String replicationMasterUser;

    @Value("${spring.datasource.replication.master-password:}")
    private String replicationMasterPassword;

    @Value("${spring.datasource.replication.auto-reconnect:true}")
    private boolean autoReconnect;

    /**
     * 是否允许在数据库不可用时降级启动
     * true: 至少有一个数据库可用即可启动
     * false: 必须至少有一个数据库可用
     */
    @Value("${spring.datasource.allow-degraded-startup:false}")
    private boolean allowDegradedStartup;

    /** 当前新主库地址（failover 后更新） */
    private volatile String currentNewMasterHost;

    /** 是否已完成启动初始化（防止启动时误触发恢复逻辑） */
    private volatile boolean initializationComplete = false;

    /** 是否正在执行恢复原主库（防止重复执行） */
    private final AtomicBoolean recoveryInProgress = new AtomicBoolean(false);

    /** 当前活跃的数据源状态 */
    private final AtomicReference<DataSourceStatus> currentStatus = new AtomicReference<>(DataSourceStatus.MASTER_ACTIVE);
    private final AtomicBoolean masterFailed = new AtomicBoolean(false);
    private final AtomicBoolean slaveFailed = new AtomicBoolean(false);

    private HikariDataSource masterDataSourceBean;
    private HikariDataSource slaveDataSourceBean;

    /** 异步任务执行器 */
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /** Redis 分布式协调服务 */
    @Autowired(required = false)
    private DatasourceCoordinationService coordinationService;

    /**
     * 主数据源（写操作使用）
     */
    @Bean("dbguardianMasterDataSource")
    public DataSource masterDataSource() {
        masterDataSourceBean = createDataSource(masterUrl, masterUsername, masterPassword, driverClassName, masterPoolSize, masterMinIdle, "dbguardian-master-pool", !allowDegradedStartup);
        return masterDataSourceBean;
    }

    /**
     * 从数据源（读操作使用）
     */
    @Bean("dbguardianSlaveDataSource")
    public DataSource slaveDataSource() {
        slaveDataSourceBean = createDataSource(slaveUrl, slaveUsername, slavePassword, slaveDriverClassName, slavePoolSize, slaveMinIdle, "dbguardian-slave-pool", !allowDegradedStartup);
        return slaveDataSourceBean;
    }

    private HikariDataSource createDataSource(String url, String username, String password, String driverClassName, int poolSize, int minIdle, String poolName, boolean testConnection) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setMaximumPoolSize(poolSize);
        dataSource.setMinimumIdle(minIdle);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setPoolName(poolName);
        // 降级模式下跳过连接测试，避免启动时卡住
        if (!testConnection) {
            dataSource.setConnectionTestQuery(null);
        } else {
            dataSource.setConnectionTestQuery("SELECT 1");
            dataSource.setValidationTimeout(5000);
        }
        return dataSource;
    }

    /**
     * 动态数据源（根据上下文切换主从）
     */
    @Bean("dbguardianRoutingDataSource")
    @Primary
    public DataSource routingDataSource(
            @Qualifier("dbguardianMasterDataSource") DataSource masterDataSource,
            @Qualifier("dbguardianSlaveDataSource") DataSource slaveDataSource) {
        Map<Object, Object> targetDataSources = new HashMap<>(2);
        targetDataSources.put(DataSourceType.MASTER, masterDataSource);
        targetDataSources.put(DataSourceType.SLAVE, slaveDataSource);

        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);

        return routingDataSource;
    }

    /**
     * 启动时检查数据源状态并自动配置主从复制
     */
    @PostConstruct
    public void init() {
        log.info("=== DBGuardian 读写分离配置初始化 ===");

        // 快速检查，仅用于决定是否允许降级启动
        boolean masterOk = checkMasterHealth();
        boolean slaveOk = checkSlaveHealth();

        if (masterOk || slaveOk) {
            log.info("数据源连接正常，跳过降级模式");
            currentStatus.set(DataSourceStatus.MASTER_ACTIVE);

            // 启动时检查并关闭主库的只读模式
            if (masterOk) {
                disableReadOnlyOnDataSource(masterDataSourceBean);
            }
        } else if (allowDegradedStartup) {
            log.warn("主库和从库都不可用，但允许降级启动（仅使用主库配置）");
            currentStatus.set(DataSourceStatus.DEGRADED);
        } else {
            throw new RuntimeException("主库和从库都不可用，无法启动");
        }

        log.info("=== 初始数据源状态: {}, 主库: {}, 从库: {} ===",
                 currentStatus.get(), masterOk ? "可用" : "不可用", slaveOk ? "可用" : "不可用");

        // 标记初始化完成，之后定时任务才生效
        initializationComplete = true;

        // 初始化 Redis 协调服务
        initializeRedisCoordination();
    }

    /**
     * 应用启动完成后，重新检测数据源状态并恢复正常模式
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=== 应用启动完成，重新检测数据源状态 ===");

        boolean masterOk = checkMasterHealth();
        boolean slaveOk = checkSlaveHealth();

        if (masterOk && slaveOk) {
            currentStatus.set(DataSourceStatus.MASTER_ACTIVE);
            determineAndConfigureMasterSlave();
            log.info("数据源状态已恢复正常: MASTER_ACTIVE（两个数据源都可用）");
        } else if (masterOk) {
            currentStatus.set(DataSourceStatus.MASTER_ACTIVE);
            log.info("数据源状态已恢复正常: MASTER_ACTIVE（只有主库可用）");
        } else if (slaveOk) {
            currentStatus.set(DataSourceStatus.SLAVE_PROMOTED);
            currentNewMasterHost = slaveUrl;
            log.warn("数据源状态已恢复: SLAVE_PROMOTED（从库升为主库）");
        } else {
            log.warn("数据源仍然不可用，保持 DEGRADED 模式");
        }

        log.info("=== 最终数据源状态: {}, 主库: {}, 从库: {} ===",
                 currentStatus.get(), masterOk ? "可用" : "不可用", slaveOk ? "可用" : "不可用");

        // 同步最终状态到 Redis
        syncStatusToRedis();
    }

    /**
     * 初始化 Redis 分布式协调
     */
    private void initializeRedisCoordination() {
        if (coordinationService != null && coordinationService.isHealthy()) {
            coordinationService.initialize();
            // 同步状态到 Redis
            syncStatusToRedis();
            log.info("Redis 分布式协调服务已启用");
        } else {
            log.warn("Redis 协调服务不可用，将使用单机模式（无多后端同步）");
        }
    }

    /**
     * 同步当前状态到 Redis
     */
    private void syncStatusToRedis() {
        if (coordinationService == null || !coordinationService.isHealthy()) {
            return;
        }
        try {
            coordinationService.registerAsMaster(coordinationService.getInstanceId());
            String status;
            if (currentStatus.get() == DataSourceStatus.DEGRADED) {
                status = "DEGRADED";
            } else if (currentStatus.get() == DataSourceStatus.SLAVE_PROMOTED) {
                status = DatasourceCoordinationService.STATUS_SLAVE_PROMOTED;
            } else {
                status = DatasourceCoordinationService.STATUS_NORMAL;
            }
            coordinationService.setMasterStatus(status);
            log.info("状态已同步到 Redis: {}", status);
        } catch (Exception e) {
            log.error("同步状态到 Redis 失败: {}", e.getMessage());
        }
    }

    /**
     * 从 Redis 同步到正常模式（故障恢复）
     */
    public void syncToNormalMode() {
        if (currentStatus.get() != DataSourceStatus.SLAVE_PROMOTED) {
            return;
        }
        log.info("收到 Redis 广播：切换到正常模式");
        currentStatus.set(DataSourceStatus.MASTER_ACTIVE);
        DataSourceContextHolder.useMaster();
        log.info("已切换到正常模式");
    }

    /**
     * 从 Redis 同步到从库升主库模式（故障转移）
     */
    public void syncToSlavePromotedMode() {
        if (currentStatus.get() == DataSourceStatus.SLAVE_PROMOTED) {
            return;
        }
        log.info("收到 Redis 广播：从库升主库模式");
        currentStatus.set(DataSourceStatus.SLAVE_PROMOTED);
        DataSourceContextHolder.useMaster();
        log.info("已切换到从库升主库模式");
    }

    /**
     * 自动判断哪个数据源更新，并配置主从复制
     */
    private void determineAndConfigureMasterSlave() {
        try {
            // 获取两个数据源的 GTID 或 binlog 位置
            String masterGtid = getGtidPosition(masterDataSourceBean);
            String slaveGtid = getGtidPosition(slaveDataSourceBean);

            log.info("主库 GTID: {}", masterGtid);
            log.info("从库 GTID: {}", slaveGtid);

            // 比较 GTID 集合，确定哪个更新
            boolean masterNewer = isNewerThan(masterGtid, slaveGtid);

            if (masterNewer) {
                // 主库更新，以主库为主，配置从库复制主库
                log.info("主库数据更新，以主库为主库");

                // 确保主库可写（关闭只读）
                disableReadOnlyOnDataSource(masterDataSourceBean);

                // 配置从库复制主库：masterHost 应该是主库的 IP
                configureSlaveReplication(masterDataSourceBean, slaveDataSourceBean,
                    extractHost(masterUrl), extractPort(masterUrl));
            } else {
                // 从库更新，以从库为主，配置主库复制从库
                // 此时 newMasterHost 应该是从库的地址
                String newMasterHost = extractHost(slaveUrl);
                int newMasterPort = extractPort(slaveUrl);
                configureSlaveReplication(slaveDataSourceBean, masterDataSourceBean,
                    newMasterHost, newMasterPort);

                // 交换数据源引用
                HikariDataSource temp = masterDataSourceBean;
                masterDataSourceBean = slaveDataSourceBean;
                slaveDataSourceBean = temp;
                currentNewMasterHost = slaveUrl;

                currentStatus.set(DataSourceStatus.SLAVE_PROMOTED);
            }

        } catch (Exception e) {
            log.error("自动判断主从失败，使用默认配置: {}", e.getMessage());
            // 默认以主库为主
            currentStatus.set(DataSourceStatus.MASTER_ACTIVE);
        }
    }

    /**
     * 关闭数据源的只读模式
     */
    private void disableReadOnlyOnDataSource(DataSource ds) {
        if (ds == null) return;
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET GLOBAL READ_ONLY=OFF");
            stmt.execute("SET GLOBAL SUPER_READ_ONLY=OFF");
            log.info("已关闭数据源只读模式");
        } catch (SQLException e) {
            log.debug("关闭只读模式失败（可能无权限或已关闭）: {}", e.getMessage());
        }
    }

    /**
     * 获取 GTID 位置
     */
    private String getGtidPosition(DataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW MASTER STATUS")) {
            if (rs.next()) {
                return rs.getString("Executed_Gtid_Set");
            }
        } catch (SQLException e) {
            log.debug("获取 GTID 失败: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 比较两个 GTID 集合，判断第一个是否更新
     */
    private boolean isNewerThan(String gtid1, String gtid2) {
        if (gtid1 == null || gtid1.isEmpty()) return false;
        if (gtid2 == null || gtid2.isEmpty()) return true;

        try {
            // 简单比较：分割 GTID 并统计已执行的事务数量
            Set<String> set1 = new java.util.HashSet<>(Arrays.asList(gtid1.split(",")));
            Set<String> set2 = new java.util.HashSet<>(Arrays.asList(gtid2.split(",")));

            // 合并并统计
            Set<String> all = new java.util.HashSet<>(set1);
            all.addAll(set2);

            int count1 = 0, count2 = 0;
            for (String gtid : all) {
                if (set1.contains(gtid)) count1++;
                if (set2.contains(gtid)) count2++;
            }

            return count1 >= count2;
        } catch (Exception e) {
            log.debug("GTID 比较失败，使用默认逻辑: {}", e.getMessage());
            return true; // 默认主库为主
        }
    }

    /**
     * 配置从库复制主库
     */
    private void configureSlaveReplication(DataSource masterDs, DataSource slaveDs,
                                         String masterHost, int masterPort) {
        try (Connection slaveConn = slaveDs.getConnection();
             Statement stmt = slaveConn.createStatement()) {

            // 0. 确保"新主库"可写（关闭只读）
            disableReadOnlyOnDataSource(masterDs);

            // 1. 停止旧复制
            try {
                stmt.execute("STOP SLAVE");
                stmt.execute("RESET SLAVE ALL");
            } catch (SQLException ignored) {}

            // 2. 配置复制（使用 GTID 自动定位）
            String sql = String.format(
                "CHANGE MASTER TO " +
                "MASTER_HOST='%s', " +
                "MASTER_PORT=%d, " +
                "MASTER_USER='%s', " +
                "MASTER_PASSWORD='%s', " +
                "MASTER_AUTO_POSITION=1",
                masterHost, masterPort, replicationMasterUser, replicationMasterPassword);
            stmt.execute(sql);

            // 3. 启动复制
            stmt.execute("START SLAVE");
            log.info("已配置从库复制主库: {}:{}", masterHost, masterPort);

            // 4. 检查复制状态（异步检查，不阻塞）
            executorService.submit(() -> {
                try {
                    Thread.sleep(2000);
                    try (Connection checkConn = slaveDs.getConnection();
                         Statement checkStmt = checkConn.createStatement();
                         ResultSet rs = checkStmt.executeQuery("SHOW SLAVE STATUS")) {
                        if (rs.next()) {
                            String ioRunning = rs.getString("Slave_IO_Running");
                            String sqlRunning = rs.getString("Slave_SQL_Running");
                            log.info("复制状态 - IO: {}, SQL: {}", ioRunning, sqlRunning);
                        }
                    }
                } catch (Exception e) {
                    log.debug("检查复制状态失败: {}", e.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("配置主从复制失败: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        if (masterDataSourceBean != null && !masterDataSourceBean.isClosed()) {
            masterDataSourceBean.close();
        }
        if (slaveDataSourceBean != null && !slaveDataSourceBean.isClosed()) {
            slaveDataSourceBean.close();
        }
    }

    /**
     * 每 30 秒检测数据源健康状态
     */
    @Scheduled(fixedRate = 30000)
    public void healthCheck() {
        // 等待初始化完成
        if (!initializationComplete) {
            return;
        }

        checkMasterHealth();
        checkSlaveHealth();
        handleFailover();
    }

    /**
     * 检查主库健康状态
     */
    private boolean checkMasterHealth() {
        if (masterDataSourceBean == null) {
            return false;
        }
        try (Connection conn = masterDataSourceBean.getConnection()) {
            if (conn.isValid(5)) {
                if (masterFailed.get()) {
                    log.info("主库恢复正常");
                    masterFailed.set(false);
                }
                return true;
            }
        } catch (SQLException e) {
            log.error("主库连接失败: {}", e.getMessage());
        }
        masterFailed.set(true);
        return false;
    }

    /**
     * 检查从库健康状态
     */
    private boolean checkSlaveHealth() {
        if (slaveDataSourceBean == null) {
            return false;
        }
        try (Connection conn = slaveDataSourceBean.getConnection()) {
            if (conn.isValid(5)) {
                if (slaveFailed.get()) {
                    log.info("从库恢复正常");
                    slaveFailed.set(false);
                }
                return true;
            }
        } catch (SQLException e) {
            log.error("从库连接失败: {}", e.getMessage());
        }
        slaveFailed.set(true);
        return false;
    }

    /**
     * 处理故障转移
     */
    private void handleFailover() {
        // 降级模式下不执行故障转移
        if (currentStatus.get() == DataSourceStatus.DEGRADED) {
            return;
        }

        DataSourceStatus oldStatus = currentStatus.get();

        if (oldStatus == DataSourceStatus.MASTER_ACTIVE) {
            // 主库正常，检查是否需要切换
            if (masterFailed.get() && !slaveFailed.get()) {
                // 尝试获取分布式锁，只有获取成功的实例才执行故障转移
                if (coordinationService != null && coordinationService.isHealthy()) {
                    if (coordinationService.tryAcquireFailoverLock(60)) {
                        try {
                            log.warn("主库故障，从库将升为主库（分布式协调）");
                            autoPromoteSlaveToMasterWithCoordination();
                        } finally {
                            coordinationService.releaseFailoverLock();
                        }
                    } else {
                        log.info("其他实例正在执行故障转移");
                    }
                } else {
                    // 无 Redis，降级为单机模式
                    log.warn("主库故障，Redis不可用，执行本地故障转移");
                    autoPromoteSlaveToMaster();
                }
            }
        } else if (oldStatus == DataSourceStatus.SLAVE_PROMOTED) {
            // 从库升主，检查原主库是否恢复，且没有正在进行的恢复
            if (!masterFailed.get() && !recoveryInProgress.get()) {
                log.info("原主库已恢复，开始恢复流程...");
                recoverOriginalMaster();
            }
        }
    }

    /**
     * 自动将从库升为主库（带分布式协调）
     */
    private void autoPromoteSlaveToMasterWithCoordination() {
        try {
            // 记录新主库地址
            currentNewMasterHost = slaveUrl;
            log.info("记录新主库地址: {}", currentNewMasterHost);

            // 关闭从库只读模式
            disableSlaveReadOnly();

            currentStatus.set(DataSourceStatus.SLAVE_PROMOTED);
            DataSourceContextHolder.useMaster();

            // 同步状态到 Redis
            if (coordinationService != null && coordinationService.isHealthy()) {
                coordinationService.setMasterStatus(DatasourceCoordinationService.STATUS_SLAVE_PROMOTED);
            }

            log.info("分布式故障转移完成：从库已升为主库，状态已同步到 Redis");
        } catch (Exception e) {
            log.error("分布式故障转移失败: {}", e.getMessage());
        }
    }

    /**
     * 自动将从库升为主库
     */
    private void autoPromoteSlaveToMaster() {
        try {
            // 记录新主库地址（原从库的地址，用于原主库恢复时配置复制）
            currentNewMasterHost = slaveUrl;
            log.info("记录新主库地址: {}", currentNewMasterHost);

            // 关闭从库只读模式
            disableSlaveReadOnly();

            currentStatus.set(DataSourceStatus.SLAVE_PROMOTED);
            DataSourceContextHolder.useMaster();
            log.info("自动故障转移完成：从库已升为主库");
        } catch (Exception e) {
            log.error("自动故障转移失败: {}", e.getMessage());
        }
    }

    /**
     * 手动切换到从库（从库升主库）
     */
    public void promoteSlaveToMaster() {
        DataSourceStatus oldStatus = currentStatus.get();
        if (oldStatus == DataSourceStatus.SLAVE_PROMOTED) {
            log.warn("从库已经是主库状态，无需切换");
            return;
        }

        if (slaveFailed.get()) {
            throw new RuntimeException("从库不可用，无法升为主库");
        }

        // 关闭从库只读模式
        disableSlaveReadOnly();

        currentStatus.set(DataSourceStatus.SLAVE_PROMOTED);
        DataSourceContextHolder.useMaster(); // 从库承担主库角色
        log.info("从库已升为主库");
    }

    /**
     * 关闭从库只读模式（执行 SQL）
     */
    private void disableSlaveReadOnly() {
        try (Connection conn = slaveDataSourceBean.getConnection();
             Statement stmt = conn.createStatement()) {
            // 停止主从复制
            try {
                stmt.execute("STOP SLAVE");
                stmt.execute("RESET SLAVE ALL");
                log.info("主从复制已停止并重置");
            } catch (SQLException e) {
                log.debug("停止主从复制失败（可能不是从库）: {}", e.getMessage());
            }

            // 关闭只读模式
            stmt.execute("SET GLOBAL READ_ONLY=OFF");
            stmt.execute("SET GLOBAL SUPER_READ_ONLY=OFF");
            log.info("从库只读模式已关闭");
        } catch (SQLException e) {
            log.warn("关闭从库只读模式失败: {}. 请手动执行: SET GLOBAL READ_ONLY=OFF", e.getMessage());
        }
    }

    /**
     * 恢复原主库：作为从库同步数据后重新变为主库
     */
    private void recoverOriginalMaster() {
        // 设置恢复标志，防止重复执行
        if (!recoveryInProgress.compareAndSet(false, true)) {
            log.info("恢复原主库正在执行中，跳过");
            return;
        }

        try {
            log.info("=== 开始恢复原主库 ===");

            // 此时 slaveDataSourceBean 指向的是原从库（新主库）
            // 需要在新主库上执行操作
            try (Connection newMasterConn = slaveDataSourceBean.getConnection();
                 Statement stmt = newMasterConn.createStatement()) {
                stmt.execute("FLUSH TABLES WITH READ LOCK");
                log.info("新主库已加读锁");
            }

            // 在原主库上配置主从复制
            recoverMasterAsSlave();

            // 解锁新主库
            try (Connection newMasterConn = slaveDataSourceBean.getConnection();
                 Statement stmt = newMasterConn.createStatement()) {
                stmt.execute("UNLOCK TABLES");
                log.info("新主库已解锁");
            }

            log.info("=== 原主库恢复完成，可手动切换回原主库 ===");
        } catch (Exception e) {
            log.error("原主库恢复失败: {}", e.getMessage());
        } finally {
            // 清除恢复标志（恢复完成或失败都清除）
            recoveryInProgress.set(false);
        }
    }

    /**
     * 将原主库配置为从库，连接新主库同步数据（追赶阶段）
     */
    private void recoverMasterAsSlave() {
        if (currentNewMasterHost == null) {
            log.error("新主库地址为空，无法恢复原主库");
            return;
        }

        try (Connection masterConn = masterDataSourceBean.getConnection();
             Statement stmt = masterConn.createStatement()) {

            // 1. 停止可能存在的旧复制
            try {
                stmt.execute("STOP SLAVE");
                stmt.execute("RESET SLAVE ALL");
            } catch (SQLException ignored) {
            }

            // 2. 从 slaveDataSourceBean 获取新主库的实际地址（分布式环境下更可靠）
            String newMasterHost;
            int newMasterPort;
            try (Connection newMasterConn = slaveDataSourceBean.getConnection();
                 Statement newMasterStmt = newMasterConn.createStatement();
                 ResultSet rs = newMasterStmt.executeQuery("SELECT @@hostname, @@port")) {
                if (rs.next()) {
                    newMasterHost = rs.getString(1);
                    newMasterPort = rs.getInt(2);
                    log.info("从新主库连接获取到实际地址: {}:{}", newMasterHost, newMasterPort);
                } else {
                    // 降级方案：从 currentNewMasterHost 解析
                    log.warn("无法从新主库连接获取地址，使用配置地址");
                    newMasterHost = extractHost(currentNewMasterHost);
                    newMasterPort = extractPort(currentNewMasterHost);
                }
            } catch (SQLException e) {
                log.warn("从新主库获取地址失败，使用配置地址: {}", e.getMessage());
                newMasterHost = extractHost(currentNewMasterHost);
                newMasterPort = extractPort(currentNewMasterHost);
            }
            log.info("新主库地址: {}:{}", newMasterHost, newMasterPort);

            // 3. 设置新的主库连接（指向当前的新主库）
            String changeMasterSql = String.format(
                "CHANGE MASTER TO " +
                "MASTER_HOST='%s', " +
                "MASTER_PORT=%d, " +
                "MASTER_USER='%s', " +
                "MASTER_PASSWORD='%s', " +
                "MASTER_AUTO_POSITION=1",
                newMasterHost,
                newMasterPort,
                replicationMasterUser,
                replicationMasterPassword
            );
            stmt.execute(changeMasterSql);
            log.info("已配置原主库连接到新主库: {}:{}", newMasterHost, newMasterPort);

            // 4. 启动复制（追赶数据）
            stmt.execute("START SLAVE");
            log.info("原主库已启动主从复制，开始追赶数据");

            // 5. 异步检查复制状态
            executorService.submit(() -> {
                try {
                    Thread.sleep(2000);
                    try (Connection checkConn = masterDataSourceBean.getConnection();
                         Statement checkStmt = checkConn.createStatement();
                         ResultSet rs = checkStmt.executeQuery("SHOW SLAVE STATUS")) {
                        if (rs.next()) {
                            String ioRunning = rs.getString("Slave_IO_Running");
                            String sqlRunning = rs.getString("Slave_SQL_Running");
                            String secondsBehind = rs.getString("Seconds_Behind_Master");
                            log.info("复制状态 - IO线程: {}, SQL线程: {}, 延迟: {}秒", ioRunning, sqlRunning, secondsBehind);
                        }
                    }
                } catch (Exception e) {
                    log.debug("检查复制状态失败: {}", e.getMessage());
                }
            });

        } catch (SQLException e) {
            log.error("配置原主库为主从复制失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 从 JDBC URL 提取主机地址
     */
    private String extractHost(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        // jdbc:mysql://106.55.104.229:3306/...
        int start = jdbcUrl.indexOf("://") + 3;
        int end = jdbcUrl.indexOf(":", start);
        if (end == -1) end = jdbcUrl.indexOf("/", start);
        return jdbcUrl.substring(start, end);
    }

    /**
     * 从 JDBC URL 提取端口
     */
    private int extractPort(String jdbcUrl) {
        if (jdbcUrl == null) return 3306;
        // jdbc:mysql://host:3306/...
        int start = jdbcUrl.indexOf("://") + 3;
        start = jdbcUrl.indexOf(":", start) + 1;
        int end = jdbcUrl.indexOf("/", start);
        String port = jdbcUrl.substring(start, end);
        return Integer.parseInt(port);
    }

    /**
     * 手动切回原主库（恢复原始主从架构）
     * 前提：原主库必须已完成追赶才能切换
     */
    public void switchToOriginalMaster() {
        if (masterFailed.get()) {
            throw new RuntimeException("原主库不可用，无法切换");
        }

        DataSourceStatus status = currentStatus.get();
        if (status != DataSourceStatus.SLAVE_PROMOTED) {
            log.warn("当前不是从库升主库模式，无需切换");
            return;
        }

        try {
            // 1. 预检查追赶状态（不等待，直接检查）
            RecoveryStatus recoveryStatus = getRecoveryStatus();
            if (!recoveryStatus.isReadyToSwitch()) {
                String errMsg = String.format(
                    "原主库追赶未完成，无法切换。IO线程: %s, SQL线程: %s, 延迟: %d秒, 已追上: %s",
                    recoveryStatus.isIoRunning() ? "运行中" : "未运行",
                    recoveryStatus.isSqlRunning() ? "运行中" : "未运行",
                    recoveryStatus.getSecondsBehindMaster(),
                    recoveryStatus.isCaughtUp() ? "是" : "否");
                log.error(errMsg);
                throw new RuntimeException(errMsg);
            }

            log.info("追赶检查通过：IO线程运行中，SQL线程运行中，延迟为0，已追上数据");

            // 2. 在新主库（原从库）上配置反向复制
            configureReverseReplication();

            // 3. 在原主库上停止复制，关闭只读
            try (Connection masterConn = masterDataSourceBean.getConnection();
                 Statement stmt = masterConn.createStatement()) {
                stmt.execute("STOP SLAVE");
                stmt.execute("RESET SLAVE ALL");
                stmt.execute("SET GLOBAL READ_ONLY=OFF");
                stmt.execute("SET GLOBAL SUPER_READ_ONLY=OFF");
                log.info("原主库已停止复制并关闭只读");
            }

            // 4. 交换数据源引用（恢复原始配置）
            HikariDataSource temp = masterDataSourceBean;
            masterDataSourceBean = slaveDataSourceBean;
            slaveDataSourceBean = temp;

            // 5. 切换应用数据源到原主库
            currentStatus.set(DataSourceStatus.MASTER_ACTIVE);
            currentNewMasterHost = null; // 恢复完成，清除记录
            log.info("已成功切换回原主库");

            // 6. 同步状态到 Redis
            if (coordinationService != null && coordinationService.isHealthy()) {
                coordinationService.setMasterStatus(DatasourceCoordinationService.STATUS_NORMAL);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("切换回原主库失败: {}", e.getMessage());
            throw new RuntimeException("切换失败: " + e.getMessage());
        }
    }

    /**
     * 手动切回原主库（恢复原始主从架构）- 等待模式
     * @param maxWaitSeconds 最大等待秒数
     */
    public void switchToOriginalMaster(long maxWaitSeconds) {
        if (masterFailed.get()) {
            throw new RuntimeException("原主库不可用，无法切换");
        }

        DataSourceStatus status = currentStatus.get();
        if (status != DataSourceStatus.SLAVE_PROMOTED) {
            log.warn("当前不是从库升主库模式，无需切换");
            return;
        }

        try {
            // 1. 等待追赶完成
            log.info("开始等待原主库追赶，最多等待 {} 秒...", maxWaitSeconds);
            boolean synced = waitForSyncWithTimeout(maxWaitSeconds * 1000);
            if (!synced) {
                throw new RuntimeException(String.format(
                    "等待追赶超时（%d秒），请稍后重试或检查复制状态", maxWaitSeconds));
            }

            // 2. 再次确认追赶状态
            RecoveryStatus recoveryStatus = getRecoveryStatus();
            if (!recoveryStatus.isReadyToSwitch()) {
                throw new RuntimeException("追赶状态异常，无法切换");
            }

            // 3. 执行切换
            configureReverseReplication();

            try (Connection masterConn = masterDataSourceBean.getConnection();
                 Statement stmt = masterConn.createStatement()) {
                stmt.execute("STOP SLAVE");
                stmt.execute("RESET SLAVE ALL");
                stmt.execute("SET GLOBAL READ_ONLY=OFF");
                stmt.execute("SET GLOBAL SUPER_READ_ONLY=OFF");
                log.info("原主库已停止复制并关闭只读");
            }

            // 4. 交换数据源引用（恢复原始配置）
            HikariDataSource temp = masterDataSourceBean;
            masterDataSourceBean = slaveDataSourceBean;
            slaveDataSourceBean = temp;

            // 5. 切换应用数据源
            currentStatus.set(DataSourceStatus.MASTER_ACTIVE);
            currentNewMasterHost = null;
            log.info("已成功切换回原主库");

            // 6. 同步状态到 Redis
            if (coordinationService != null && coordinationService.isHealthy()) {
                coordinationService.setMasterStatus(DatasourceCoordinationService.STATUS_NORMAL);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("切换回原主库失败: {}", e.getMessage());
            throw new RuntimeException("切换失败: " + e.getMessage());
        }
    }

    /**
     * 等待原主库同步完成（追赶阶段）
     * @param timeoutMillis 超时毫秒数
     * @return true 同步完成，false 超时
     */
    private boolean waitForSyncWithTimeout(long timeoutMillis) {
        int maxWait = (int) (timeoutMillis / 1000);
        int waitCount = 0;

        log.info("开始等待原主库同步，超时时间: {}秒", maxWait);

        while (waitCount < maxWait) {
            try {
                try (Connection masterConn = masterDataSourceBean.getConnection();
                     Statement stmt = masterConn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW SLAVE STATUS")) {

                    if (rs.next()) {
                        String ioRunning = rs.getString("Slave_IO_Running");
                        String sqlRunning = rs.getString("Slave_SQL_Running");
                        String secondsBehind = rs.getString("Seconds_Behind_Master");
                        String readMasterLogPos = rs.getString("Read_Master_Log_Pos");
                        String execMasterLogPos = rs.getString("Exec_Master_Log_Pos");
                        String lastError = rs.getString("Last_Error");

                        log.info("复制状态 - IO: {}, SQL: {}, 延迟: {}秒", ioRunning, sqlRunning, secondsBehind);

                        // 检查错误
                        if (lastError != null && !lastError.isEmpty()) {
                            log.error("复制错误: {}", lastError);
                            throw new RuntimeException("复制出错: " + lastError);
                        }

                        // 同步完成的判断条件
                        boolean ioOk = "Yes".equals(ioRunning);
                        boolean sqlOk = "Yes".equals(sqlRunning);
                        boolean caughtUp = (readMasterLogPos != null && execMasterLogPos != null
                                           && readMasterLogPos.equals(execMasterLogPos));
                        boolean noLag = (secondsBehind == null || "0".equals(secondsBehind) || "-1".equals(secondsBehind));

                        if (ioOk && sqlOk && caughtUp && noLag) {
                            log.info("原主库已完全同步完成");
                            return true;
                        }
                    }
                }

                Thread.sleep(2000);
                waitCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待同步被中断");
                return false;
            } catch (RuntimeException e) {
                throw e;
            } catch (SQLException e) {
                log.debug("检查同步状态失败: {}", e.getMessage());
            } catch (Exception e) {
                log.debug("等待同步失败: {}", e.getMessage());
            }
        }
        log.warn("等待同步超时");
        return false;
    }

    /**
     * 获取原主库追赶状态
     * @return 追赶状态信息
     */
    public RecoveryStatus getRecoveryStatus() {
        RecoveryStatus status = new RecoveryStatus();
        status.setOriginalMasterHealthy(!masterFailed.get());
        status.setCurrentStatus(currentStatus.get().name());

        if (currentStatus.get() == DataSourceStatus.SLAVE_PROMOTED) {
            try (Connection masterConn = masterDataSourceBean.getConnection();
                 Statement stmt = masterConn.createStatement();
                 ResultSet rs = stmt.executeQuery("SHOW SLAVE STATUS")) {

                if (rs.next()) {
                    String ioRunning = rs.getString("Slave_IO_Running");
                    String sqlRunning = rs.getString("Slave_SQL_Running");
                    String secondsBehind = rs.getString("Seconds_Behind_Master");
                    String readPos = rs.getString("Read_Master_Log_Pos");
                    String execPos = rs.getString("Exec_Master_Log_Pos");

                    status.setIoRunning("Yes".equals(ioRunning));
                    status.setSqlRunning("Yes".equals(sqlRunning));
                    status.setSecondsBehindMaster(secondsBehind != null ? Integer.parseInt(secondsBehind) : -1);
                    status.setCaughtUp(readPos != null && execPos != null && readPos.equals(execPos));
                    status.setReadyToSwitch(status.isCaughtUp() && status.isIoRunning() && status.isSqlRunning());
                }
            } catch (Exception e) {
                log.debug("获取追赶状态失败: {}", e.getMessage());
            }
        } else {
            status.setCaughtUp(true);
            status.setReadyToSwitch(false);
            status.setMessage("系统正常运行，无需切换");
        }

        return status;
    }

    /**
     * 将新主库（原从库）配置为原主库的从库（反向复制）
     */
    private void configureReverseReplication() throws SQLException {
        try (Connection slaveConn = slaveDataSourceBean.getConnection();
             Statement stmt = slaveConn.createStatement()) {

            // 1. 获取当前新主库（原从库）的复制位置
            try (ResultSet rs = stmt.executeQuery("SHOW MASTER STATUS")) {
                if (rs.next()) {
                    String masterLogFile = rs.getString("File");
                    long masterLogPos = rs.getLong("Position");
                    log.info("新主库当前位置: {} @ {}", masterLogFile, masterLogPos);
                }
            }

            // 2. 停止新主库的复制
            try {
                stmt.execute("STOP SLAVE");
                stmt.execute("RESET SLAVE ALL");
            } catch (SQLException ignored) {
            }

            // 3. 开启新主库只读
            stmt.execute("SET GLOBAL READ_ONLY=ON");
            stmt.execute("SET GLOBAL SUPER_READ_ONLY=ON");
            log.info("新主库已开启只读模式，作为备用从库");

        }
    }

    /**
     * 手动切换回主库
     */
    public void switchToMaster() {
        if (masterFailed.get()) {
            throw new RuntimeException("主库不可用，无法切换");
        }

        currentStatus.set(DataSourceStatus.MASTER_ACTIVE);
        log.info("已切换回主库");
    }

    // ==================== Getter Methods ====================

    public DataSourceStatus getCurrentStatus() {
        return currentStatus.get();
    }

    public boolean isMasterHealthy() {
        return !masterFailed.get();
    }

    public boolean isSlaveHealthy() {
        return !slaveFailed.get();
    }

    public HikariDataSource getMasterDataSourceBean() {
        return masterDataSourceBean;
    }

    public HikariDataSource getSlaveDataSourceBean() {
        return slaveDataSourceBean;
    }

    // ==================== Inner Classes ====================

    /**
     * 数据源上下文持有者
     */
    public static class DataSourceContextHolder {
        private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

        public static void set(DataSourceType type) {
            CONTEXT.set(type);
        }

        public static DataSourceType get() {
            return CONTEXT.get();
        }

        public static void clear() {
            CONTEXT.remove();
        }

        public static void useMaster() {
            set(DataSourceType.MASTER);
        }

        public static void useSlave() {
            set(DataSourceType.SLAVE);
        }
    }

    /**
     * 动态路由数据源
     */
    public static class RoutingDataSource extends AbstractRoutingDataSource {

        @Override
        protected Object determineCurrentLookupKey() {
            DataSourceType type = DataSourceContextHolder.get();
            if (type == null) {
                return DataSourceType.MASTER;
            }
            return type;
        }
    }

    /**
     * 追赶状态类
     */
    public static class RecoveryStatus {
        private boolean originalMasterHealthy;
        private String currentStatus;
        private boolean ioRunning;
        private boolean sqlRunning;
        private int secondsBehindMaster;
        private boolean caughtUp;
        private boolean readyToSwitch;
        private String message;

        public boolean isOriginalMasterHealthy() { return originalMasterHealthy; }
        public void setOriginalMasterHealthy(boolean v) { this.originalMasterHealthy = v; }
        public String getCurrentStatus() { return currentStatus; }
        public void setCurrentStatus(String v) { this.currentStatus = v; }
        public boolean isIoRunning() { return ioRunning; }
        public void setIoRunning(boolean v) { this.ioRunning = v; }
        public boolean isSqlRunning() { return sqlRunning; }
        public void setSqlRunning(boolean v) { this.sqlRunning = v; }
        public int getSecondsBehindMaster() { return secondsBehindMaster; }
        public void setSecondsBehindMaster(int v) { this.secondsBehindMaster = v; }
        public boolean isCaughtUp() { return caughtUp; }
        public void setCaughtUp(boolean v) { this.caughtUp = v; }
        public boolean isReadyToSwitch() { return readyToSwitch; }
        public void setReadyToSwitch(boolean v) { this.readyToSwitch = v; }
        public String getMessage() { return message; }
        public void setMessage(String v) { this.message = v; }
    }
}
