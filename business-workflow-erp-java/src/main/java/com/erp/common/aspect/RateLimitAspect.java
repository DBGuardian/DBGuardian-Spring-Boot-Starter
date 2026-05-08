package com.erp.common.aspect;

import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;

/**
 * 基于Redis的接口限流切面
 * 使用令牌桶算法 + Lua脚本保证原子性
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class RateLimitAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * Lua脚本：令牌桶限流
     * key: 限流key
     * args[0]: 令牌桶容量
     * args[1]: 每秒补充令牌数
     * args[2]: 当前请求消耗令牌数
     */
    private static final String RATE_LIMIT_SCRIPT =
        "local key = KEYS[1] " +
        "local capacity = tonumber(ARGV[1]) " +
        "local refillRate = tonumber(ARGV[2]) " +
        "local requested = tonumber(ARGV[3]) " +
        "local now = tonumber(ARGV[4]) " +
        "local window = 1 " +
        "local bucket = redis.call('HMGET', key, 'tokens', 'lastRefill') " +
        "local tokens = tonumber(bucket[1]) " +
        "local lastRefill = tonumber(bucket[2]) " +
        "if tokens == nil then " +
        "    tokens = capacity " +
        "    lastRefill = now " +
        "end " +
        "local elapsed = now - lastRefill " +
        "local refill = math.floor(elapsed * refillRate) " +
        "tokens = math.min(capacity, tokens + refill) " +
        "if refill > 0 then " +
        "    lastRefill = now " +
        "end " +
        "if tokens >= requested then " +
        "    tokens = tokens - requested " +
        "    redis.call('HMSET', key, 'tokens', tokens, 'lastRefill', lastRefill) " +
        "    redis.call('EXPIRE', key, window * 2) " +
        "    return 1 " +
        "else " +
        "    return 0 " +
        "end";

    private final DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);

    @Around("@annotation(com.erp.common.aspect.RateLimit)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        String key = buildKey(point, rateLimit);
        long capacity = (long) rateLimit.permitsPerSecond(); // 令牌桶容量
        long refillRate = (long) rateLimit.permitsPerSecond(); // 每秒补充令牌数
        int requested = 1; // 本次请求消耗令牌数

        try {
            // 执行限流检查
            Long result = redisTemplate.execute(
                    redisScript,
                    Collections.singletonList(key),
                    String.valueOf(capacity),
                    String.valueOf(refillRate),
                    String.valueOf(requested),
                    String.valueOf(System.currentTimeMillis() / 1000)
            );

            if (result != null && result == 1) {
                // 允许通过
                return point.proceed();
            } else {
                // 被限流
                log.warn("接口限流触发: key={}, capacity={}, message={}", key, capacity, rateLimit.message());
                return Result.error(429, rateLimit.message());
            }
        } catch (Exception e) {
            // Redis异常时，降级放行（避免影响业务）
            log.warn("限流服务异常，降级放行: key={}, error={}", key, e.getMessage());
            return point.proceed();
        }
    }

    /**
     * 构建限流key
     */
    private String buildKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        String prefix = rateLimit.key();
        if (prefix == null || prefix.isEmpty()) {
            MethodSignature signature = (MethodSignature) point.getSignature();
            prefix = signature.getDeclaringType().getSimpleName() + ":" + signature.getName();
        }
        return "rate_limit:" + prefix;
    }
}
