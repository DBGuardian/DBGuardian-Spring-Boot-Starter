package com.dbguardian.test;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DBGuardian 测试项目 - Java8 + Spring Boot 2.7 + MyBatis
 */
@SpringBootApplication
@MapperScan("com.dbguardian.test.mapper")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}