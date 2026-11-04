package com.github.cadecode.xboot.starter.concurrent.aspect;

import com.github.cadecode.xboot.common.exception.RateLimitException;
import com.github.cadecode.xboot.common.util.AspectUtil;
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
        // 方法上的注解优先，类上的兜底
        RateLimit rateLimit = AspectUtil.requireAnnotation(point.getTarget(), methodSignature.getMethod(),
                RateLimit.class);

        // 限流器粒度是「被切到的每个方法」，key 带上目标类，避免同一接口方法的不同实现共用限流器
        String limiterKey = point.getTarget().getClass().getName() + '#' + methodSignature.toLongString();
        // 从缓存获取或创建 RateLimiter
        RateLimiter rateLimiter = LIMITER_MAP.computeIfAbsent(limiterKey, m -> {
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
