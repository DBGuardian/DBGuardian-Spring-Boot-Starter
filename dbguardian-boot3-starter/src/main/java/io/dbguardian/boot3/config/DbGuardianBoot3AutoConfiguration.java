package io.dbguardian.boot3.config;

import io.dbguardian.core.CapabilityRegistry;
import io.dbguardian.core.FailoverOrchestrator;
import io.dbguardian.core.RoutingEngine;
import io.dbguardian.core.TopologyRegistry;
import io.dbguardian.core.registry.DataSourceRegistry;
import io.dbguardian.core.routing.DefaultRoutingPolicy;
import io.dbguardian.spring.aspect.MyBatisPlusDataSourceAdvisor;
import io.dbguardian.spring.aspect.MyBatisDataSourceAdvisor;
import io.dbguardian.spring.coordination.DatasourceCoordinationService;
import io.dbguardian.spring.failover.DataSourceHealthChecker;
import io.dbguardian.spring.failover.FailoverController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

@AutoConfiguration
@EnableConfigurationProperties(DbGuardianProperties.class)
public class DbGuardianBoot3AutoConfiguration {

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

    @Bean
    @ConditionalOnMissingBean
    public DataSourceRegistry dataSourceRegistry() {
        return new DataSourceRegistry();
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
}
