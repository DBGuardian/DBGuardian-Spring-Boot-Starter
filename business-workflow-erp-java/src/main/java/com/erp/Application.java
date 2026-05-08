package com.erp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 危险废物处理企业ERP管理系统启动类
 *
 * @author ERP System
 * @date 2025-01-01
 */
@SpringBootApplication
@MapperScan("com.erp.mapper")
@EnableRabbit
@EnableScheduling
@EnableTransactionManagement
public class  Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}






















































































