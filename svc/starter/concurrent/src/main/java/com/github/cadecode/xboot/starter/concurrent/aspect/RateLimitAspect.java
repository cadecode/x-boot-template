package com.github.cadecode.xboot.starter.concurrent.aspect;

import com.github.cadecode.xboot.common.exception.RateLimitException;
import com.github.cadecode.xboot.starter.concurrent.annotation.RateLimit;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面，基于 Guava RateLimiter
 *
 * @author Cade Li
 * @date 2022/9/4
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private static final ConcurrentHashMap<String, RateLimiter> LIMITER_MAP = new ConcurrentHashMap<>();

    @Pointcut("@within(com.github.cadecode.xboot.starter.concurrent.annotation.RateLimit) " +
            "|| @annotation(com.github.cadecode.xboot.starter.concurrent.annotation.RateLimit)")
    public void pointCut() {
    }

    @Before("pointCut()")
    public void rateLimit(JoinPoint point) {
        MethodSignature methodSignature = (MethodSignature) point.getSignature();
        RateLimit rateLimit = methodSignature.getMethod().getAnnotation(RateLimit.class);

        // 从缓存获取或创建 RateLimiter
        RateLimiter rateLimiter = LIMITER_MAP.computeIfAbsent(methodSignature.toLongString(), m -> {
            if (rateLimit.warmupMillis() < 0) {
                return RateLimiter.create(rateLimit.limitPerSecond());
            }
            return RateLimiter.create(rateLimit.limitPerSecond(), rateLimit.warmupMillis(), TimeUnit.MILLISECONDS);
        });

        // 判断是否需要阻塞等待
        if (rateLimit.waitTimeout() < 0) {
            rateLimiter.acquire();
            return;
        }
        boolean tryOk;
        if (rateLimit.waitTimeout() > 0) {
            tryOk = rateLimiter.tryAcquire(rateLimit.waitTimeout(), rateLimit.waitTimeUnit());
        } else {
            tryOk = rateLimiter.tryAcquire();
        }
        if (!tryOk) {
            throw new RateLimitException("Rate limit exceeded");
        }
    }
}
