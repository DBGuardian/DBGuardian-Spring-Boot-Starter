package io.dbguardian.boot3.config;

import com.zaxxer.hikari.HikariDataSource;
import io.dbguardian.core.CapabilityRegistry;
import io.dbguardian.core.FailoverOrchestrator;
import io.dbguardian.core.RoutingEngine;
import io.dbguardian.core.TopologyRegistry;
import io.dbguardian.core.datasource.DataSourceWrapper;
import io.dbguardian.core.registry.DataSourceRegistry;
import io.dbguardian.core.routing.DefaultRoutingPolicy;
import io.dbguardian.spring.DbGuardianRoutingDataSource;
import io.dbguardian.spring.aspect.MyBatisPlusDataSourceAdvisor;
import io.dbguardian.spring.aspect.MyBatisDataSourceAdvisor;
import io.dbguardian.spring.coordination.DatasourceCoordinationService;
import io.dbguardian.spring.failover.DataSourceHealthChecker;
import io.dbguardian.spring.failover.FailoverController;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

@org.springframework.boot.autoconfigure.AutoConfiguration
@EnableScheduling
@AutoConfigureBefore(
        name = {
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
                "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
        }
)
@EnableConfigurationProperties({DbGuardianProperties.class, SpringDataSourceProperties.class})
public class DbGuardianBoot3AutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DbGuardianBoot3AutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public CapabilityRegistry capabilityRegistry() {
        CapabilityRegistry registry = new CapabilityRegistry();
        registry.registerRoutingPolicy(new DefaultRoutingPolicy());
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public TopologyRegistry topologyRegistry(DbGuardianProperties properties) {
        TopologyRegistry registry = new TopologyRegistry();
        registry.replace(properties.toClusterModel());
        return registry;
    }

    @Bean(name = "dbguardianMasterDataSource")
    @ConditionalOnMissingBean(name = "dbguardianMasterDataSource")
    @ConditionalOnProperty(prefix = "spring.datasource.master", name = "url")
    public DataSource masterDataSource(SpringDataSourceProperties properties) {
        return createDataSource(properties.getMaster(), "dbguardian-master-pool");
    }

    @Bean(name = "dbguardianSlaveDataSource")
    @ConditionalOnMissingBean(name = "dbguardianSlaveDataSource")
    @ConditionalOnProperty(prefix = "spring.datasource.slave", name = "url")
    public DataSource slaveDataSource(SpringDataSourceProperties properties) {
        return createDataSource(properties.getSlave(), "dbguardian-slave-pool");
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSourceRegistry dataSourceRegistry(
            SpringDataSourceProperties properties,
            @Autowired(required = false) @Qualifier("dbguardianMasterDataSource") DataSource masterDataSource,
            @Autowired(required = false) @Qualifier("dbguardianSlaveDataSource") DataSource slaveDataSource) {
        DataSourceRegistry registry = new DataSourceRegistry();
        if (masterDataSource != null) {
            registry.registerMaster("master", new DataSourceWrapper(
                    "master",
                    properties.getMaster().getUrl(),
                    properties.getMaster().getUsername(),
                    properties.getMaster().getPassword(),
                    properties.getMaster().getDriverClassName(),
                    100,
                    100,
                    true,
                    masterDataSource));
        }
        if (slaveDataSource != null) {
            registry.registerSlave("slave", new DataSourceWrapper(
                    "slave",
                    properties.getSlave().getUrl(),
                    properties.getSlave().getUsername(),
                    properties.getSlave().getPassword(),
                    properties.getSlave().getDriverClassName(),
                    100,
                    100,
                    false,
                    slaveDataSource));
        }
        return registry;
    }

    @Bean(name = "dataSource")
    @Primary
    @ConditionalOnMissingBean(name = "dataSource")
    @ConditionalOnBean(name = "dbguardianMasterDataSource")
    public DataSource routingDataSource(
            @Qualifier("dbguardianMasterDataSource") DataSource masterDataSource,
            @Autowired(required = false) @Qualifier("dbguardianSlaveDataSource") DataSource slaveDataSource) {
        Map<Object, Object> targetDataSources = new LinkedHashMap<Object, Object>();
        targetDataSources.put(DbGuardianRoutingDataSource.MASTER_KEY, masterDataSource);
        targetDataSources.put(DbGuardianRoutingDataSource.SLAVE_KEY, slaveDataSource != null ? slaveDataSource : masterDataSource);

        DbGuardianRoutingDataSource routingDataSource = new DbGuardianRoutingDataSource();
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.afterPropertiesSet();
        log.info("DBGuardian 读写分离数据源已初始化");
        return routingDataSource;
    }

    @Bean
    @Primary
    @ConditionalOnBean(name = "dataSource")
    @ConditionalOnMissingBean
    public DataSourceTransactionManager dataSourceTransactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public RoutingEngine routingEngine(CapabilityRegistry capabilityRegistry) {
        return new RoutingEngine(capabilityRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public FailoverOrchestrator failoverOrchestrator(CapabilityRegistry capabilityRegistry) {
        return new FailoverOrchestrator(capabilityRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatasourceCoordinationService datasourceCoordinationService(
            @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        DatasourceCoordinationService service = new DatasourceCoordinationService();
        if (redisTemplate != null) {
            service.setRedisTemplate(redisTemplate);
        }
        return service;
    }

    @Bean
    @ConditionalOnMissingBean
    public FailoverController failoverController(
            DataSourceRegistry dataSourceRegistry,
            @Autowired(required = false) DatasourceCoordinationService coordinationService) {
        return new FailoverController(dataSourceRegistry, coordinationService);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSourceHealthChecker dataSourceHealthChecker(
            DataSourceRegistry dataSourceRegistry,
            FailoverController failoverController) {
        return new DataSourceHealthChecker(dataSourceRegistry, failoverController);
    }

    /**
     * MyBatis-Plus Mapper 拦截切面
     *
     * <p>仅在 classpath 中存在 MyBatis-Plus 时注册
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.baomidou.mybatisplus.core.mapper.BaseMapper")
    public MyBatisPlusDataSourceAdvisor mybatisPlusDataSourceAdvisor() {
        return new MyBatisPlusDataSourceAdvisor();
    }

    /**
     * MyBatis Mapper 拦截切面
     *
     * <p>仅在 classpath 中存在 MyBatis (非 MyBatis-Plus) 时注册
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.apache.ibatis.session.SqlSessionFactory")
    public MyBatisDataSourceAdvisor mybatisDataSourceAdvisor() {
        return new MyBatisDataSourceAdvisor();
    }

    @PostConstruct
    public void logAutoConfigurationLoaded() {
        log.info("DBGuardian Boot3 自动配置已加载");
    }

    private DataSource createDataSource(SpringDataSourceProperties.NodeProperties node, String defaultPoolName) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(node.getUrl());
        dataSource.setUsername(node.getUsername());
        dataSource.setPassword(node.getPassword());
        dataSource.setDriverClassName(node.getDriverClassName());
        dataSource.setMaximumPoolSize(node.getHikari().getMaximumPoolSize());
        dataSource.setMinimumIdle(node.getHikari().getMinimumIdle());
        dataSource.setConnectionTimeout(node.getHikari().getConnectionTimeout());
        dataSource.setIdleTimeout(node.getHikari().getIdleTimeout());
        dataSource.setMaxLifetime(node.getHikari().getMaxLifetime());
        dataSource.setPoolName(node.getHikari().getPoolName() != null ? node.getHikari().getPoolName() : defaultPoolName);
        dataSource.setConnectionTestQuery("SELECT 1");
        return dataSource;
    }
}
