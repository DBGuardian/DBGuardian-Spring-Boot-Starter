package com.erp.common.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * 使用 Bucket4j + Redis 实现分布式限流
 *
 * <p>使用方式：
 * <pre>
 * {@code
 * @RateLimit(key = "api:user", permitsPerSecond = 10, timeout = 1)
 * public Result<?> getUserInfo() { ... }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流key前缀，默认使用方法名
     */
    String key() default "";

    /**
     * 每秒允许的请求数
     */
    double permitsPerSecond() default 100;

    /**
     * 获取令牌超时时间（秒）
     */
    long timeout() default 1;

    /**
     * 限流提示消息
     */
    String message() default "请求过于频繁，请稍后再试";
}
