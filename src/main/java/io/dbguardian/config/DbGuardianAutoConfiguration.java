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
import org.springframework.context.annotation.Lazy;
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
        handleFailover();
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
            if (!masterFailed.get() && !recoveryInProgress.get()) {
                log.info("原主库已恢复，开始恢复流程...");
                recoverOriginalMaster();
            }
        }
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
