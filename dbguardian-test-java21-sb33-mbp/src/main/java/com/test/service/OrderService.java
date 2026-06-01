package com.test.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.test.entity.Order;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService extends IService<Order> {

    /**
     * 保存订单（写操作，应路由到主库）
     */
    Order saveOrder(Order order);

    /**
     * 根据ID查询订单（读操作，应路由到从库）
     */
    Order getOrderById(Long id);

    /**
     * 获取用户的所有订单（读操作，应路由到从库）
     */
    List<Order> getOrdersByUserId(Long userId);
}
