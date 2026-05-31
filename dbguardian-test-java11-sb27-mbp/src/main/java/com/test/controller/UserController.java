package com.test.controller;

import com.test.entity.User;
import com.test.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器 - 用于测试读写分离
 */
@Api(tags = "用户管理 - 读写分离测试")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @ApiOperation("创建用户（写操作，应路由到主库）")
    @PostMapping
    public Map<String, Object> createUser(@RequestBody User user) {
        User savedUser = userService.saveUser(user);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", savedUser);
        result.put("message", "用户创建成功（已路由到主库）");
        return result;
    }

    @ApiOperation("根据ID查询用户（读操作，应路由到从库）")
    @GetMapping("/{id}")
    public Map<String, Object> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", user != null);
        result.put("data", user);
        result.put("message", user != null ? "查询成功（已路由到从库）" : "用户不存在");
        return result;
    }

    @ApiOperation("获取所有用户（读操作，应路由到从库）")
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
