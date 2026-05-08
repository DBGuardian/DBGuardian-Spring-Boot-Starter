package com.erp.service.auth.impl;

import com.erp.common.constant.RedisConstant;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.controller.auth.dto.*;
import com.erp.controller.system.dto.PermissionResponse;
import com.erp.entity.system.Employee;
import com.erp.entity.system.Permission;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.PermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.security.token.JwtTokenUtil;
import com.erp.service.auth.AuthService;
import com.erp.service.system.EmailChannelService;
import com.erp.service.system.dto.EmailSendRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.erp.entity.system.EmployeePermission;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.util.SecurityUtil;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final int RESET_EMAIL_CODE_EXPIRE_MINUTES = 10;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private EmailChannelService emailChannelService;

    @Autowired
    private Environment environment;

    @Autowired
    private com.erp.service.system.SystemService systemService;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private com.erp.mapper.system.SystemMapper systemMapper;

    @Override
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        // 1. 验证验证码
        validateCaptcha(request.getCaptchaKey(), request.getCaptcha());

        // 2. 查询员工信息
        Employee employee = employeeMapper.selectByLoginAccount(request.getUsername());
        if (employee == null) {
            throw new BusinessException(ResultCodeEnum.LOGIN_ERROR.getCode(), "用户名或密码错误");
        }

        // 3. 检查员工状态
        String status = employee.getEmployeeStatus();
        if (!"在职".equals(status)) {
            throw new BusinessException(ResultCodeEnum.LOGIN_ERROR.getCode(), "您的账号已停用，请联系管理员");
        }

        // 4. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), employee.getPassword())) {
            // 记录登录失败次数
            recordLoginFailure(request.getUsername());
            throw new BusinessException(ResultCodeEnum.LOGIN_ERROR.getCode(), "用户名或密码错误");
        }

        // 5. 直接用已查到的 Employee 构建 UserDetails 并写入 SecurityContext，
        //    避免 authenticationManager.authenticate() 再次触发 UserDetailsService 查库
        com.erp.security.user.UserDetailsImpl userDetailsForCtx =
                new com.erp.security.user.UserDetailsImpl(employee);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        userDetailsForCtx, null, userDetailsForCtx.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // 6. 生成Token
        String token = jwtTokenUtil.generateToken(employee.getLoginAccount(), employee.getEmployeeId());
        String refreshToken = jwtTokenUtil.generateRefreshToken(employee.getLoginAccount(), employee.getEmployeeId());

        // 7. 使用pipeline批量写入Token到Redis，减少网络往返
        String tokenKey = RedisConstant.TOKEN_PREFIX + employee.getLoginAccount();
        String refreshTokenKey = RedisConstant.REFRESH_TOKEN_PREFIX + employee.getLoginAccount();
        final String finalToken = token;
        final String finalRefreshToken = refreshToken;
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            byte[] tkKey = redisTemplate.getStringSerializer().serialize(tokenKey);
            byte[] tkVal = redisTemplate.getStringSerializer().serialize(finalToken);
            byte[] rtkKey = redisTemplate.getStringSerializer().serialize(refreshTokenKey);
            byte[] rtkVal = redisTemplate.getStringSerializer().serialize(finalRefreshToken);
            connection.setEx(tkKey, 2 * 3600L, tkVal);
            connection.setEx(rtkKey, 7 * 24 * 3600L, rtkVal);
            return null;
        });

        // 8. 清除登录失败记录（pipeline批量删除）
        clearLoginFailure(request.getUsername());

        // 9. 构建响应
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .loginAccount(employee.getLoginAccount())
                .employeeId(employee.getEmployeeId())
                .employeeName(employee.getEmployeeName())
                .department(employee.getDepartment())
                .jobTitle(employee.getJobTitle())
                .phone(employee.getPhone())
                .email(employee.getEmail())
                .employeeStatus(employee.getEmployeeStatus())
                .roles(getUserRoles(employee.getEmployeeId()))
                .permissions(getUserPermissions(employee.getEmployeeId()))
                .build();

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userInfo(userInfo)
                .build();
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCodeEnum.TOKEN_INVALID.getCode(), "Token不存在或无效");
        }

        try {
            String username = jwtTokenUtil.getUsernameFromToken(token);

            // 校验Redis中的Token是否匹配
            String tokenKey = RedisConstant.TOKEN_PREFIX + username;
            String refreshTokenKey = RedisConstant.REFRESH_TOKEN_PREFIX + username;
            String storedToken = (String) redisTemplate.opsForValue().get(tokenKey);

            if (storedToken != null && !token.equals(storedToken)) {
                throw new BusinessException(ResultCodeEnum.TOKEN_INVALID.getCode(), "Token已失效");
            }

            // 使用pipeline批量删除Token及刷新Token，减少Redis网络往返
            List<String> keysToDelete = new ArrayList<>();
            keysToDelete.add(tokenKey);
            keysToDelete.add(refreshTokenKey);
            redisTemplate.delete(keysToDelete);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户登出失败", e);
            throw new BusinessException(ResultCodeEnum.TOKEN_INVALID.getCode(), "Token解析失败");
        } finally {
            // 清除SecurityContext
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        try {
            // 1. 验证RefreshToken
            String username = jwtTokenUtil.getUsernameFromToken(request.getRefreshToken());
            if (!jwtTokenUtil.validateToken(request.getRefreshToken(), username)) {
                throw new BusinessException(ResultCodeEnum.TOKEN_INVALID.getCode(), "刷新Token无效");
            }

            // 2. 检查Redis中的RefreshToken
            String refreshTokenKey = RedisConstant.REFRESH_TOKEN_PREFIX + username;
            String storedRefreshToken = (String) redisTemplate.opsForValue().get(refreshTokenKey);
            if (!request.getRefreshToken().equals(storedRefreshToken)) {
                throw new BusinessException(ResultCodeEnum.TOKEN_INVALID.getCode(), "刷新Token无效");
            }

            // 3. 查询员工信息
            Employee employee = employeeMapper.selectByLoginAccount(username);
            if (employee == null || !"在职".equals(employee.getEmployeeStatus())) {
                throw new BusinessException(ResultCodeEnum.TOKEN_INVALID.getCode(), "用户不存在或已被禁用");
            }

            // 4. 生成新的Token
            String newToken = jwtTokenUtil.generateToken(employee.getLoginAccount(), employee.getEmployeeId());
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(employee.getLoginAccount(), employee.getEmployeeId());

            // 5. 更新Redis中的Token
            String tokenKey = RedisConstant.TOKEN_PREFIX + username;
            redisTemplate.opsForValue().set(tokenKey, newToken, 2, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(refreshTokenKey, newRefreshToken, 7, TimeUnit.DAYS);

            return TokenRefreshResponse.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .build();
        } catch (Exception e) {
            log.error("刷新Token失败", e);
            throw new BusinessException(ResultCodeEnum.TOKEN_INVALID.getCode(), "刷新Token失败");
        }
    }

    @Override
    public boolean validateToken(String token, String username) {
        // 仅依赖JWT自身的签名与过期时间进行校验
        return jwtTokenUtil.validateToken(token, username);
    }

    @Override
    public ResetPasswordVerifyResponse verifyAccountPhone(ResetPasswordVerifyRequest request) {
        String account = StringUtils.trimWhitespace(request.getAccount());
        String phone = StringUtils.trimWhitespace(request.getPhone());

        Employee employee = employeeMapper.selectByLoginAccount(account);
        if (employee == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "账号不存在");
        }
        if (!"在职".equals(employee.getEmployeeStatus())) {
            throw new BusinessException(ResultCodeEnum.LOGIN_ERROR.getCode(), "该账号已停用，请联系管理员");
        }
        String employeePhone = employee.getPhone();
        if (!StringUtils.hasText(employeePhone) || !employeePhone.equals(phone)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "账号与手机号不匹配");
        }

        ResetPasswordVerifyResponse response = new ResetPasswordVerifyResponse();
        response.setEmployeeId(employee.getEmployeeId());
        response.setEmployeeName(employee.getEmployeeName());
        response.setLoginAccount(employee.getLoginAccount());
        response.setPhone(employeePhone);
        return response;
    }

    @Override
    public ResetPasswordSendCodeResponse sendResetEmailCode(ResetPasswordSendCodeRequest request) {
        String email = normalizeEmail(request.getEmail());

        // 根据邮箱查询员工（从 EMPLOYEE.邮箱 字段）
        Employee employee = employeeMapper.selectByEmail(email);
        if (employee == null) {
            throw   new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "该邮箱未注册，请确认后重试");
        }
        if (!"在职".equals(employee.getEmployeeStatus())) {
            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "账号已停用，请联系管理员");
        }

        String verifyCode = generateVerifyCode();
        LocalDateTime now = LocalDateTime.now();

        ResetEmailCodeCache cache = new ResetEmailCodeCache(email, verifyCode, now, employee.getEmployeeId());
        String redisKey = buildResetEmailCodeKey(email);
        redisTemplate.opsForValue().set(redisKey, cache, RESET_EMAIL_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        EmailSendRequest emailSendRequest = new EmailSendRequest();
        emailSendRequest.getToList().add(email);
        emailSendRequest.setSubject("【危险废物ERP】密码重置验证码");
        emailSendRequest.setContent(buildEmailContent(employee.getEmployeeName(), verifyCode, RESET_EMAIL_CODE_EXPIRE_MINUTES));

        emailChannelService.sendEmail(emailSendRequest);

        ResetPasswordSendCodeResponse response = new ResetPasswordSendCodeResponse();
        response.setTargetEmail(email);
        response.setEmployeeId(employee.getEmployeeId());
        response.setExpireSeconds(RESET_EMAIL_CODE_EXPIRE_MINUTES * 60);
        response.setSentAt(now);
        return response;
    }

    @Override
    public ResetPasswordVerifyCodeResponse verifyResetEmailCode(ResetPasswordVerifyCodeRequest request) {
        if (request.getEmployeeId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "员工ID不能为空");
        }

        String email = normalizeEmail(request.getEmail());
        String redisKey = buildResetEmailCodeKey(email);

        ResetEmailCodeCache cache = (ResetEmailCodeCache) redisTemplate.opsForValue().get(redisKey);
        if (cache == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "验证码已失效，请重新获取");
        }
        if (!email.equals(cache.getEmail())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "邮箱地址与验证码不匹配");
        }
        if (!request.getEmployeeId().equals(cache.getEmployeeId())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "员工信息不匹配，请重新验证");
        }

        String inputCode = StringUtils.trimWhitespace(request.getEmailCode());
        if (!StringUtils.hasText(inputCode) || !inputCode.equals(cache.getCode())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "验证码不正确");
        }

        redisTemplate.delete(redisKey);

        ResetPasswordVerifyCodeResponse response = new ResetPasswordVerifyCodeResponse();
        response.setVerified(true);
        response.setEmployeeId(cache.getEmployeeId());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequest request) {
        Integer employeeId = request.getEmployeeId();
        if (employeeId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "员工信息异常，请重新验证账号");
        }

        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "员工不存在");
        }
        if (!"在职".equals(employee.getEmployeeStatus())) {
            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "账号已停用，请联系管理员");
        }

        // 验证密码和确认密码是否一致
        String newPassword = StringUtils.trimWhitespace(request.getNewPassword());
        String confirmPassword = StringUtils.trimWhitespace(request.getConfirmPassword());
        if (!StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmPassword)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "密码不能为空");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "两次输入的密码不一致");
        }

        // 验证密码强度（已在DTO中通过@Pattern验证，这里再次确认）
        if (newPassword.length() < 8 || newPassword.length() > 32) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "密码长度必须在8-32位之间");
        }
        if (!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "密码必须包含字母和数字");
        }

        // 检查新密码是否与旧密码相同
        if (passwordEncoder.matches(newPassword, employee.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "新密码不能与旧密码相同");
        }

        // 加密新密码
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 更新员工密码
        employee.setPassword(encodedPassword);
        int rows = employeeMapper.updateById(employee);
        if (rows == 0) {
            throw new BusinessException(ResultCodeEnum.DATA_CONFLICT.getCode(), "重置密码失败：记录已被其他用户修改");
        }

        // 清除验证码缓存（如果存在）
        String redisKey = buildResetEmailCodeKey(employee.getEmail());
        redisTemplate.delete(redisKey);

        log.info("员工 {} 密码重置成功", employee.getLoginAccount());
    }

    @Override
    public java.util.List<PermissionResponse> getMyPermissions() {
        // 获取当前登录用户的 employeeId
        Integer employeeId = com.erp.common.util.SecurityUtil.getCurrentUserId();
        if (employeeId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }

        // 优先从Redis缓存读取（与角色/员工权限变更的失效逻辑对齐）
        String cacheKey = RedisConstant.PERMISSION_PREFIX + "employee:" + employeeId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<PermissionResponse> cachedList = (java.util.List<PermissionResponse>) cached;
                if (cachedList != null) {
                    // 登录阶段需要计算并缓存字段权限视图：当权限对象缓存命中时，补齐/预热 employee_view 缓存（最佳努力，不影响主流程返回）
                    final String viewCacheKey = RedisConstant.PERMISSION_PREFIX + "employee_view:" + employeeId;
                    try {
                        Object viewCached = redisTemplate.opsForValue().get(viewCacheKey);
                        if (viewCached == null) {
                            systemService.getEmployeePermissions(employeeId);
                        } else if (!(viewCached instanceof java.util.Map)) {
                            redisTemplate.delete(viewCacheKey);
                            systemService.getEmployeePermissions(employeeId);
                        }
                    } catch (Exception ex) {
                        log.warn("预热员工权限视图缓存失败 employeeId={}", employeeId, ex);
                    }
                    return cachedList;
                }
            } else if (cached != null) {
                // 缓存类型异常，删除避免污染
                redisTemplate.delete(cacheKey);
            }
        } catch (Exception e) {
            log.warn("读取权限缓存失败 employeeId={}", employeeId, e);
        }

        java.util.List<String> permissionCodes;

        // 一次查询判断是否超级管理员，后续复用结果，避免 isSuperAdmin() 被多次调用触发重复查库
        final boolean superAdmin = isSuperAdmin(employeeId);

        // 判断是否是超级管理员
        if (superAdmin) {
            // 超级管理员返回所有权限编码（包括模块、页面、动作、字段级权限）
            QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
            queryWrapper.isNotNull("权限编码");
            queryWrapper.ne("权限编码", "");
            java.util.List<Permission> allPermissions = permissionMapper.selectList(queryWrapper);
            permissionCodes = allPermissions.stream()
                    .map(Permission::getPermissionCode)
                    .filter(code -> code != null && !code.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        } else {
            // 普通用户：调用 SystemService 获取员工权限（包含角色权限和员工层覆盖）
            java.util.Map<String, Object> permissionsMap = systemService.getEmployeePermissions(employeeId);
            
            // 提取权限编码集合（SystemService返回的是HashSet）
            @SuppressWarnings("unchecked")
            java.util.Set<String> permissionCodesSet = (java.util.Set<String>) permissionsMap.get("permissionCodes");

            // 如果权限编码集合为空，返回空列表
            if (permissionCodesSet == null || permissionCodesSet.isEmpty()) {
                return new java.util.ArrayList<>();
            }

            // 普通用户：显式过滤掉“系统管理”模块及其所有子权限，只保留非系统管理权限
            // 约定：系统管理相关权限编码为：
            //  - 模块级：  系统管理
            //  - 页面级：  以“系统管理:”开头（如 系统管理:页面、系统管理:邮件配置:页面 等）
            //  - 动作级：  同样以“系统管理:”开头
            permissionCodesSet = permissionCodesSet.stream()
                    .filter(code -> {
                        if (code == null) {
                            return false;
                        }
                        String trimmed = code.trim();
                        // 非超级管理员不允许拥有任何系统管理相关权限编码
                        return !("系统管理".equals(trimmed) || trimmed.startsWith("系统管理:"));
                    })
                    .collect(java.util.stream.Collectors.toSet());

            // 过滤结果可能为空：此时直接返回空列表
            if (permissionCodesSet.isEmpty()) {
                return new java.util.ArrayList<>();
            }

            // 过滤空编码并转换为List
            permissionCodes = permissionCodesSet.stream()
                    .filter(code -> code != null && !code.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // 根据权限编码查询对应的权限对象（包含权限类型ID）
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // 批量查询权限对象
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("权限编码", permissionCodes);
        queryWrapper.isNotNull("权限编码");
        queryWrapper.ne("权限编码", "");
        java.util.List<Permission> permissions = permissionMapper.selectList(queryWrapper);
        
        // 转换为权限编码到权限对象的映射
        java.util.Map<String, Permission> codeToPermissionMap = permissions.stream()
                .collect(java.util.stream.Collectors.toMap(
                    Permission::getPermissionCode,
                    p -> p,
                    (existing, replacement) -> existing
                ));
        
        // 收集所有需要查询的父权限ID（去重）
        java.util.Set<Integer> parentPermissionIds = permissions.stream()
                .map(Permission::getParentPermissionId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        
        // 批量查询所有父权限
        java.util.Map<Integer, Permission> parentPermissionMap = new java.util.HashMap<>();
        if (!parentPermissionIds.isEmpty()) {
            QueryWrapper<Permission> parentQueryWrapper = new QueryWrapper<>();
            parentQueryWrapper.in("权限编号", parentPermissionIds);
            java.util.List<Permission> parentPermissions = permissionMapper.selectList(parentQueryWrapper);
            parentPermissionMap = parentPermissions.stream()
                    .collect(java.util.stream.Collectors.toMap(
                        Permission::getPermissionId,
                        p -> p,
                        (existing, replacement) -> existing
                    ));
        }
        
        // 批量查询员工的页面权限配置（仅针对页面级权限）
        java.util.List<Integer> pagePermissionIds = permissions.stream()
                .filter(p -> p.getPermissionTypeId() != null && p.getPermissionTypeId() == 2)
                .map(Permission::getPermissionId)
                .collect(java.util.stream.Collectors.toList());
        
        java.util.Map<Integer, com.erp.entity.system.EmployeePermission> employeePermissionMap = new java.util.HashMap<>();
        if (!pagePermissionIds.isEmpty() && !superAdmin) {
            // 批量查询员工的页面权限配置
            QueryWrapper<com.erp.entity.system.EmployeePermission> empPermQueryWrapper = new QueryWrapper<>();
            empPermQueryWrapper.eq("员工编码", employeeId);
            empPermQueryWrapper.in("页面权限编号", pagePermissionIds);
            java.util.List<com.erp.entity.system.EmployeePermission> employeePermissions = 
                    employeePermissionMapper.selectList(empPermQueryWrapper);
            
            if (employeePermissions != null && !employeePermissions.isEmpty()) {
                employeePermissionMap = employeePermissions.stream()
                        .collect(java.util.stream.Collectors.toMap(
                            com.erp.entity.system.EmployeePermission::getPagePermissionId,
                            ep -> ep,
                            (existing, replacement) -> existing
                        ));
            }
        }
        
        // 构建响应列表，保持与权限编码列表相同的顺序
        java.util.List<PermissionResponse> result = new java.util.ArrayList<>();
        for (String code : permissionCodes) {
            Permission permission = codeToPermissionMap.get(code);
            if (permission != null) {
                // 从映射中获取父权限名称（避免重复查询）
                String parentPermissionName = null;
                if (permission.getParentPermissionId() != null) {
                    Permission parentPermission = parentPermissionMap.get(permission.getParentPermissionId());
                    if (parentPermission != null) {
                        parentPermissionName = parentPermission.getPermissionName();
                    }
                }
                
                // 构建基础响应对象
                PermissionResponse.PermissionResponseBuilder responseBuilder = PermissionResponse.builder()
                        .permissionId(permission.getPermissionId())
                        .permissionName(permission.getPermissionName())
                        .permissionDescription(permission.getPermissionDescription())
                        .permissionTypeId(permission.getPermissionTypeId())
                        .permissionCode(permission.getPermissionCode())
                        .pageMode(permission.getPageMode())
                        .parentPermissionId(permission.getParentPermissionId())
                        .parentPermissionName(parentPermissionName);
                
                // 对于页面级权限，添加权限配置信息
                if (permission.getPermissionTypeId() != null && permission.getPermissionTypeId() == 2) {
                    if (superAdmin) {
                        // 超级管理员：默认全部权限
                        responseBuilder
                                .viewScope("ALL")
                                .operateScope("ALL")
                                .canView(1)
                                .canEdit(1);
                    } else {
                        // 普通用户：从员工权限配置中获取
                        com.erp.entity.system.EmployeePermission empPerm = 
                                employeePermissionMap.get(permission.getPermissionId());
                        
                        if (empPerm != null) {
                            responseBuilder
                                    .viewScope(empPerm.getViewScope())
                                    .operateScope(empPerm.getOperateScope())
                                    .canView(empPerm.getCanView())
                                    .canEdit(empPerm.getCanEdit());
                        } else {
                            // 如果没有配置，使用默认值（ALL表示不限制）
                            responseBuilder
                                    .viewScope("ALL")
                                    .operateScope("ALL")
                                    .canView(1)
                                    .canEdit(1);
                        }
                    }
                }
                
                result.add(responseBuilder.build());
            }
        }

        // 写入缓存（默认30分钟；变更时由相关写入口主动删除）
        try {
            redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(30));
        } catch (Exception e) {
            log.warn("写入权限缓存失败 employeeId={}", employeeId, e);
        }

        return result;
    }

    /**
     * 判断指定员工是否为管理员
     * 同时检查：
     * 1. employee 表的 role 字段是否为「超级管理员」
     * 2. 员工关联角色名是否包含 super_admin / 管理员 / 系统管理员 / ADMIN
     *
     * @param employeeId 员工ID
     * @return 是否为管理员
     */
    @Override
    public boolean isAdmin(Integer employeeId) {
        if (employeeId == null) {
            return false;
        }
        try {
            // 一次连表查询同时检查 employee.角色 字段和关联角色名，避免多次查库
            return systemMapper.isAdminByEmployeeId(employeeId) > 0;
        } catch (Exception e) {
            log.warn("检查管理员身份失败：employeeId={}", employeeId, e);
            return false;
        }
    }

    /**
     * 判断用户是否是超级管理员（内部兼容方法，委托给 isAdmin）
     *
     * @param employeeId 员工ID
     * @return 是否是超级管理员
     */
    private boolean isSuperAdmin(Integer employeeId) {
        return isAdmin(employeeId);
    }

    /**
     * 验证验证码
     */
    private void validateCaptcha(String captchaKey, String captcha) {
        // 开发环境下放宽验证码校验（支持固定验证码，减少对Redis的依赖）
        if (isDevProfile()) {
            // dev 环境下前端可直接传入固定验证码，无需访问Redis
            if ("9999".equalsIgnoreCase(captcha)) {
                return;
            }
        }

        if (!StringUtils.hasText(captchaKey) || !StringUtils.hasText(captcha)) {
            throw new BusinessException(ResultCodeEnum.CAPTCHA_ERROR.getCode(), "验证码不能为空");
        }

        String captchaKeyRedis = RedisConstant.CAPTCHA_PREFIX + captchaKey;
        String storedCaptcha = (String) redisTemplate.opsForValue().get(captchaKeyRedis);
        
        if (storedCaptcha == null) {
            throw new BusinessException(ResultCodeEnum.CAPTCHA_ERROR.getCode(), "验证码已过期");
        }

        if (!captcha.equalsIgnoreCase(storedCaptcha)) {
            throw new BusinessException(ResultCodeEnum.CAPTCHA_ERROR.getCode(), "验证码错误");
        }

        // 验证成功后删除验证码
        redisTemplate.delete(captchaKeyRedis);
    }

    /**
     * 是否为开发环境
     */
    private boolean isDevProfile() {
        return environment != null && environment.acceptsProfiles(Profiles.of("dev"));
    }

    /**
     * 记录登录失败次数
     */
    private void recordLoginFailure(String username) {
        String failureKey = RedisConstant.LOGIN_FAIL_COUNT_PREFIX + username;
        Integer failureCount = (Integer) redisTemplate.opsForValue().get(failureKey);
        if (failureCount == null) {
            failureCount = 0;
        }
        failureCount++;
        redisTemplate.opsForValue().set(failureKey, failureCount, 30, TimeUnit.MINUTES);

        // 超过5次失败，锁定30分钟
        if (failureCount >= 5) {
            String lockKey = RedisConstant.LOGIN_LOCK_PREFIX + username;
            redisTemplate.opsForValue().set(lockKey, true, 30, TimeUnit.MINUTES);
        }
    }

    /**
     * 清除登录失败记录（批量删除，减少Redis网络往返）
     */
    private void clearLoginFailure(String username) {
        String failureKey = RedisConstant.LOGIN_FAIL_COUNT_PREFIX + username;
        String lockKey = RedisConstant.LOGIN_LOCK_PREFIX + username;
        List<String> keys = new ArrayList<>();
        keys.add(failureKey);
        keys.add(lockKey);
        redisTemplate.delete(keys);
    }

    /**
     * 获取用户角色列表
     * TODO: 从数据库查询用户的角色
     */
    private List<String> getUserRoles(Integer employeeId) {
        // TODO: 从EMPLOYEE_ROLE和ROLE表查询
        return new ArrayList<>();
    }

    /**
     * 获取用户权限列表
     * TODO: 从数据库查询用户的权限
     */
    private List<String> getUserPermissions(Integer employeeId) {
        // TODO: 从EMPLOYEE_ROLE -> ROLE_PERMISSION -> PERMISSION表查询
        return new ArrayList<>();
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "邮箱地址不能为空");
        }
        return email.trim().toLowerCase();
    }

    private String generateVerifyCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

    private String buildResetEmailCodeKey(String email) {
        return RedisConstant.RESET_EMAIL_CODE_PREFIX + "email:" + email;
    }

    private String buildEmailContent(String employeeName, String code, int expireMinutes) {
        String name = StringUtils.hasText(employeeName) ? employeeName : "用户";
        return new StringBuilder()
                .append("<p>尊敬的 ").append(name).append("，您好：</p>")
                .append("<p>您正在进行密码重置操作，请在 <strong>")
                .append(expireMinutes)
                .append(" 分钟</strong> 内使用以下验证码完成验证：</p>")
                .append("<p style=\"font-size:28px;font-weight:bold;letter-spacing:4px;\">")
                .append(code)
                .append("</p>")
                .append("<p>如非本人操作，请立即联系系统管理员。</p>")
                .toString();
    }

    private static class ResetEmailCodeCache implements Serializable {
        private static final long serialVersionUID = 1L;
        private String email;
        private String code;
        private LocalDateTime sentAt;
        private Integer employeeId;

        public ResetEmailCodeCache() {
        }

        public ResetEmailCodeCache(String email, String code, LocalDateTime sentAt, Integer employeeId) {
            this.email = email;
            this.code = code;
            this.sentAt = sentAt;
            this.employeeId = employeeId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public LocalDateTime getSentAt() {
            return sentAt;
        }

        public void setSentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
        }

        public Integer getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Integer employeeId) {
            this.employeeId = employeeId;
        }
    }

