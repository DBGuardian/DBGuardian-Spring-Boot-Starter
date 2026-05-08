package com.erp.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成工具类
 * 用于生成BCrypt加密后的密码，用于数据库测试数据
 * 
 * 使用方法：
 * 1. 直接运行main方法
 * 2. 或者在其他地方调用generateEncodedPassword方法
 *
 * @author ERP System
 * @date 2025-01-01
 */
public class PasswordGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 加密密码 "pwd123"
        String encodedPassword = encoder.encode("pwd123");
        
        System.out.println("========================================");
        System.out.println("密码加密工具");
        System.out.println("========================================");
        System.out.println("原始密码: pwd123");
        System.out.println("加密后密码: " + encodedPassword);
        System.out.println();
        
        // 验证密码
        boolean matches = encoder.matches("pwd123", encodedPassword);
        System.out.println("密码验证结果: " + (matches ? "✓ 验证通过" : "✗ 验证失败"));
        System.out.println();
        
        // 生成多个测试密码（每次加密结果不同，但都能验证通过）
        System.out.println("=== 生成多个加密密码（用于测试数据） ===");
        for (int i = 1; i <= 5; i++) {
            String password = "pwd123";
            String encoded = encoder.encode(password);
            System.out.println("用户" + i + " - 原始密码: " + password);
            System.out.println("        加密密码: " + encoded);
            System.out.println();
        }
        
        System.out.println("========================================");
        System.out.println("提示：将加密后的密码更新到数据库EMPLOYEE表的密码字段");
        System.out.println("========================================");
    }

    /**
     * 生成加密密码
     *
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String generateEncodedPassword(String rawPassword) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(rawPassword);
    }

    /**
     * 验证密码
     *
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean verifyPassword(String rawPassword, String encodedPassword) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(rawPassword, encodedPassword);
    }
}



