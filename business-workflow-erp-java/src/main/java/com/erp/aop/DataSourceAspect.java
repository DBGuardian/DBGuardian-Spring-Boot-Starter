package com.erp.aop;

import com.erp.config.DataSourceConfig.DataSourceContextHolder;
import com.erp.config.DataSourceConfig.DataSourceType;
import com.erp.config.DataSourceConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 读写分离 AOP 切面
 * 自动将读操作路由到从库，写操作路由到主库
 */
@Aspect
@Component
@Order(0)
public class DataSourceAspect {

    private final DataSourceConfig dataSourceConfig;

    public DataSourceAspect(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    /**
     * 定义切点：mapper 包下的所有方法
     */
    @Pointcut("execution(* com.erp.mapper..*.*(..))")
    public void mapperPointcut() {
    }

    /**
     * 环绕通知：自动切换数据源
     */
    @Around("mapperPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // 根据方法名判断读写操作
            String methodName = joinPoint.getSignature().getName();

            // 检查当前数据源状态
            DataSourceConfig.DataSourceStatus status = dataSourceConfig.getCurrentStatus();

            if (status == DataSourceConfig.DataSourceStatus.SLAVE_PROMOTED) {
                // 从库升主库模式，所有操作使用主库
                DataSourceContextHolder.useMaster();
            } else if (isReadMethod(methodName)) {
                // 读操作使用从库
                DataSourceContextHolder.useSlave();
            } else {
                // 写操作使用主库
                DataSourceContextHolder.useMaster();
            }

            return joinPoint.proceed();
        } finally {
            // 清除数据源上下文
            DataSourceContextHolder.clear();
        }
    }

    /**
     * 判断是否为读方法
     */
    private boolean isReadMethod(String methodName) {
        String[] readPrefixes = {"select", "get", "query", "find", "count", "list", "page", "search"};
        String lowerName = methodName.toLowerCase();

        for (String prefix : readPrefixes) {
            if (lowerName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
