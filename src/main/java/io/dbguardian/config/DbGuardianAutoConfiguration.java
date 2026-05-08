package io.dbguardian.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import io.dbguardian.aspect.DbGuardianDataSourceAspect;
import io.dbguardian.coordination.DatasourceCoordinationService;
import io.dbguardian.coordination.DatasourceStatusListener;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;

/**
 * DBGuardian 自动配置类
 * 
 * 基于 business-workflow-erp-java 项目的配置方式
 */
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
@Import(DbGuardianDataSourceConfig.class)
@AutoConfigureAfter({DbGuardianDataSourceConfig.class})
public class DbGuardianAutoConfiguration {

    /**
     * 数据源协调服务（用于多实例分布式部署）
     * 通过 FactoryBean 方式创建，确保 Spring 注入生效
     */
    @Bean
    @ConditionalOnMissingBean
    public DatasourceCoordinationService datasourceCoordinationService(
            @Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
            @Value("${spring.application.name:dbguardian}") String applicationName) {
        
        DatasourceCoordinationService service = new DatasourceCoordinationService();
        // 手动注入依赖
        service.setRedisTemplate(redisTemplate);
        service.setApplicationName(applicationName);
        return service;
    }

    /**
     * 显式创建 SqlSessionFactory，确保使用 DBGuardian 的路由数据源
     * 使用 @DependsOn 确保数据源 Bean 在 SqlSessionFactory 之前创建
     */
    @Bean("sqlSessionFactory")
    @DependsOn({"dbguardianMasterDataSource", "dbguardianSlaveDataSource", "dbguardianRoutingDataSource"})
    @ConditionalOnMissingBean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) {
        try {
            SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
            sessionFactory.setDataSource(dataSource);
            
            // 设置 MyBatis 配置文件路径
            sessionFactory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml"));
            
            // 设置类型别名包
            sessionFactory.setTypeAliasesPackage("com.erp.entity");
            
            org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setCacheEnabled(true);
            configuration.setLazyLoadingEnabled(true);
            configuration.setAggressiveLazyLoading(false);
            sessionFactory.setConfiguration(configuration);
            
            return sessionFactory.getObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SqlSessionFactory: " + e.getMessage(), e);
        }
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
     * MyBatis-Plus 配置
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    /**
     * 状态监听器（仅当协调服务存在时）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(DatasourceCoordinationService.class)
    public DatasourceStatusListener datasourceStatusListener() {
        return new DatasourceStatusListener();
    }
}
