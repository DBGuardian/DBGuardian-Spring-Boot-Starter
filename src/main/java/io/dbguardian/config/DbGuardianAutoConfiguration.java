package io.dbguardian.config;

import com.zaxxer.hikari.HikariDataSource;
import io.dbguardian.aspect.DbGuardianDataSourceAspect;
import io.dbguardian.coordination.DatasourceCoordinationService;
import io.dbguardian.coordination.DatasourceStatusListener;
import io.dbguardian.enums.DataSourceStatus;
import io.dbguardian.enums.DataSourceType;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
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
 * DBGuardian 自动配置类
 *
 * 职责：负责读写分离数据源配置，同时也提供 MyBatis 兼容的数据源 Bean
 * 组件内部排除 Spring Boot 默认数据源自动配置
 *
 * 支持功能：
 * - 读写分离（主库写，从库读）
 * - 主从故障转移（从库升主库）
 * - 分布式协调（基于 Redis）
 * - 健康检查与自动恢复
 */
@Configuration
@AutoConfigureOrder(Integer.MIN_VALUE)
@EnableConfigurationProperties(DataSourceProperties.class)
@EnableScheduling
@Slf4j
@AutoConfigureBefore(name = {"com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration", "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"})
public class DbGuardianAutoConfiguration {

    // ==================== 配置属性 ====================

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

    @Value("${spring.datasource.replication.master-user:repl}")
    private String replicationMasterUser;

    @Value("${spring.datasource.replication.master-password:}")
    private String replicationMasterPassword;

    @Value("${spring.datasource.allow-degraded-startup:true}")
    private boolean allowDegradedStartup;

    // ==================== 运行时状态 ====================

    private HikariDataSource masterDataSourceBean;
    private HikariDataSource slaveDataSourceBean;
    private volatile String currentNewMasterHost;
    private volatile boolean initializationComplete = false;
    private final AtomicBoolean recoveryInProgress = new AtomicBoolean(false);
    private final AtomicReference<DataSourceStatus> currentStatus = new AtomicReference<>(DataSourceStatus.MASTER_ACTIVE);
    private final AtomicBoolean masterFailed = new AtomicBoolean(false);
    private final AtomicBoolean slaveFailed = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /** 数据源协调服务 */
    private DatasourceCoordinationService coordinationService;

    @Autowired
    private ApplicationContext applicationContext;

    // ==================== Bean 定义 ====================

    @Bean("dbguardianMasterDataSource")
    public DataSource masterDataSource() {
        HikariDataSource ds = createDataSource(masterUrl, masterUsername, masterPassword, driverClassName, masterPoolSize, masterMinIdle, "dbguardian-master-pool");
        masterDataSourceBean = ds;
        return ds;
    }

    @Bean("dbguardianSlaveDataSource")
    public DataSource slaveDataSource() {
        HikariDataSource ds = createDataSource(slaveUrl, slaveUsername, slavePassword, slaveDriverClassName, slavePoolSize, slaveMinIdle, "dbguardian-slave-pool");
        slaveDataSourceBean = ds;
        return ds;
    }

    private HikariDataSource createDataSource(String url, String username, String password, String driverClassName, int poolSize, int minIdle, String poolName) {
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
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setValidationTimeout(5000);
        return dataSource;
    }

    @Bean("dataSource")
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

