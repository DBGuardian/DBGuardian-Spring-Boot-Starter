package io.dbguardian.spring.aspect;

import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.RoutingContextHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.Order;

/**
 * Spring JDBC 操作拦截 Advice。
 *
 * <p>JdbcTemplate 和 NamedParameterJdbcTemplate 都通过 JdbcOperations 执行数据库操作，
 * 在调用期间设置统一路由上下文即可复用 DBGuardian 的数据源选择逻辑。</p>
 */
@Order(0)
public class SpringJdbcDataSourceAdvice implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        RoutingContext context = new RoutingContext();
        context.setOrmType("spring-jdbc");
        context.setOperation(MethodNameClassifier.classify(invocation.getMethod().getName()));
        RoutingContextHolder.set(context);
        try {
            return invocation.proceed();
        } finally {
            RoutingContextHolder.clear();
        }
    }
}