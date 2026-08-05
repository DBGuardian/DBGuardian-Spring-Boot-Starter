package io.dbguardian.spring.aspect;

import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor;

/**
 * Spring Data JPA Repository 拦截切面 Advisor
 *
 * <p>使用 AspectJExpressionPointcutAdvisor 编程式注册切面，
 * 避免 AspectJ 在启动时扫描不存在的类。</p>
 *
 * <p>点切入表达式匹配所有 Spring Data JPA Repository 接口方法：
 * - JpaRepository (包括 CrudRepository, PagingAndSortingRepository, Repository)
 * </p>
 */
public class JpaDataSourceAdvisor extends AspectJExpressionPointcutAdvisor {

    /**
     * Spring Data JPA 核心接口层次结构点切入表达式
     * 匹配所有继承 JpaRepository 或 CrudRepository 的代理类方法
     */
    private static final String POINTCUT =
            "execution(* org.springframework.data.jpa.repository.JpaRepository+.*(..))";

    public JpaDataSourceAdvisor() {
        super();
        setExpression(POINTCUT);
        setAdvice(new JpaDataSourceAdvice());
    }
}