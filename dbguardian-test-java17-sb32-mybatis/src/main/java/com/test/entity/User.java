package com.test.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试用户实体
 * 
 * 技术栈: [JAVA17] [SPRING_BOOT_32] [MYBATIS] [MYSQL]
 */
@Data
public class User {

    private Long id;

    private String username;

    private String email;

    private String phone;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
