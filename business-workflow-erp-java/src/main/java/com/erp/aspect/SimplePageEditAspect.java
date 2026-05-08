package com.erp.aspect;

import com.erp.common.annotation.RequireSimplePageEdit;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.auth.dto.PagePermissionConfigResponse;
import com.erp.controller.system.dto.PermissionResponse;
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
import java.util.List;

/**
 * SIMPLE 模式写操作权限切面
 *
 * <p>拦截所有标注了 {@link RequireSimplePageEdit} 的方法，通过
 * {@link AuthService#getMyPagePermission(String)} 获取当前员工对指定页面的权限配置，
 * 校验 {@code canEdit=1}，不区分数据范围（不检查 operateScope）。</p>
 *
 * <p>校验流程：</p>
 * <ol>
 *     <li>从 JWT 中获取当前 employeeId；</li>
 *     <li>超级管理员直接放行；</li>
 *     <li>调用 {@code AuthService.getMyPagePermission(pageCode)} 获取页面权限配置；</li>
 *     <li>若 {@code canEdit != 1}，抛出 {@link BusinessException}（PERMISSION_DENIED）；</li>
 *     <li>{@code canEdit=1} 放行。</li>
 * </ol>
 */
@Aspect
@Component
@Slf4j
public class SimplePageEditAspect {

    @Autowired
    private AuthService authService;

    @Autowired(required = false)
    private ILogRecordService logRecordService;

    @Autowired(required = false)
    private HttpServletRequest httpServletRequest;

    /**
     * 切点：所有标注了 {@link RequireSimplePageEdit} 的方法
     */
    @Pointcut("@annotation(com.erp.common.annotation.RequireSimplePageEdit)")
    public void simplePageEditPointcut() {
    }

    /**
     * 在方法执行前进行 SIMPLE 模式写操作权限校验
     */
    @Before("simplePageEditPointcut()")
    public void checkSimplePageEdit(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireSimplePageEdit annotation = method.getAnnotation(RequireSimplePageEdit.class);
        if (annotation == null) {
            return;
        }

        String pageCode = annotation.value();
        if (pageCode == null || pageCode.trim().isEmpty()) {
            return;
        }

        // 1. 获取当前登录员工ID
        Integer employeeId = SecurityUtil.getCurrentUserId();
        if (employeeId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }

        // 2. 超级管理员直接放行
        if (isSuperAdmin()) {
            log.debug("[SimplePageEdit] 超级管理员，直接放行，pageCode={}", pageCode);
            return;
        }

        // 3. 获取当前员工对指定页面的权限配置
        PagePermissionConfigResponse config = null;
        try {
            config = authService.getMyPagePermission(pageCode);
        } catch (Exception e) {
            log.error("[SimplePageEdit] 获取页面权限配置失败，pageCode={}，employeeId={}", pageCode, employeeId, e);
            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(),
                    "权限验证失败，请稍后重试");
        }

        // 4. canEdit != 1，拒绝访问
        if (config == null || config.getCanEdit() == null || config.getCanEdit() != 1) {
            String methodName = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
            int canEditVal = (config != null && config.getCanEdit() != null) ? config.getCanEdit() : -1;
            log.warn("[SimplePageEdit] 写操作权限不足，employeeId={}，pageCode={}，method={}，canEdit={}",
                    employeeId, pageCode, methodName, canEditVal == -1 ? "null(未配置)" : canEditVal);

            // 记录操作日志（不影响主流程）
            try {
                if (logRecordService != null) {
                    String ipAddress = httpServletRequest != null
                            ? logRecordService.getClientIp(httpServletRequest) : null;
                    String content = "接口写操作无权限，方法=" + methodName
                            + "，pageCode=" + pageCode
                            + "，canEdit=" + (canEditVal == -1 ? "null(未配置)" : canEditVal);
                    logRecordService.recordOperationLog("接口访问控制", "写操作", content,
                            employeeId, ipAddress, false, "没有执行此操作的权限（canEdit!=1）");
                }
            } catch (Exception e) {
                log.warn("[SimplePageEdit] 记录操作日志失败", e);
            }

            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(),
                    "没有执行此操作的权限");
        }

        // 5. canEdit=1，放行
        log.debug("[SimplePageEdit] 写操作权限校验通过，employeeId={}，pageCode={}", employeeId, pageCode);
    }

    /**
     * 判断当前用户是否为超级管理员。
     * 超级管理员权限集合中包含编码 "super_admin"。
     */
    private boolean isSuperAdmin() {
        try {
            List<PermissionResponse> permissions = authService.getMyPermissions();
            if (permissions != null) {
                for (PermissionResponse p : permissions) {
                    if (p != null && "super_admin".equals(p.getPermissionCode())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[SimplePageEdit] 判断超级管理员失败", e);
        }
        return false;
    }
}
