package com.dbguardian.test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测试订单实体
 *
 * 技术栈: [JAVA8] [SPRING_BOOT_27] [MYBATIS_PLUS] [MYSQL]
 */
@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal amount;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
