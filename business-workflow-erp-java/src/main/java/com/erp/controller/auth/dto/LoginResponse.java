package com.erp.controller.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 登录响应DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 访问Token
     */
    private String token;

    /**
     * 刷新Token
     */
    private String refreshToken;

    /**
     * 用户信息
     */
    private UserInfo userInfo;

    /**
     * 用户信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 登录账号
         */
        private String loginAccount;

        /**
         * 员工ID
         */
        private Integer employeeId;

        /**
         * 员工姓名
         */
        private String employeeName;

        /**
         * 部门
         */
        private String department;

        /**
         * 岗位
         */
        private String jobTitle;

        /**
         * 联系方式
         */
        private String phone;

        /**
         * 邮箱
         */
        private String email;

        /**
         * 员工状态
         */
        private String employeeStatus;

        /**
         * 角色列表
         */
        private List<String> roles;

        /**
         * 权限列表
         */
        private List<String> permissions;
    }
}





