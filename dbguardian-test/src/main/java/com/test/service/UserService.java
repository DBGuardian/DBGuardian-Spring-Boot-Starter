package com.test.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.test.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 保存用户（写操作，应路由到主库）
     */
    User saveUser(User user);

    /**
     * 根据ID查询用户（读操作，应路由到从库）
     */
    User getUserById(Long id);

    /**
     * 获取所有用户列表（读操作，应路由到从库）
     */
    java.util.List<User> listUsers();
}
