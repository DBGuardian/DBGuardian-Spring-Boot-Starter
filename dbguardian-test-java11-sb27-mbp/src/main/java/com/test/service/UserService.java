package com.dbguardian.test.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dbguardian.test.entity.User;

import java.util.List;

/**
 * 用户服务接口
 *
 * 技术栈: [JAVA8] [SPRING_BOOT_27] [MYBATIS_PLUS] [MYSQL]
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
    List<User> listUsers();
}
