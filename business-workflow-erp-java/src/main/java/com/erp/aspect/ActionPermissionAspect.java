package com.erp.aspect;

import com.erp.common.annotation.RequireActionPermission;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 动作级权限控制切面
 *
 * <p><b>已禁用：</b>动作级权限检查已取消，现在改用页面级权限的 operateScope 来控制操作权限。</p>
 * <p>此切面保留是为了兼容现有代码中的 {@link RequireActionPermission} 注解，但不再进行权限检查。</p>
 * <p>所有操作权限现在由页面级权限的 operateScope（SELF/ALL）来控制。</p>
 */
@Aspect
@Component
@Slf4j
public class ActionPermissionAspect {

    /**
     * 切点：所有标注了 {@link RequireActionPermission} 的方法
     */
    @Pointcut("@annotation(com.erp.common.annotation.RequireActionPermission)")
    public void actionPermissionPointcut() {
    }

    /**
     * 在方法执行前进行动作级权限校验
     *
     * <p><b>已禁用：</b>此方法已不再进行权限检查，直接通过。</p>
     * <p>操作权限现在由页面级权限的 operateScope 来控制。</p>
     *
     * @param joinPoint AOP连接点
     */
    @Before("actionPermissionPointcut()")
    public void checkActionPermission(JoinPoint joinPoint) {
        // 动作级权限检查已禁用，直接通过
        // 操作权限现在由页面级权限的 operateScope（SELF/ALL）来控制
        // 保留此方法是为了兼容现有代码中的 @RequireActionPermission 注解
        return;
    }
}

