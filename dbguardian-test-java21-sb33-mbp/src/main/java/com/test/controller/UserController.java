package com.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@Tag(name = "用户管理", description = "用户相关接口")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private com.test.service.UserService userService;

    @Operation(summary = "获取用户列表", description = "获取所有用户信息（读操作，应路由到从库）")
    @GetMapping("/list")
    public List<com.test.entity.User> listUsers() {
        return userService.listUsers();
    }

    @Operation(summary = "获取用户详情", description = "根据ID获取用户信息（读操作，应路由到从库）")
    @GetMapping("/{id}")
    public com.test.entity.User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @Operation(summary = "创建用户", description = "创建新用户（写操作，应路由到主库）")
    @PostMapping
    public com.test.entity.User createUser(@RequestBody com.test.entity.User user) {
        return userService.saveUser(user);
    }

    @Operation(summary = "更新用户", description = "更新用户信息（写操作，应路由到主库）")
    @PutMapping("/{id}")
    public boolean updateUser(@PathVariable Long id, @RequestBody com.test.entity.User user) {
        user.setId(id);
        return userService.updateById(user);
    }

    @Operation(summary = "删除用户", description = "删除用户（写操作，应路由到主库）")
    @DeleteMapping("/{id}")
    public boolean deleteUser(@PathVariable Long id) {
        return userService.removeById(id);
    }
}
