package io.dbguardian.spring.aspect;

import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor;

/**
 * Spring JDBC 操作拦截 Advisor。
 *
 * <p>匹配 JdbcOperations 及 NamedParameterJdbcOperations 的实现类，
 * 覆盖 JdbcTemplate 和 NamedParameterJdbcTemplate 的公共操作入口。</p>
 */
public class SpringJdbcDataSourceAdvisor extends AspectJExpressionPointcutAdvisor {

    private static final String POINTCUT =
            "execution(* org.springframework.jdbc.core.JdbcOperations+.*(..))"
                    + " || execution(* org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations+.*(..))";

    public SpringJdbcDataSourceAdvisor() {
        super();
        setExpression(POINTCUT);
        setAdvice(new SpringJdbcDataSourceAdvice());
    }
}