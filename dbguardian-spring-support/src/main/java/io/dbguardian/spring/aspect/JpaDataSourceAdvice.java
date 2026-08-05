package io.dbguardian.spring.aspect;

import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.RoutingContextHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.Order;

/**
 * Spring Data JPA Repository 拦截 Advice
 *
 * <p>实现 MethodInterceptor 接口，由 AspectJExpressionPointcutAdvisor 调用。
 * 通过方法名前缀判定读写操作类型，设置 RoutingContext.ormType = "jpa"。</p>
 */
@Order(0)
public class JpaDataSourceAdvice implements MethodInterceptor {

    private static final String[] READ_PREFIXES = new String[]{
            "find", "get", "query", "count", "exists", "list", "page",
            "search", "fetch", "load", "read", "select"
    };

    private static final String[] WRITE_PREFIXES = new String[]{
            "save", "insert", "update", "delete", "remove", "create",
            "merge", "persist", "flush", "refresh", "detach"
    };

    private static final String[] TRANSACTIONAL_READ_METHODS = new String[]{
            "findById", "findOne", "findAll", "getOne", "getReference"
    };

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        RoutingContext context = new RoutingContext();
        context.setOrmType("jpa");
        context.setOperation(classifyOperation(invocation.getMethod().getName()));
        RoutingContextHolder.set(context);
        try {
            return invocation.proceed();
        } finally {
            RoutingContextHolder.clear();
        }
    }

    /**
     * 判定操作类型：read / write / unknown
     */
    private String classifyOperation(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            return "unknown";
        }

        // 已知的事务内读取方法（Spring Data JPA 内部事务注解）
        for (String knownMethod : TRANSACTIONAL_READ_METHODS) {
            if (knownMethod.equals(methodName)) {
                return "read";
            }
        }

        String lowerMethod = methodName.toLowerCase();

        // 匹配写操作前缀
        for (String prefix : WRITE_PREFIXES) {
            if (lowerMethod.startsWith(prefix)) {
                return "write";
            }
        }

        // 匹配读操作前缀
        for (String prefix : READ_PREFIXES) {
            if (lowerMethod.startsWith(prefix)) {
                return "read";
            }
        }

        // Spring Data JPA 自定义接口方法（如 findByXxx）通常返回查询结果 → read
        if (lowerMethod.startsWith("by") || lowerMethod.startsWith("where")) {
            return "read";
        }

        return "unknown";
    }
}