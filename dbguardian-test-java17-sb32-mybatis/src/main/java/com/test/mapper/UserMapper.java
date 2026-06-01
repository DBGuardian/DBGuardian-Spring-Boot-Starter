package com.test.mapper;

import com.test.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户Mapper
 * 
 * 技术栈: [JAVA17] [SPRING_BOOT_32] [MYBATIS] [MYSQL]
 */
@Mapper
public interface UserMapper {

    @Insert("INSERT INTO t_user (username, email, phone, status, create_time, update_time) VALUES (#{username}, #{email}, #{phone}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT id, username, email, phone, status, create_time, update_time FROM t_user WHERE id = #{id}")
    User selectById(Long id);

    @Select("SELECT id, username, email, phone, status, create_time, update_time FROM t_user ORDER BY id DESC")
    List<User> selectAll();
}
