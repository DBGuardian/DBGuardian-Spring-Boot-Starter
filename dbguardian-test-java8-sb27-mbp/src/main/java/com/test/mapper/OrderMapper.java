package com.dbguardian.test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dbguardian.test.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单Mapper
 *
 * 技术栈: [JAVA8] [SPRING_BOOT_27] [MYBATIS_PLUS] [MYSQL]
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