        log.info("DBGuardian 读写分离数据源已初始化");
        return routingDataSource;
    }

    /**
     * 事务管理器 - 支持 @Transactional 注解
     */
    @Bean
    @Primary
    public DataSourceTransactionManager dataSourceTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatasourceCoordinationService datasourceCoordinationService(
            @Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
            @Value("${spring.application.name:dbguardian}") String applicationName) {
        DatasourceCoordinationService service = new DatasourceCoordinationService();
        // 直接注入 redisTemplate
        if (redisTemplate != null) {
            service.setRedisTemplate(redisTemplate);
        }
        service.setApplicationName(applicationName);
        return service;
    }

    /**
     * Redis 消息监听容器
     * 用于订阅数据源状态变更消息，实现多实例间的状态同步
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            @Autowired(required = false) RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        if (redisConnectionFactory != null) {
            container.setConnectionFactory(redisConnectionFactory);
            log.info("Redis 消息监听容器已初始化");
        }
        return container;
    }

    /**
     * RedisTemplate Bean
     * 用于 DatasourceCoordinationService 的 Redis 操作
     */
    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        template.setValueSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        template.setHashValueSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public DbGuardianDataSourceAspect dbGuardianDataSourceAspect() {
        return new DbGuardianDataSourceAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    public DatasourceStatusListener datasourceStatusListener(
            @Autowired(required = false) RedisMessageListenerContainer redisMessageListenerContainer,
            @Autowired(required = false) DatasourceCoordinationService datasourceCoordinationService) {
        DatasourceStatusListener listener = new DatasourceStatusListener();
        listener.setContainer(redisMessageListenerContainer);
        listener.setCoordinationService(datasourceCoordinationService);
        listener.subscribe();
        return listener;
    }

    // ==================== 初始化与健康检查 ====================

    @PostConstruct
    public void init() {
        log.info("=== DBGuardian 读写分离配置初始化 ===");

        boolean masterOk = checkMasterHealth();
        boolean slaveOk = checkSlaveHealth();

        if (masterOk || slaveOk) {
            log.info("数据源连接正常");
            currentStatus.set(DataSourceStatus.MASTER_ACTIVE);
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
        initializationComplete = true;
    }

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

        initializeRedisCoordination();
    }

    private void initializeRedisCoordination() {
        try {
            DatasourceCoordinationService service = applicationContext.getBean(DatasourceCoordinationService.class);
            if (service != null && service.isHealthy()) {
                service.initialize();
                syncStatusToRedis(service);
                coordinationService = service;
                log.info("Redis 分布式协调服务已启用");
            } else {
                log.warn("Redis 协调服务不可用，将使用单机模式");
            }
        } catch (Exception e) {
            log.warn("Redis 协调服务不可用，将使用单机模式");
        }
    }

    private void syncStatusToRedis(DatasourceCoordinationService service) {
        if (service == null || !service.isHealthy()) {
            return;
        }
        try {
            service.registerAsMaster(service.getInstanceId());
            String status = currentStatus.get() == DataSourceStatus.SLAVE_PROMOTED
                    ? DatasourceCoordinationService.STATUS_SLAVE_PROMOTED
                    : DatasourceCoordinationService.STATUS_NORMAL;
            service.setMasterStatus(status);
            log.info("状态已同步到 Redis: {}", status);
        } catch (Exception e) {
            log.warn("同步状态到 Redis 失败: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 30000)
    public void healthCheck() {
        if (!initializationComplete) return;
        checkMasterHealth();
        checkSlaveHealth();

        // 定期检查复制状态
        if (slaveDataSourceBean != null) {
            checkReplicationStatus(slaveDataSourceBean, "定期检查");
        }

        // 定期检测 GTID 一致性，防止从库被误写入
        checkGtidConsistency();

        handleFailover();
    }

    /**
     * 定期检测主从 GTID 一致性
     * 如果检测到从库有额外数据，立即触发数据一致性保护机制
     */
    private void checkGtidConsistency() {
        // 恢复过程中跳过检测，避免干扰恢复流程
        if (recoveryInProgress.get()) {
            return;
        }
        try {
            java.util.Set<String> extraGtids = detectSlaveExtraData();
            if (!extraGtids.isEmpty()) {
                log.error("!!! 警告：检测到从库存在非复制来源的数据 !!!");
                log.error("GTID 数量: {}", extraGtids.size());
                log.error("触发数据一致性保护机制...");
                handleSlaveDataContamination();
            }
        } catch (Exception e) {
            log.debug("GTID 一致性检测失败: {}", e.getMessage());
        }
    }

    private boolean checkMasterHealth() {
        if (masterDataSourceBean == null) return false;
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

    private boolean checkSlaveHealth() {
        if (slaveDataSourceBean == null) return false;
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

    private void handleFailover() {
        if (currentStatus.get() == DataSourceStatus.DEGRADED) return;

        DataSourceStatus oldStatus = currentStatus.get();
        if (oldStatus == DataSourceStatus.MASTER_ACTIVE) {
            if (masterFailed.get() && !slaveFailed.get()) {
                log.warn("主库故障，从库将升为主库");
                autoPromoteSlaveToMaster();
            }
        } else if (oldStatus == DataSourceStatus.SLAVE_PROMOTED) {
            if (!masterFailed.get() && !slaveFailed.get() && !recoveryInProgress.get()) {
                log.info("原主库已恢复，开始恢复流程...");
                recoverOriginalMasterAndRestoreReplication();
            }
        }
    }

    /**
     * 恢复原主库并重建主从复制关系
     * 流程：原主库作为从库连接到临时新主库 → 追赶数据 → 原主库恢复主库 → 原从库重新复制原主库
     */
    private void recoverOriginalMasterAndRestoreReplication() {
        if (!recoveryInProgress.compareAndSet(false, true)) return;
        try {
            log.info("=== 开始恢复原主库并重建主从复制 ===");

            // 1. 原主库连接到新主库并追赶数据
            log.info("步骤1: 原主库开始追赶新主库...");
            recoverMasterAsSlave();

            // 2. 等待追赶完成
            log.info("步骤2: 等待数据追赶完成...");
            waitForCatchupOnMaster();

            // 3. 原主库追赶完成后重新恢复为主库，不能交换数据源引用。
            // 否则后续再次断开原主库时，会被误判为“从库连接失败”。
            log.info("步骤3: 原主库恢复主库角色...");
            restoreOriginalMasterRole();

            // 4. 原从库重新作为从库复制原主库
            log.info("步骤4: 配置原从库重新复制原主库...");
            configureSlaveReplication(masterDataSourceBean, slaveDataSourceBean,
                extractHost(masterUrl), extractPort(masterUrl));

            // 5. 验证复制状态
            log.info("步骤5: 验证主从复制状态...");
            checkReplicationStatusAsync(slaveDataSourceBean, "重建后从库");

            // 6. 状态恢复正常
            currentStatus.set(DataSourceStatus.MASTER_ACTIVE);
            currentNewMasterHost = null;
            masterFailed.set(false);
            slaveFailed.set(false);
            DataSourceContextHolder.clear();
            syncStatusToRedis(coordinationService);

            log.info("=== 主从复制恢复完成 ===");

        } catch (Exception e) {
            log.error("恢复主从复制失败: {}", e.getMessage());
            currentStatus.set(DataSourceStatus.SLAVE_PROMOTED);
        } finally {
            recoveryInProgress.set(false);
        }
    }

    /**
     * 原主库追赶完成后恢复为主库，保持 master/slave 数据源引用不变。
     */
    private void restoreOriginalMasterRole() {
        disableReadOnlyOnDataSource(masterDataSourceBean);
        stopSlaveReplication(masterDataSourceBean);
        DataSourceContextHolder.useMaster();
        log.info("原主库已恢复为主库");
    }

    /**
     * 等待原主库追赶完成
     */
    private void waitForCatchupOnMaster() {
        log.info("等待原主库追赶完成...");
        int maxWaitSeconds = 300;
        int waitedSeconds = 0;
        int checkInterval = 2;

        while (waitedSeconds < maxWaitSeconds) {
            try {
                Thread.sleep(checkInterval * 1000);
                waitedSeconds += checkInterval;

                try (Connection conn = masterDataSourceBean.getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW SLAVE STATUS")) {
                    if (rs.next()) {
                        String ioRunning = rs.getString("Slave_IO_Running");
                        String sqlRunning = rs.getString("Slave_SQL_Running");
                        String behind = rs.getString("Seconds_Behind_Master");

                        if ("Yes".equals(ioRunning) && "Yes".equals(sqlRunning)) {
                            if ("0".equals(behind) || "NULL".equals(behind)) {
                                log.info("原主库追赶完成");
                                return;
                            }
                            log.info("追赶中... 延迟: {}秒", behind);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("检查追赶状态失败: {}", e.getMessage());
            }
        }
        log.warn("追赶超时，继续执行");
    }

    private void autoPromoteSlaveToMaster() {
        try {
            currentNewMasterHost = slaveUrl;
            disableSlaveReadOnly();
            currentStatus.set(DataSourceStatus.SLAVE_PROMOTED);
            DataSourceContextHolder.useMaster();
            syncStatusToRedis(coordinationService);
            log.info("故障转移完成：从库已升为主库");
        } catch (Exception e) {
            log.error("故障转移失败: {}", e.getMessage());
        }
    }

    private void disableSlaveReadOnly() {
        try (Connection conn = slaveDataSourceBean.getConnection(); Statement stmt = conn.createStatement()) {
            try {
                stmt.execute("STOP SLAVE");
                stmt.execute("RESET SLAVE ALL");
            } catch (SQLException e) {
                log.debug("停止主从复制失败: {}", e.getMessage());
            }
            stmt.execute("SET GLOBAL READ_ONLY=OFF");
            stmt.execute("SET GLOBAL SUPER_READ_ONLY=OFF");
            log.info("从库只读模式已关闭");
        } catch (SQLException e) {
            log.warn("关闭从库只读模式失败: {}", e.getMessage());
        }
    }

    /**
     * 在指定数据源上启用只读模式
     */
    private void enableReadOnlyOnDataSource(DataSource ds) {
        if (ds == null) return;
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET GLOBAL READ_ONLY=ON");
            stmt.execute("SET GLOBAL SUPER_READ_ONLY=ON");
            log.info("已启用数据源只读模式: READ_ONLY=ON, SUPER_READ_ONLY=ON");
        } catch (SQLException e) {
            log.warn("启用只读模式失败: {}", e.getMessage());
        }
    }

    private void recoverOriginalMaster() {
        if (!recoveryInProgress.compareAndSet(false, true)) return;
        try {
            log.info("=== 开始恢复原主库 ===");
            recoverMasterAsSlave();
            log.info("=== 原主库恢复完成 ===");
        } catch (Exception e) {
            log.error("原主库恢复失败: {}", e.getMessage());
        } finally {
            recoveryInProgress.set(false);
        }
    }

    private void recoverMasterAsSlave() {
        if (currentNewMasterHost == null) {
            log.error("新主库地址为空，无法恢复原主库");
            return;
        }
        if (replicationMasterUser == null || replicationMasterUser.isEmpty() ||
            replicationMasterPassword == null || replicationMasterPassword.isEmpty()) {
            log.info("未配置复制用户，跳过自动恢复配置");
            return;
        }
        try (Connection masterConn = masterDataSourceBean.getConnection(); Statement stmt = masterConn.createStatement()) {
            try {
                stmt.execute("STOP SLAVE");
                stmt.execute("RESET SLAVE ALL");
            } catch (SQLException ignored) {}

            String newMasterHost = extractHost(currentNewMasterHost);
            int newMasterPort = extractPort(currentNewMasterHost);

            String sql = String.format(
                "CHANGE MASTER TO MASTER_HOST='%s', MASTER_PORT=%d, MASTER_USER='%s', MASTER_PASSWORD='%s', MASTER_AUTO_POSITION=1",
                newMasterHost, newMasterPort, replicationMasterUser, replicationMasterPassword);
            stmt.execute(sql);
            stmt.execute("START SLAVE");
            log.info("已配置原主库连接到新主库: {}:{}", newMasterHost, newMasterPort);

            // 异步检查原主库恢复后的复制状态
            checkReplicationStatusAsync(masterDataSourceBean, "原主库恢复");
        } catch (SQLException e) {
            log.error("配置原主库为主从复制失败: {}", e.getMessage());
        }
    }

    // ==================== 主从判断与配置 ====================

    private void determineAndConfigureMasterSlave() {
        try {
            String masterGtid = getGtidPosition(masterDataSourceBean);
            String slaveGtid = getGtidPosition(slaveDataSourceBean);

            log.info("主库 GTID: {}", masterGtid);
            log.info("从库 GTID: {}", slaveGtid);

            // 检测从库是否有额外的写入数据
            java.util.Set<String> slaveExtraGtids = detectSlaveExtraData();

            // 如果从库有额外数据，触发数据一致性保护机制
            if (!slaveExtraGtids.isEmpty()) {
                log.warn("检测到从库存在非复制来源的数据！");
                log.warn("从库可能曾被误写入数据，启用主库追赶机制");
                handleSlaveDataContamination();
                // 重新获取 GTID
                masterGtid = getGtidPosition(masterDataSourceBean);
                slaveGtid = getGtidPosition(slaveDataSourceBean);
            }

            boolean masterNewer = isNewerThan(masterGtid, slaveGtid);

            if (masterNewer) {
                log.info("主库数据更新，以主库为主库");
                disableReadOnlyOnDataSource(masterDataSourceBean);
                configureSlaveReplication(masterDataSourceBean, slaveDataSourceBean,
                    extractHost(masterUrl), extractPort(masterUrl));
            } else {
                log.info("从库数据更新，以从库为主库");
                disableReadOnlyOnDataSource(slaveDataSourceBean);
                configureSlaveReplication(slaveDataSourceBean, masterDataSourceBean,
                    extractHost(slaveUrl), extractPort(slaveUrl));

                HikariDataSource temp = masterDataSourceBean;
                masterDataSourceBean = slaveDataSourceBean;
                slaveDataSourceBean = temp;
                currentNewMasterHost = slaveUrl;
                currentStatus.set(DataSourceStatus.SLAVE_PROMOTED);
            }
        } catch (Exception e) {
            log.error("自动判断主从失败: {}", e.getMessage());
            currentStatus.set(DataSourceStatus.MASTER_ACTIVE);
        }
    }

    private void disableReadOnlyOnDataSource(DataSource ds) {
        if (ds == null) return;
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET GLOBAL READ_ONLY=OFF");
            stmt.execute("SET GLOBAL SUPER_READ_ONLY=OFF");
            log.info("已关闭数据源只读模式");
        } catch (SQLException e) {
            log.debug("关闭只读模式失败: {}", e.getMessage());
        }
    }

    private String getGtidPosition(DataSource ds) {
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW MASTER STATUS")) {
            if (rs.next()) {
                return rs.getString("Executed_Gtid_Set");
            }
        } catch (SQLException e) {
            log.debug("获取 GTID 失败: {}", e.getMessage());
        }
        return "";
    }

    private boolean isNewerThan(String gtid1, String gtid2) {
        if (gtid1 == null || gtid1.isEmpty()) return false;
        if (gtid2 == null || gtid2.isEmpty()) return true;
        try {
            Set<String> set1 = new java.util.HashSet<>(Arrays.asList(gtid1.split(",")));
            Set<String> set2 = new java.util.HashSet<>(Arrays.asList(gtid2.split(",")));
            Set<String> all = new java.util.HashSet<>(set1);
            all.addAll(set2);
            int count1 = 0, count2 = 0;
            for (String gtid : all) {
                if (set1.contains(gtid)) count1++;
                if (set2.contains(gtid)) count2++;
            }
            return count1 >= count2;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 检测从库是否有额外的数据（被误写入的数据）
     * 通过比较 GTID 判断从库是否比主库多了事务
     *
     * @return 从库独有的 GTID 集合，如果为空表示无额外数据
     */
    private java.util.Set<String> detectSlaveExtraData() {
        if (masterDataSourceBean == null || slaveDataSourceBean == null) {
            return new java.util.HashSet<>();
        }

        try {
            String masterGtid = getGtidPosition(masterDataSourceBean);
            String slaveGtid = getGtidPosition(slaveDataSourceBean);

            if (masterGtid.isEmpty() || slaveGtid.isEmpty()) {
                return new java.util.HashSet<>();
            }

            java.util.Set<String> masterSet = new java.util.HashSet<>(java.util.Arrays.asList(masterGtid.split(",")));
            java.util.Set<String> slaveSet = new java.util.HashSet<>(java.util.Arrays.asList(slaveGtid.split(",")));

            // 从库独有的 GTID（这些事务只存在于从库，不在主库）
            java.util.Set<String> extraInSlave = new java.util.HashSet<>(slaveSet);
            extraInSlave.removeAll(masterSet);

            if (!extraInSlave.isEmpty()) {
                log.warn("检测到从库存在额外数据！GTID数量: {}", extraInSlave.size());
                // 显示前5个GTID示例
                StringBuilder examples = new StringBuilder();
                int count = 0;
                for (String g : extraInSlave) {
                    if (count++ >= 5) break;
                    if (examples.length() > 0) examples.append(", ");
                    examples.append(g);
                }
                log.warn("从库额外GTID示例: {}", examples);
            }

            return extraInSlave;
        } catch (Exception e) {
            log.error("检测从库额外数据失败: {}", e.getMessage());
            return new java.util.HashSet<>();
        }
    }

    /**
     * 从库被误写入数据后的处理策略
     * 主库开始追赶从库，保持数据一致性
     */
    private void handleSlaveDataContamination() {
        if (currentStatus.get() == DataSourceStatus.SLAVE_PROMOTED) {
            log.warn("当前是从库升主状态，跳过数据追赶（从库已变成主库）");
            return;
        }

        log.warn("=== 从库数据污染检测触发 ===");
        log.warn("检测到从库存在非复制的写入操作，启用主库追赶机制");

        try {
            // 记录从库额外数据的 GTID
            java.util.Set<String> extraGtids = detectSlaveExtraData();
            if (extraGtids.isEmpty()) {
                log.info("GTID集合为空，无需追赶");
                return;
            }

            // 获取从库的 binlog 位置信息
            String slaveBinlogInfo = getSlaveBinlogInfo();
            log.info("从库 Binlog 信息: {}", slaveBinlogInfo);

            // 执行主库追赶从库逻辑
            masterCatchesUpFromSlave();

            // 验证数据一致性
            verifyDataConsistency();

            log.warn("=== 主库追赶完成 ===");

        } catch (Exception e) {
            log.error("主库追赶失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 主库追赶从库
     * 策略：将主库临时配置为从库，从原从库复制缺失的事务
     */
    private void masterCatchesUpFromSlave() {
        if (masterDataSourceBean == null || slaveDataSourceBean == null) {
            log.error("主库或从库连接不可用，无法执行追赶");
            return;
        }

        if (replicationMasterUser == null || replicationMasterUser.isEmpty()) {
            log.error("未配置复制用户，无法执行主库追赶");
            return;
        }

        log.info("=== 开始主库追赶从库 ===");

        try {
            // 1. 获取从库作为"临时主库"的连接信息
            String slaveHost = extractHost(slaveUrl);
            int slavePort = extractPort(slaveUrl);

            // 2. 停止主库的读写操作（临时降级）
            log.info("停止主库写入，准备数据追赶...");
            enableReadOnlyOnDataSource(masterDataSourceBean);

            // 3. 停止主库原有的复制（如果有）
            try (Connection masterConn = masterDataSourceBean.getConnection();
                 Statement stmt = masterConn.createStatement()) {
                try {
                    stmt.execute("STOP SLAVE");
                    stmt.execute("RESET SLAVE ALL");
                } catch (SQLException ignored) {
                    // 主库可能没有配置为从库
                }
            }

            // 4. 获取从库的 GTID Executed 位置
            String slaveGtidExecuted = getSlaveGtidExecuted();
            log.info("从库 GTID Executed: {}", slaveGtidExecuted);

            // 获取主库的 GTID UUID 列表
            String masterGtidExecuted = getGtidPosition(masterDataSourceBean);
            log.info("主库 GTID Executed: {}", masterGtidExecuted);

            // 5. 在主库上设置 GTID 复制位置并启动复制
            try (Connection masterConn = masterDataSourceBean.getConnection();
                 Statement stmt = masterConn.createStatement()) {

                // 先设置 gtid_purged，允许主库接受从从库过来的 UUID 事务
                // 因为主库和从库的 binlog UUID 可能不同
                java.util.Set<String> masterUuids = extractUuids(masterGtidExecuted);
                java.util.Set<String> slaveUuids = extractUuids(slaveGtidExecuted);

                // 从库的 UUID 集合中移除主库已有的 UUID
                slaveUuids.removeAll(masterUuids);
                if (!slaveUuids.isEmpty()) {
                    String gtidPurged = String.join(",", slaveUuids);
                    try {
                        stmt.execute("SET GLOBAL gtid_purged = '" + gtidPurged + "'");
                        log.info("已设置 gtid_purged: {}", gtidPurged);
                    } catch (SQLException e) {
                        log.debug("设置 gtid_purged 失败（可能已设置）: {}", e.getMessage());
                    }
                }

                // 设置主库从从库复制（使用 GTID 自动定位）
                String sql = String.format(
                    "CHANGE MASTER TO MASTER_HOST='%s', MASTER_PORT=%d, MASTER_USER='%s', MASTER_PASSWORD='%s', MASTER_AUTO_POSITION=1",
                    slaveHost, slavePort, replicationMasterUser, replicationMasterPassword);
                stmt.execute(sql);

                log.info("已配置主库从从库复制: {}:{}", slaveHost, slavePort);

                // 6. 启动复制
                stmt.execute("START SLAVE");
                log.info("主库开始追赶从库...");
            }

            // 7. 等待追赶完成（检查复制状态）
            waitForCatchup();

            // 8. 停止追赶复制
            try (Connection masterConn = masterDataSourceBean.getConnection();
                 Statement stmt = masterConn.createStatement()) {
                stmt.execute("STOP SLAVE");
                stmt.execute("RESET SLAVE ALL");
                log.info("已停止主库追赶复制");
            }

            // 9. 恢复主库读写模式
            disableReadOnlyOnDataSource(masterDataSourceBean);

            // 10. 恢复原主从复制关系
            restoreOriginalReplication();

            log.info("=== 主库追赶完成 ===");

        } catch (SQLException e) {
            log.error("主库追赶失败: {}", e.getMessage(), e);
            // 确保恢复主库读写模式
            try {
                disableReadOnlyOnDataSource(masterDataSourceBean);
            } catch (Exception ex) {
                log.debug("恢复主库读写模式失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 获取从库的 GTID Executed 位置
     */
    private String getSlaveGtidExecuted() {
        try (Connection conn = slaveDataSourceBean.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW SLAVE STATUS")) {
            if (rs.next()) {
                return rs.getString("Executed_Gtid_Set");
            }
        } catch (SQLException e) {
            log.debug("获取从库 GTID Executed 失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 等待主库追赶完成
     * 如果复制出错（表不存在等），跳过错误继续追赶
     */
    private void waitForCatchup() {
        log.info("等待主库追赶完成...");

        int maxWaitSeconds = 300; // 最多等待5分钟
        int waitedSeconds = 0;
        int checkInterval = 2; // 每2秒检查一次
        int skipCount = 0;
        int maxSkipCount = 10; // 最多跳过10个错误事务

        while (waitedSeconds < maxWaitSeconds) {
            try {
                Thread.sleep(checkInterval * 1000);
                waitedSeconds += checkInterval;

                // 检查复制状态
                boolean ioRunning = false;
                boolean sqlRunning = false;
                long secondsBehind = -1;
                String lastError = null;
                long lastErrorNumber = 0;

                try (Connection masterConn = masterDataSourceBean.getConnection();
                     Statement stmt = masterConn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW SLAVE STATUS")) {
                    if (rs.next()) {
                        ioRunning = "Yes".equals(rs.getString("Slave_IO_Running"));
                        sqlRunning = "Yes".equals(rs.getString("Slave_SQL_Running"));
                        String behind = rs.getString("Seconds_Behind_Master");
                        lastError = rs.getString("Last_Error");
                        String errNo = rs.getString("Last_Errno");
                        if (behind != null && !behind.equals("NULL")) {
                            secondsBehind = Long.parseLong(behind);
                        }
                        if (errNo != null && !errNo.isEmpty()) {
                            lastErrorNumber = Long.parseLong(errNo);
                        }
                    }
                }

                // 检测可跳过的错误
                if (lastErrorNumber > 0 && sqlRunning) {
                    // 1146 = 表不存在, 1050 = 表已存在, 1032 = 记录不存在
                    if (lastErrorNumber == 1146 || lastErrorNumber == 1050 || lastErrorNumber == 1032) {
                        if (skipCount < maxSkipCount) {
                            log.warn("检测到复制错误 {}: {}，跳过错误事务继续追赶...", lastErrorNumber, lastError);
                            try {
                                try (Connection skipConn = masterDataSourceBean.getConnection();
                                     Statement skipStmt = skipConn.createStatement()) {
                                    skipStmt.execute("STOP SLAVE");
                                    skipStmt.execute("SET @@SESSION.SQL_SLAVE_SKIP_COUNTER = 1");
                                    skipStmt.execute("START SLAVE");
                                }
                                skipCount++;
                                log.info("已跳过错误事务，继续追赶 (已跳过 {} 个)", skipCount);
                                continue;
                            } catch (SQLException skipErr) {
                                log.debug("跳过事务失败: {}", skipErr.getMessage());
                            }
                        } else {
                            log.error("跳过错误次数过多，停止追赶");
                            return;
                        }
                    } else if (lastErrorNumber != 0) {
                        log.error("复制遇到不可忽略的错误 {}: {}", lastErrorNumber, lastError);
                        return;
                    }
                }

                if (ioRunning && sqlRunning) {
                    if (secondsBehind == 0) {
                        log.info("主库追赶完成（IO: {}, SQL: {}, 延迟: 0秒, 跳过 {} 个错误）", ioRunning, sqlRunning, skipCount);
                        return;
                    } else if (secondsBehind > 0) {
                        log.info("追赶中... IO: {}, SQL: {}, 延迟: {}秒 (已等待{}秒)",
                                 ioRunning, sqlRunning, secondsBehind, waitedSeconds);
                    }
                } else if (!sqlRunning && lastErrorNumber == 0) {
                    // SQL 线程停止但没有错误，可能是追赶完成了
                    log.info("SQL 线程已停止，等待追赶完成...");
                }

            } catch (Exception e) {
                log.debug("检查追赶状态失败: {}", e.getMessage());
            }
        }

        log.warn("主库追赶超时（等待超过{}秒）", maxWaitSeconds);
    }

    /**
     * 恢复原主从复制关系
     */
    private void restoreOriginalReplication() {
        if (masterDataSourceBean == null || slaveDataSourceBean == null) {
            return;
        }

        if (replicationMasterUser == null || replicationMasterUser.isEmpty()) {
            log.info("未配置复制用户，跳过原主从复制恢复");
            return;
        }

        try {
            String masterHost = extractHost(masterUrl);
            int masterPort = extractPort(masterUrl);

            log.info("恢复原主从复制: {}:{}", masterHost, masterPort);

            // 配置从库复制主库
            configureSlaveReplication(masterDataSourceBean, slaveDataSourceBean, masterHost, masterPort);

        } catch (Exception e) {
            log.error("恢复原主从复制失败: {}", e.getMessage());
        }
    }

    /**
     * 兼容旧方法名
     */
    private void syncExtraDataFromSlave(java.util.Set<String> extraGtids) {
        masterCatchesUpFromSlave();
    }

    /**
     * 获取从库的 binlog 信息
     */
    private String getSlaveBinlogInfo() {
        try (Connection conn = slaveDataSourceBean.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW SLAVE STATUS")) {
            if (rs.next()) {
                String file = rs.getString("Master_Log_File");
                String pos = rs.getString("Read_Master_Log_Pos");
                return String.format("File: %s, Position: %s", file, pos);
            }
        } catch (SQLException e) {
            log.debug("获取从库 binlog 信息失败: {}", e.getMessage());
        }
        return "N/A";
    }

    /**
     * 停止从库复制
     */
    private void stopSlaveReplication(com.zaxxer.hikari.HikariDataSource ds) {
        if (ds == null) return;
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("STOP SLAVE");
            stmt.execute("RESET SLAVE ALL");
            log.info("已停止从库复制");
        } catch (SQLException e) {
            log.debug("停止从库复制失败: {}", e.getMessage());
        }
    }

    /**
     * 验证主从数据一致性
     */
    private void verifyDataConsistency() {
        executorService.submit(() -> {
            try {
                Thread.sleep(3000); // 等待复制追赶

                java.util.Set<String> remainingExtra = detectSlaveExtraData();
                if (remainingExtra.isEmpty()) {
                    log.info("数据一致性验证通过：主从 GTID 已同步");
                } else {
                    log.warn("数据一致性警告：仍有 {} 个 GTID 未同步", remainingExtra.size());
                    log.warn("建议：使用 pt-table-checksum 等工具进行深度数据校验");
                }
            } catch (Exception e) {
                log.debug("数据一致性验证失败: {}", e.getMessage());
            }
        });
    }

    private void configureSlaveReplication(DataSource masterDs, DataSource slaveDs, String masterHost, int masterPort) {
        if (replicationMasterUser == null || replicationMasterUser.isEmpty() ||
            replicationMasterPassword == null || replicationMasterPassword.isEmpty()) {
            log.info("未配置复制用户，跳过主从复制配置");
            return;
        }
        try (Connection slaveConn = slaveDs.getConnection(); Statement stmt = slaveConn.createStatement()) {
            disableReadOnlyOnDataSource(masterDs);
            try {
                stmt.execute("STOP SLAVE");
                stmt.execute("RESET SLAVE ALL");
            } catch (SQLException ignored) {}

            String sql = String.format(
                "CHANGE MASTER TO MASTER_HOST='%s', MASTER_PORT=%d, MASTER_USER='%s', MASTER_PASSWORD='%s', MASTER_AUTO_POSITION=1",
                masterHost, masterPort, replicationMasterUser, replicationMasterPassword);
            stmt.execute(sql);
            stmt.execute("START SLAVE");

            // 启用从库只读模式，防止任何写入操作
            enableReadOnlyOnDataSource(slaveDs);

            log.info("已配置从库复制主库: {}:{}", masterHost, masterPort);

            // 异步检查复制状态
            checkReplicationStatusAsync(slaveDs, "主从复制");
        } catch (SQLException e) {
            log.error("配置主从复制失败: {}", e.getMessage());
        }
    }

    /**
     * 异步检查复制状态
     * @param ds 数据源
     * @param desc 描述（用于日志）
     */
    private void checkReplicationStatusAsync(DataSource ds, String desc) {
        executorService.submit(() -> {
            try {
                Thread.sleep(2000);
                checkReplicationStatus(ds, desc);
            } catch (Exception e) {
                log.debug("异步检查复制状态失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 检查并打印复制状态
     * @param ds 数据源
     * @param desc 描述
     */
    private void checkReplicationStatus(DataSource ds, String desc) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW SLAVE STATUS")) {
            if (rs.next()) {
                String ioRunning = rs.getString("Slave_IO_Running");
                String sqlRunning = rs.getString("Slave_SQL_Running");
                String secondsBehind = rs.getString("Seconds_Behind_Master");
                log.info("{} - IO: {}, SQL: {}", desc, ioRunning, sqlRunning);
                if (secondsBehind != null && !secondsBehind.equals("0") && !secondsBehind.equals("NULL")) {
                    log.info("{} - 复制延迟: {}秒", desc, secondsBehind);
                }
            }
        } catch (SQLException e) {
            log.debug("检查复制状态失败: {}", e.getMessage());
        }
    }

    /**
     * 从 GTID 集合中提取所有 UUID
     * @param gtidSet GTID 集合字符串，格式如 "uuid1:1-100,uuid2:1-50"
     * @return UUID 集合
     */
    private java.util.Set<String> extractUuids(String gtidSet) {
        java.util.Set<String> uuids = new java.util.HashSet<>();
        if (gtidSet == null || gtidSet.isEmpty()) {
            return uuids;
        }
        // GTID 格式: uuid:interval 或 uuid:interval1-interval2
        String[] gtids = gtidSet.split(",");
        for (String gtid : gtids) {
            String[] parts = gtid.trim().split(":");
            if (parts.length >= 1) {
                uuids.add(parts[0]);
            }
        }
        return uuids;
    }

    private String extractHost(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        int start = jdbcUrl.indexOf("://") + 3;
        int end = jdbcUrl.indexOf(":", start);
        if (end == -1) end = jdbcUrl.indexOf("/", start);
        return jdbcUrl.substring(start, end);
    }

    private int extractPort(String jdbcUrl) {
        if (jdbcUrl == null) return 3306;
        int start = jdbcUrl.indexOf("://") + 3;
        start = jdbcUrl.indexOf(":", start) + 1;
        int end = jdbcUrl.indexOf("/", start);
        return Integer.parseInt(jdbcUrl.substring(start, end));
    }

    // ==================== 内部类 ====================

    public static class DataSourceContextHolder {
        private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

        public static void set(DataSourceType type) { CONTEXT.set(type); }
        public static DataSourceType get() { return CONTEXT.get(); }
        public static void clear() { CONTEXT.remove(); }
        public static void useMaster() { set(DataSourceType.MASTER); }
        public static void useSlave() { set(DataSourceType.SLAVE); }
    }

    public static class RoutingDataSource extends AbstractRoutingDataSource {
        @Override
        protected Object determineCurrentLookupKey() {
            DataSourceType type = DataSourceContextHolder.get();
            return type == null ? DataSourceType.MASTER : type;
        }
    }

    @PreDestroy
    public void destroy() {
        if (executorService != null) executorService.shutdown();
        if (masterDataSourceBean != null && !masterDataSourceBean.isClosed()) masterDataSourceBean.close();
        if (slaveDataSourceBean != null && !slaveDataSourceBean.isClosed()) slaveDataSourceBean.close();
    }

    // ==================== 公开方法 ====================

    public DataSourceStatus getCurrentStatus() {
        return currentStatus.get();
    }
}
