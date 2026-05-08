package io.dbguardian.aspect;

import io.dbguardian.config.DbGuardianDataSourceConfig;
import io.dbguardian.enums.DataSourceStatus;
import io.dbguardian.enums.DataSourceType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 读写分离 AOP 切面
 * 自动将读操作路由到从库，写操作路由到主库
 */
@Aspect
@Component
@Order(0)
public class DbGuardianDataSourceAspect {

    @Autowired
    private DbGuardianDataSourceConfig dataSourceConfig;

    /**
     * 定义切点：MyBatis-Plus BaseMapper 的所有方法
     * 支持任意项目使用 MyBatis-Plus
     */
    @Pointcut("execution(* com.baomidou.mybatisplus.core.mapper.BaseMapper+.*(..))")
    public void mapperPointcut() {
    }

    /**
     * 环绕通知：自动切换数据源
     */
    @Around("mapperPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            String methodName = joinPoint.getSignature().getName();

            DataSourceStatus status = dataSourceConfig.getCurrentStatus();

            if (status == DataSourceStatus.SLAVE_PROMOTED) {
                DbGuardianDataSourceConfig.DataSourceContextHolder.useMaster();
            } else if (isReadMethod(methodName)) {
                DbGuardianDataSourceConfig.DataSourceContextHolder.useSlave();
            } else {
                DbGuardianDataSourceConfig.DataSourceContextHolder.useMaster();
            }

            return joinPoint.proceed();
        } finally {
            DbGuardianDataSourceConfig.DataSourceContextHolder.clear();
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
