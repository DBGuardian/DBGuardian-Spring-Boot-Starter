package com.erp.aspect;

import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.service.auth.AuthService;
import com.erp.service.system.ILogRecordService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 页面级权限控制切面
 *
 * <p>实现《后端列表查询权限控制实现方案》中 127-131 行描述的逻辑：</p>
 * <ol>
 *     <li>拦截所有带 {@link RequirePagePermission} 注解的方法；</li>
 *     <li>从 {@link SecurityUtil} 中获取当前 employeeId；</li>
 *     <li>通过 {@link SystemService#getEmployeePermissions(Integer)} 或
 *         {@link AuthService#getMyPermissions()} 获取权限编码集合；</li>
 *     <li>若当前员工未包含注解指定的页面权限编码，则抛出
 *         {@link BusinessException}，错误码使用 {@link ResultCodeEnum#PERMISSION_DENIED}。</li>
 * </ol>
 */
@Aspect
@Component
@Slf4j
public class PagePermissionAspect {

    @Autowired
    private AuthService authService;

    @Autowired(required = false)
    private ILogRecordService logRecordService;

    @Autowired(required = false)
    private HttpServletRequest httpServletRequest;

    /**
     * 切点：所有标注了 {@link RequirePagePermission} 的方法
     */
    @Pointcut("@annotation(com.erp.common.annotation.RequirePagePermission)")
    public void pagePermissionPointcut() {
    }

    /**
     * 在方法执行前进行页面级权限校验
     *
     * @param joinPoint AOP连接点
     */
    @Before("pagePermissionPointcut()")
    public void checkPagePermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePagePermission annotation = method.getAnnotation(RequirePagePermission.class);
        if (annotation == null) {
            return;
        }

        String[] requiredCodes = annotation.value();
        if (requiredCodes == null || requiredCodes.length == 0) {
            return;
        }

        // 1. 获取当前登录员工ID
        Integer employeeId = SecurityUtil.getCurrentUserId();
        if (employeeId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }

        // 2. 获取当前员工权限编码集合
        Set<String> permissionCodes = getCurrentEmployeePermissionCodes(employeeId);

        // 3. 判断是否拥有任意一个页面权限编码
        for (String required : requiredCodes) {
            if (required != null && permissionCodes.contains(required.trim())) {
                return;
            }
        }

        // 4. 无访问权限，记录日志并抛出业务异常
        String methodName = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        log.warn("页面权限校验失败，employeeId={}，method={}，requiredCodes={}，ownedCodesSize={}",
                employeeId, methodName, java.util.Arrays.toString(requiredCodes), permissionCodes.size());

        // 记录无权限访问的操作日志（不影响主流程抛异常）
        try {
            if (logRecordService != null) {
                String ipAddress = null;
                if (httpServletRequest != null) {
                    ipAddress = logRecordService.getClientIp(httpServletRequest);
                }
                String content = "接口无权限访问，方法=" + methodName
                        + "，要求页面权限=" + java.util.Arrays.toString(requiredCodes);
                logRecordService.recordOperationLog("接口访问控制", "访问", content,
                        employeeId, ipAddress, false, "没有访问当前页面的权限");
            }
        } catch (Exception e) {
            // 日志记录失败不影响权限校验结果
            log.warn("记录无权限访问操作日志失败，employeeId={}，method={}", employeeId, methodName, e);
        }

        throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(),
                "没有访问当前页面的权限");
    }

    /**
     * 获取当前员工的权限编码集合
     *
     * <p>权限编码来源说明：</p>
     * <ul>
     *     <li>优先依赖 {@link AuthService#getMyPermissions()} 封装的逻辑，从 Redis
     *         {@code permission:employee:{employeeId}} 读取当前员工权限对象列表；</li>
     *     <li>若缓存缺失，则由 {@code AuthServiceImpl} 通过 {@code SystemService.getEmployeePermissions}
     *         重新计算权限集合并回写缓存；</li>
     *     <li>同时兼容超级管理员 {@code super_admin}：其权限集合会被扩展为系统全部权限编码。</li>
     * </ul>
     *
     * @param employeeId 员工ID
     * @return 权限编码去重集合（从不为 null）
     */
    @SuppressWarnings("unchecked")
    private Set<String> getCurrentEmployeePermissionCodes(Integer employeeId) {
        Set<String> result = new HashSet<>();

        // 统一委托给 AuthService.getMyPermissions()，内部已实现：
        // 1）优先从 Redis 读取 permission:employee:{employeeId}；
        // 2）缓存缺失时调用 SystemService.getEmployeePermissions(employeeId) 重新计算并回写缓存；
        // 3）对超级管理员 super_admin 返回全量权限集合；
        try {
            List<com.erp.controller.system.dto.PermissionResponse> permissionResponses = authService.getMyPermissions();
            if (permissionResponses != null) {
                for (com.erp.controller.system.dto.PermissionResponse perm : permissionResponses) {
                    if (perm == null) {
                        continue;
                    }
                    String code = perm.getPermissionCode();
                    if (code != null && !code.trim().isEmpty()) {
                        result.add(code.trim());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("通过 AuthService.getMyPermissions 获取权限编码失败 employeeId={}", employeeId, e);
        }

        return result;
    }
}

