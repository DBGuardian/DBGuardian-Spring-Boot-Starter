package com.test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.test.entity.Order;
import com.test.mapper.OrderMapper;
import com.test.service.OrderService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单服务实现
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Override
    public Order saveOrder(Order order) {
        if (order.getCreateTime() == null) {
            order.setCreateTime(LocalDateTime.now());
        }
        order.setUpdateTime(LocalDateTime.now());
        this.save(order);
        return order;
    }

    @Override
    public Order getOrderById(Long id) {
        return this.getById(id);
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        return this.list(wrapper);
    }
}
