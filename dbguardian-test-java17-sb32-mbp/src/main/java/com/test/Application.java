package com.test;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

/**
 * DBGuardian 测试项目 - Java17 + Spring Boot 3.2 + MyBatis-Plus
 * 
 * 注意：只排除 DataSource 自动配置，让 DBGuardian 提供数据源
 * MyBatis-Plus 自动配置会使用 DBGuardian 提供的数据源
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class
})
@MapperScan("com.test.mapper")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
