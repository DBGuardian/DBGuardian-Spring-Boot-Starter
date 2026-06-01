package com.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 */
@Tag(name = "订单管理", description = "订单相关接口")
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private com.test.service.OrderService orderService;

    @Operation(summary = "获取订单列表", description = "获取所有订单信息（读操作，应路由到从库）")
    @GetMapping("/list")
    public List<com.test.entity.Order> listOrders() {
        return orderService.list();
    }

    @Operation(summary = "获取订单详情", description = "根据ID获取订单信息（读操作，应路由到从库）")
    @GetMapping("/{id}")
    public com.test.entity.Order getOrder(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @Operation(summary = "获取用户的订单", description = "获取指定用户的所有订单（读操作，应路由到从库）")
    @GetMapping("/user/{userId}")
    public List<com.test.entity.Order> getOrdersByUser(@PathVariable Long userId) {
        return orderService.getOrdersByUserId(userId);
    }

    @Operation(summary = "创建订单", description = "创建新订单（写操作，应路由到主库）")
    @PostMapping
    public com.test.entity.Order createOrder(@RequestBody com.test.entity.Order order) {
        return orderService.saveOrder(order);
    }

    @Operation(summary = "更新订单", description = "更新订单信息（写操作，应路由到主库）")
    @PutMapping("/{id}")
    public boolean updateOrder(@PathVariable Long id, @RequestBody com.test.entity.Order order) {
        order.setId(id);
        return orderService.updateById(order);
    }

    @Operation(summary = "删除订单", description = "删除订单（写操作，应路由到主库）")
    @DeleteMapping("/{id}")
    public boolean deleteOrder(@PathVariable Long id) {
        return orderService.removeById(id);
    }
}
