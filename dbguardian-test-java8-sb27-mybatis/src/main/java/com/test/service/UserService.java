package com.dbguardian.test.service;

import com.dbguardian.test.entity.User;

import java.util.List;

/**
 * 用户服务接口
 *
 * 技术栈: [JAVA8] [SPRING_BOOT_27] [MYBATIS] [MYSQL]
 */
public interface UserService {

    User saveUser(User user);

    User getUserById(Long id);

    List<User> listUsers();
}