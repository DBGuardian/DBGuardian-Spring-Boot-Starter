package io.dbguardian.spring.aspect;

import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.RoutingContextHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.Order;

/**
 * MyBatis-Plus Mapper 拦截 Advice
 *
 * <p>实现 MethodInterceptor 接口，由 AspectJExpressionPointcutAdvisor 调用。
 */
@Order(0)
public class DbGuardianDataSourceAdvice implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        RoutingContext context = new RoutingContext();
        context.setOrmType("mybatis-plus");
        context.setOperation(MethodNameClassifier.classify(invocation.getMethod().getName()));
        RoutingContextHolder.set(context);
        try {
            return invocation.proceed();
        } finally {
            RoutingContextHolder.clear();
        }
    }
}
