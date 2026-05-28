package com.dbguardian.test.mapper;

import com.dbguardian.test.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单Mapper
 *
 * 技术栈: [JAVA8] [SPRING_BOOT_27] [MYBATIS] [MYSQL]
 */
@Mapper
public interface OrderMapper {

    @Select("SELECT id, order_no, user_id, amount, status, create_time, update_time FROM t_order ORDER BY id DESC")
    List<Order> selectAll();
}