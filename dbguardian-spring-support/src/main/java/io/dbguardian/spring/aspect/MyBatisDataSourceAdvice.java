package io.dbguardian.spring.aspect;

import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.RoutingContextHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.Order;

/**
 * MyBatis Mapper 拦截 Advice
 *
 * <p>实现 MethodInterceptor 接口，由 AspectJExpressionPointcutAdvisor 调用。
 */
@Order(0)
public class MyBatisDataSourceAdvice implements MethodInterceptor {

    private static final String MYBATIS_PLUS_PACKAGE = "com.baomidou.mybatisplus";

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 跳过 MyBatis-Plus 的 Mapper
        if (isMybatisPlusMapper(invocation)) {
            return invocation.proceed();
        }

        RoutingContext context = new RoutingContext();
        context.setOrmType("mybatis");
        context.setOperation(MethodNameClassifier.classify(invocation.getMethod().getName()));
        RoutingContextHolder.set(context);
        try {
            return invocation.proceed();
        } finally {
            RoutingContextHolder.clear();
        }
    }

    private boolean isMybatisPlusMapper(MethodInvocation invocation) {
        Object target = invocation.getThis();
        if (target == null) {
            return false;
        }
        Class<?> targetClass = target.getClass();
        for (Class<?> iface : targetClass.getInterfaces()) {
            if (iface.getName().startsWith(MYBATIS_PLUS_PACKAGE)) {
                return true;
            }
        }
        return targetClass.getName().startsWith(MYBATIS_PLUS_PACKAGE);
    }
}
