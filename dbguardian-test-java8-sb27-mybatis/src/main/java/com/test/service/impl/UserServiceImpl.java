package com.dbguardian.test.service.impl;

import com.dbguardian.test.entity.User;
import com.dbguardian.test.mapper.UserMapper;
import com.dbguardian.test.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务实现
 *
 * 技术栈: [JAVA8] [SPRING_BOOT_27] [MYBATIS] [MYSQL]
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User saveUser(User user) {
        if (user.getCreateTime() == null) {
            user.setCreateTime(LocalDateTime.now());
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public List<User> listUsers() {
        return userMapper.selectAll();
    }
}