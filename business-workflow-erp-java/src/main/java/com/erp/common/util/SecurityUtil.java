package com.erp.common.util;

import com.erp.security.user.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 安全工具类
 *
 * @author ERP System
 * @date 2025-01-01
 */
public class SecurityUtil {

    /**
     * 获取当前登录用户信息
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        return null;
    }

    /**
     * 获取当前登录员工姓名
     */
    public static String getEmployeeName() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return "系统";
        }

        Object principal = authentication.getPrincipal();
        // 优先处理项目自定义的 UserDetailsImpl，包含 Employee 信息
        if (principal instanceof com.erp.security.user.UserDetailsImpl) {
            com.erp.security.user.UserDetailsImpl userDetails = (com.erp.security.user.UserDetailsImpl) principal;
            if (userDetails.getEmployee() != null && userDetails.getEmployee().getEmployeeName() != null) {
                return userDetails.getEmployee().getEmployeeName();
            }
        }

        // 回退到标准 UserDetails 的 username（可能是登录账号），以避免返回 null
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return username != null ? username : "系统";
        }

        return "系统";
    }

    /**
     * 获取当前登录用户ID
     */
    public static Integer getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) principal).getEmployeeId();
        }
        if (principal instanceof UserDetails) {
            return null;
        }
        return null;
    }

    /**
     * 获取当前登录员工ID（与getCurrentUserId相同，提供语义化别名）
     */
    public static Integer getCurrentEmployeeId() {
        return getCurrentUserId();
    }
}
