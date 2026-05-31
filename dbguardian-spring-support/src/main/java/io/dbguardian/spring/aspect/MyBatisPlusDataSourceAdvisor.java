package io.dbguardian.spring.aspect;

import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor;

/**
 * MyBatis-Plus 拦截切面 Advisor
 *
 * <p>使用 AspectJExpressionPointcutAdvisor 编程式注册切面，
 * 避免 AspectJ 在启动时扫描不存在的类。
 */
public class MyBatisPlusDataSourceAdvisor extends AspectJExpressionPointcutAdvisor {

    private static final String POINTCUT = "execution(* com.baomidou.mybatisplus.core.mapper.BaseMapper+.*(..))";

    public MyBatisPlusDataSourceAdvisor() {
        super();
        setExpression(POINTCUT);
        setAdvice(new DbGuardianDataSourceAdvice());
    }
}
