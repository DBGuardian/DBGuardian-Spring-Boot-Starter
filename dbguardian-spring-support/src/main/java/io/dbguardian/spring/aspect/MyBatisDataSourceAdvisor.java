package io.dbguardian.spring.aspect;

import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor;

/**
 * MyBatis 拦截切面 Advisor
 *
 * <p>使用 AspectJExpressionPointcutAdvisor 编程式注册切面，
 * 避免 AspectJ 在启动时扫描不存在的类。
 */
public class MyBatisDataSourceAdvisor extends AspectJExpressionPointcutAdvisor {

    private static final String POINTCUT = "execution(* *..*Mapper.*(..))";

    public MyBatisDataSourceAdvisor() {
        super();
        setExpression(POINTCUT);
        setAdvice(new MyBatisDataSourceAdvice());
    }
}
