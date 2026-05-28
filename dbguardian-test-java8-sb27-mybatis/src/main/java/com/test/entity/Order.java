package com.dbguardian.test.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测试订单实体
 *
 * 技术栈: [JAVA8] [SPRING_BOOT_27] [MYBATIS] [MYSQL]
 */
@Data
public class Order {

    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal amount;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}