package com.erp.controller.auth;

import com.erp.common.result.Result;
import com.erp.controller.auth.dto.LoginRequest;
import com.erp.controller.auth.dto.LoginResponse;
import com.erp.controller.auth.dto.ResetPasswordRequest;
import com.erp.controller.auth.dto.ResetPasswordSendCodeRequest;
import com.erp.controller.auth.dto.ResetPasswordSendCodeResponse;
import com.erp.controller.auth.dto.ResetPasswordVerifyCodeRequest;
import com.erp.controller.auth.dto.ResetPasswordVerifyCodeResponse;
import com.erp.controller.auth.dto.ResetPasswordVerifyRequest;
import com.erp.controller.auth.dto.ResetPasswordVerifyResponse;
import com.erp.controller.auth.dto.TokenRefreshRequest;
import com.erp.controller.auth.dto.TokenRefreshResponse;
import com.erp.common.util.SecurityUtil;
import com.erp.service.auth.AuthService;
import com.erp.service.system.ILogRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 认证控制器
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Api(tags = "认证管理")
@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 用户登录
     */
    @ApiOperation("用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("用户登录请求: {}", request.getUsername());
        long startTime = System.currentTimeMillis();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        String errorMsg = null;
        Integer userId = null;
        
        try {
            LoginResponse response = authService.login(request, httpRequest);
            userId = response.getUserInfo() != null ? response.getUserInfo().getEmployeeId() : null;
            long durationMs = System.currentTimeMillis() - startTime;
            // 记录登录日志
            logRecordService.recordLoginLog(request.getUsername(), userId, ipAddress, true, null, durationMs);
            return Result.success("登录成功", response);
        } catch (Exception e) {
            errorMsg = e.getMessage();
            long durationMs = System.currentTimeMillis() - startTime;
            // 记录登录失败日志
            logRecordService.recordLoginLog(request.getUsername(), null, ipAddress, false, errorMsg, durationMs);
            throw e;
        }
    }

    /**
     * 用户登出
     */
    @ApiOperation("用户登出")
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        log.info("用户登出请求，Token是否存在：{}", StringUtils.hasText(token));
        
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(request);
        String username = SecurityUtil.getCurrentUsername();
        
        try {
            authService.logout(token);
            // 记录登出日志
            logRecordService.recordOperationLog("认证管理", "退出登录", 
                    username != null ? "用户" + username + "退出登录" : "退出登录", 
                    userId, ipAddress, true, null);
            return Result.success("登出成功");
        } catch (Exception e) {
            logRecordService.recordOperationLog("认证管理", "退出登录", 
                    username != null ? "用户" + username + "退出登录" : "退出登录", 
                    userId, ipAddress, false, e.getMessage());
            throw e;
        }
    }

    /**
     * 刷新Token
     */
    @ApiOperation("刷新Token")
    @PostMapping("/refresh")
    public Result<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request, HttpServletRequest httpRequest) {
        log.info("刷新Token请求");
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        String username = SecurityUtil.getCurrentUsername();
        
        try {
            TokenRefreshResponse response = authService.refreshToken(request);
            // 记录操作日志
            logRecordService.recordOperationLog("认证管理", "刷新令牌", 
                    username != null ? "用户" + username + "刷新Token" : "刷新Token", 
                    userId, ipAddress, true, null);
            return Result.success("刷新Token成功", response);
        } catch (Exception e) {
            logRecordService.recordOperationLog("认证管理", "刷新令牌", 
                    username != null ? "用户" + username + "刷新Token" : "刷新Token", 
                    userId, ipAddress, false, e.getMessage());
            throw e;
        }
    }

    /**
     * 查询当前用户登录状态
     * 安全修复：移除username参数，防止用户枚举攻击
     * 只能查询当前认证用户的登录状态
     */
    @ApiOperation("查询当前用户登录状态")
    @GetMapping("/status")
    public Result<LoginStatusResponse> getLoginStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = authentication != null 
                && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getName());
        
        LoginStatusResponse response = new LoginStatusResponse();
        if (loggedIn) {
            response.setUsername(authentication.getName());
        } else {
            response.setUsername(null);
        }
        response.setLoggedIn(loggedIn);
        
        return Result.success(loggedIn ? "已登录" : "未登录", response);
    }

    /**
     * 获取当前登录用户的权限列表（包含权限类型ID）
     * 返回当前用户通过角色继承和员工层覆盖后的所有权限对象
     */
    @ApiOperation("获取当前登录用户的权限列表")
    @GetMapping("/me/permissions")
    public Result<java.util.List<com.erp.controller.system.dto.PermissionResponse>> getMyPermissions(HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("获取当前用户权限列表：userId={}", userId);
            java.util.List<com.erp.controller.system.dto.PermissionResponse> permissions = authService.getMyPermissions();
            logRecordService.recordOperationLog("认证管理", "查询", 
                    "获取当前用户权限列表", userId, ipAddress, true, null);
            return Result.success("获取权限成功", permissions);
        } catch (Exception e) {
            log.error("获取当前用户权限列表失败：userId={}", userId, e);
            logRecordService.recordOperationLog("认证管理", "查询", 
                    "获取当前用户权限列表", userId, ipAddress, false, e.getMessage());
            throw e;
        }
    }

    /**
     * 获取当前登录用户的页面权限配置
     * 返回指定页面的权限配置（canView, canEdit, viewScope, operateScope）
     */
    @ApiOperation("获取当前登录用户的页面权限配置")
    @GetMapping("/me/page-permission")
    public Result<com.erp.controller.auth.dto.PagePermissionConfigResponse> getMyPagePermission(
            @RequestParam("pageCode") String pageCode, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("获取当前用户页面权限配置：userId={}, pageCode={}", userId, pageCode);
            com.erp.controller.auth.dto.PagePermissionConfigResponse permission = authService.getMyPagePermission(pageCode);
            logRecordService.recordOperationLog("认证管理", "查询", 
                    "获取页面权限配置：" + pageCode, userId, ipAddress, true, null);
            return Result.success("获取页面权限配置成功", permission);
        } catch (Exception e) {
            log.error("获取当前用户页面权限配置失败：userId={}, pageCode={}", userId, pageCode, e);
            logRecordService.recordOperationLog("认证管理", "查询", 
                    "获取页面权限配置：" + pageCode, userId, ipAddress, false, e.getMessage());
            throw e;
        }
    }

    /**
     * 忘记密码第一步：校验账号与手机号
     */
    @ApiOperation("校验账号与手机号")
    @PostMapping("/reset/verify-account")
    public Result<ResetPasswordVerifyResponse> verifyAccount(@Valid @RequestBody ResetPasswordVerifyRequest request, HttpServletRequest httpRequest) {
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            ResetPasswordVerifyResponse response = authService.verifyAccountPhone(request);
            // 记录操作日志
            logRecordService.recordOperationLog("认证管理", "找回密码", 
                    "校验账号与手机号：" + request.getAccount(), null, ipAddress, true, null);
            return Result.success("账号与手机号验证通过", response);
        } catch (Exception e) {
            logRecordService.recordOperationLog("认证管理", "找回密码", 
                    "校验账号与手机号：" + request.getAccount(), null, ipAddress, false, e.getMessage());
            throw e;
        }
    }

    /**
     * 忘记密码第二步：发送邮箱验证码
     */
    @ApiOperation("发送邮箱验证码")
    @PostMapping("/reset/send-email-code")
    public Result<ResetPasswordSendCodeResponse> sendEmailCode(
            @Valid @RequestBody ResetPasswordSendCodeRequest request, HttpServletRequest httpRequest) {
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            ResetPasswordSendCodeResponse response = authService.sendResetEmailCode(request);
            // 记录操作日志
            logRecordService.recordOperationLog("认证管理", "找回密码", 
                    "发送邮箱验证码：" + request.getEmail(), null, ipAddress, true, null);
            return Result.success("验证码发送成功", response);
        } catch (Exception e) {
            logRecordService.recordOperationLog("认证管理", "找回密码", 
                    "发送邮箱验证码：" + request.getEmail(), null, ipAddress, false, e.getMessage());
            throw e;
        }
    }

    /**
     * 忘记密码第二步：校验邮箱验证码
     */
    @ApiOperation("校验邮箱验证码")
    @PostMapping("/reset/verify-email-code")
    public Result<ResetPasswordVerifyCodeResponse> verifyEmailCode(
            @Valid @RequestBody ResetPasswordVerifyCodeRequest request, HttpServletRequest httpRequest) {
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            ResetPasswordVerifyCodeResponse response = authService.verifyResetEmailCode(request);
            // 记录操作日志
            logRecordService.recordOperationLog("认证管理", "找回密码", 
                    "校验邮箱验证码：" + request.getEmail(), null, ipAddress, true, null);
            return Result.success("验证码校验通过", response);
        } catch (Exception e) {
            logRecordService.recordOperationLog("认证管理", "找回密码", 
                    "校验邮箱验证码：" + request.getEmail(), null, ipAddress, false, e.getMessage());
            throw e;
        }
    }

    /**
     * 忘记密码第三步：重置密码
     */
    @ApiOperation("重置密码")
    @PostMapping("/reset/password")
    public Result<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            authService.resetPassword(request);
            // 记录操作日志（敏感操作）
            logRecordService.recordOperationLog("认证管理", "找回密码", 
                    "重置密码：员工ID=" + request.getEmployeeId(), null, ipAddress, true, null);
            return Result.success("密码重置成功，请使用新密码登录");
        } catch (Exception e) {
            logRecordService.recordOperationLog("认证管理", "找回密码", 
                    "重置密码：员工ID=" + request.getEmployeeId(), null, ipAddress, false, e.getMessage());
            throw e;
        }
    }

    /**
     * 登录状态响应DTO
     */
    public static class LoginStatusResponse {
        private String username;
        private boolean loggedIn;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public boolean isLoggedIn() {
            return loggedIn;
        }

        public void setLoggedIn(boolean loggedIn) {
            this.loggedIn = loggedIn;
        }
    }

    /**
     * 系统就绪检查
     */
    @ApiOperation("系统就绪检查")
    @GetMapping("/readiness")
    public Result<ModuleStatusResponse> readiness() {
        ModuleStatusResponse response = new ModuleStatusResponse();
        response.setModuleName("危险废物处理企业ERP管理系统");
        response.setDescription("系统运行正常");
        response.setReady(true);
        return Result.success("系统就绪", response);
    }

    /**
     * 模块状态响应DTO
     */
    public static class ModuleStatusResponse {
        private String moduleName;
        private String description;
        private boolean ready;

        public String getModuleName() {
            return moduleName;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isReady() {
            return ready;
        }

        public void setReady(boolean ready) {
            this.ready = ready;
        }
    }
}


