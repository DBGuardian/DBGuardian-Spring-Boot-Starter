package com.test.controller;

import com.test.entity.User;
import com.test.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器 - 用于测试读写分离
 * 
 * 技术栈: [JAVA17] [SPRING_BOOT_32] [MYBATIS] [MYSQL]
 */
@Tag(name = "用户管理", description = "用户相关接口 - 读写分离测试")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "创建用户", description = "创建新用户（写操作，应路由到主库）")
    @PostMapping
    public Map<String, Object> createUser(@RequestBody User user) {
        User savedUser = userService.saveUser(user);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", savedUser);
        result.put("message", "用户创建成功（已路由到主库）");
        return result;
    }

    @Operation(summary = "获取用户详情", description = "根据ID获取用户信息（读操作，应路由到从库）")
    @GetMapping("/{id}")
    public Map<String, Object> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", user != null);
        result.put("data", user);
        result.put("message", user != null ? "查询成功（已路由到从库）" : "用户不存在");
        return result;
    }

    @Operation(summary = "获取用户列表", description = "获取所有用户信息（读操作，应路由到从库）")
    @GetMapping("/list")
    public Map<String, Object> listUsers() {
        List<User> users = userService.listUsers();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", users);
        result.put("count", users.size());
        result.put("message", "查询成功（已路由到从库）");
        return result;
    }
}
