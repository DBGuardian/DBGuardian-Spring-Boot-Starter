package com.dbguardian.test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dbguardian.test.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper
 *
 * 技术栈: [JAVA8] [SPRING_BOOT_27] [MYBATIS_PLUS] [MYSQL]
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