@Override
public PagePermissionConfigResponse getMyPagePermission(String pageCode) {
    Integer employeeId = SecurityUtil.getCurrentEmployeeId();
    
    // 查询页面权限ID
    Permission permission = permissionMapper.selectOne(
        new LambdaQueryWrapper<Permission>()
            .eq(Permission::getPermissionCode, pageCode)
            .eq(Permission::getPermissionTypeId, 2)
    );
    
    if (permission == null) {
        return null;
    }
    
    // 查询员工页面权限配置
    EmployeePermission employeePermission = employeePermissionMapper.selectOne(
        new LambdaQueryWrapper<EmployeePermission>()
            .eq(EmployeePermission::getEmployeeId, employeeId)
            .eq(EmployeePermission::getPagePermissionId, permission.getPermissionId())
    );
    
    if (employeePermission == null) {
        return null;
    }
    
    return PagePermissionConfigResponse.builder()
        .canView(employeePermission.getCanView())
        .canEdit(employeePermission.getCanEdit())
        .viewScope(employeePermission.getViewScope())
        .operateScope(employeePermission.getOperateScope())
        .build();
}

@Override
public String getViewScope(String pageCode) {
    Integer employeeId = SecurityUtil.getCurrentEmployeeId();
    if (employeeId == null) {
        return com.erp.common.enums.ViewScopeEnum.SELF.getCode();
    }

    // 管理员默认拥有全部权限
    if (isAdmin(employeeId)) {
        return com.erp.common.enums.ViewScopeEnum.ALL.getCode();
    }

    // 获取用户对该页面的权限配置
    PagePermissionConfigResponse config = getMyPagePermission(pageCode);
    if (config == null || config.getViewScope() == null || config.getViewScope().isEmpty()) {
        // 未配置权限，默认仅查看自己
        return com.erp.common.enums.ViewScopeEnum.SELF.getCode();
    }

    // 返回数据查看范围
    String viewScope = config.getViewScope().toUpperCase();
    if (!com.erp.common.enums.ViewScopeEnum.isValid(viewScope)) {
        log.warn("用户[{}]的页面[{}]权限配置异常，viewScope={}，默认返回SELF",
                employeeId, pageCode, viewScope);
        return com.erp.common.enums.ViewScopeEnum.SELF.getCode();
    }

    return viewScope;
}
}





