package io.dbguardian.spring.aspect;

import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.RoutingContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;

@Aspect
@Order(0)
public class DbGuardianDataSourceAspect {

    @Pointcut("execution(* com.baomidou.mybatisplus.core.mapper.BaseMapper+.*(..))")
    public void mapperPointcut() {
    }

    @Around("mapperPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        RoutingContext context = new RoutingContext();
        context.setOperation(isReadMethod(joinPoint.getSignature().getName()) ? "read" : "write");
        RoutingContextHolder.set(context);
        try {
            return joinPoint.proceed();
        } finally {
            RoutingContextHolder.clear();
        }
    }

    private boolean isReadMethod(String methodName) {
        String lowerName = methodName == null ? "" : methodName.toLowerCase();
        String[] readPrefixes = new String[]{"select", "get", "query", "find", "count", "list", "page", "search"};
        for (String prefix : readPrefixes) {
            if (lowerName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}