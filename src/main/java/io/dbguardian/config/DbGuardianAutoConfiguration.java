package io.dbguardian.config;

import com.zaxxer.hikari.HikariDataSource;
import io.dbguardian.aspect.DbGuardianDataSourceAspect;
import io.dbguardian.coordination.DatasourceCoordinationService;
import io.dbguardian.coordination.DatasourceStatusListener;
import io.dbguardian.enums.DataSourceStatus;
import io.dbguardian.enums.DataSourceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * DBGuardian 自动配置类
 *
 * 职责：负责读写分离数据源配置，同时也提供 MyBatis 兼容的数据源 Bean
 * 组件内部排除 Spring Boot 默认数据源自动配置
 */
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
@AutoConfigureBefore(name = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
public class DbGuardianAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DbGuardianAutoConfiguration.class);

    // ==================== 数据源配置属性 ====================

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

    // ==================== 数据源 Bean 定义 ====================

    /**
     * 主数据源（写操作使用）
     */
    @Bean("dbguardianMasterDataSource")
    public DataSource masterDataSource() {
        return createDataSource(masterUrl, masterUsername, masterPassword, driverClassName, masterPoolSize, masterMinIdle, "dbguardian-master-pool");
    }

    /**
     * 从数据源（读操作使用）
     */
    @Bean("dbguardianSlaveDataSource")
    public DataSource slaveDataSource() {
        return createDataSource(slaveUrl, slaveUsername, slavePassword, slaveDriverClassName, slavePoolSize, slaveMinIdle, "dbguardian-slave-pool");
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

    /**
     * 动态数据源（根据上下文切换主从）
     * Bean 名称使用 "dataSource"，与 MyBatis 自动配置兼容
     */
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

    // ==================== 其他 Bean 定义 ====================

    /**
     * 数据源协调服务（用于多实例分布式部署）
     */
    @Bean
    @ConditionalOnMissingBean
    public DatasourceCoordinationService datasourceCoordinationService(
            @Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
            @Value("${spring.application.name:dbguardian}") String applicationName) {

        DatasourceCoordinationService service = new DatasourceCoordinationService();
        service.setRedisTemplate(redisTemplate);
        service.setApplicationName(applicationName);
        return service;
    }

    /**
     * 读写分离切面
     */
    @Bean
    @ConditionalOnMissingBean
    public DbGuardianDataSourceAspect dbGuardianDataSourceAspect() {
        return new DbGuardianDataSourceAspect();
    }

    /**
     * 状态监听器（仅当协调服务存在时）
     */
    @Bean
    @ConditionalOnMissingBean
    public DatasourceStatusListener datasourceStatusListener() {
        return new DatasourceStatusListener();
    }

    // ==================== 内部类 ====================

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

    // ==================== 公开方法（供 AOP 切面使用） ====================

    /**
     * 获取当前数据源状态（兼容旧接口）
     */
    public DataSourceStatus getCurrentStatus() {
        return DataSourceStatus.MASTER_ACTIVE;
    }
}
