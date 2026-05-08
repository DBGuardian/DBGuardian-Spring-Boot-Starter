package com.erp.service.auth;

import com.erp.controller.auth.dto.LoginRequest;
import com.erp.controller.auth.dto.LoginResponse;
import com.erp.controller.auth.dto.PagePermissionConfigResponse;
import com.erp.controller.auth.dto.ResetPasswordSendCodeRequest;
import com.erp.controller.auth.dto.ResetPasswordSendCodeResponse;
import com.erp.controller.auth.dto.ResetPasswordRequest;
import com.erp.controller.auth.dto.ResetPasswordVerifyCodeRequest;
import com.erp.controller.auth.dto.ResetPasswordVerifyCodeResponse;
import com.erp.controller.auth.dto.ResetPasswordVerifyRequest;
import com.erp.controller.auth.dto.ResetPasswordVerifyResponse;
import com.erp.controller.auth.dto.TokenRefreshRequest;
import com.erp.controller.auth.dto.TokenRefreshResponse;

import javax.servlet.http.HttpServletRequest;

/**
 * 认证服务接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @param httpRequest HTTP请求对象，用于获取客户端IP
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest);

    /**
     * 用户登出
     *
     * @param token Token
     */
    void logout(String token);

    /**
     * 刷新Token
     *
     * @param request 刷新Token请求
     * @return 刷新Token响应
     */
    TokenRefreshResponse refreshToken(TokenRefreshRequest request);

    /**
     * 验证Token
     *
     * @param token    Token
     * @param username 用户名
     * @return 是否有效
     */
    boolean validateToken(String token, String username);

    /**
     * 忘记密码第一步，校验账号与手机号是否匹配
     *
     * @param request 校验请求
     * @return 校验通过后返回员工信息
     */
    ResetPasswordVerifyResponse verifyAccountPhone(ResetPasswordVerifyRequest request);

    /**
     * 忘记密码第二步，发送邮箱验证码
     *
     * @param request 发送验证码请求
     * @return 发送结果
     */
    ResetPasswordSendCodeResponse sendResetEmailCode(ResetPasswordSendCodeRequest request);

    /**
     * 忘记密码第二步，校验邮箱验证码
     *
     * @param request 校验请求
     * @return 校验结果
     */
    ResetPasswordVerifyCodeResponse verifyResetEmailCode(ResetPasswordVerifyCodeRequest request);

    /**
     * 忘记密码第三步，重置密码
     *
     * @param request 重置密码请求
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * 获取当前登录用户的权限列表（包含权限类型ID）
     *
     * @return 权限对象列表
     */
    java.util.List<com.erp.controller.system.dto.PermissionResponse> getMyPermissions();

    /**
     * 获取当前登录用户的页面权限配置
     *
     * @param pageCode 页面权限编码
     * @return 页面权限配置
     */
    PagePermissionConfigResponse getMyPagePermission(String pageCode);

    /**
     * 判断指定员工是否为管理员（包含：管理员 / 系统管理员 / ADMIN / super_admin / 超级管理员）
     *
     * @param employeeId 员工ID
     * @return 是否为管理员
     */
    boolean isAdmin(Integer employeeId);

    /**
     * 获取当前用户对指定页面的数据查看范围
     *
     * @param pageCode 页面权限编码，如 "档案管理:客户档案:页面"
     * @return 数据查看范围（SELF=仅查看自己, ALL=查看全部），默认返回SELF
     */
    String getViewScope(String pageCode);
}
