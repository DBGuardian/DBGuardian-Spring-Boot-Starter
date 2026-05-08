package com.erp.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密测试类
 * 用于生成BCrypt加密后的密码，用于数据库测试数据
 *
 * @author ERP System
 * @date 2025-01-01
 */
public class PasswordEncoderTest {

    @Test
    public void encodePassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 加密密码 "pwd123"
        String encodedPassword = encoder.encode("pwd123");
        
        System.out.println("原始密码: pwd123");
        System.out.println("加密后密码: " + encodedPassword);
        System.out.println();
        
        // 验证密码
        boolean matches = encoder.matches("pwd123", encodedPassword);
        System.out.println("密码验证结果: " + matches);
        
        // 生成多个测试密码
        System.out.println("\n=== 生成多个加密密码 ===");
        for (int i = 1; i <= 5; i++) {
            String password = "pwd123";
            String encoded = encoder.encode(password);
            System.out.println("用户" + i + " - 原始密码: " + password + " -> 加密密码: " + encoded);
        }
    }
}

