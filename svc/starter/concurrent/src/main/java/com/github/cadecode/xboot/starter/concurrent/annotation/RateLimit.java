package com.github.cadecode.xboot.starter.concurrent.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解，基于 Guava RateLimiter
 * <p>
 * 可标在类或方法上，方法上的注解优先、类上的兜底；限流粒度是每个被注解的方法。
 * <p>
 * 回退粒度是「整条注解」：方法上只要标了 {@code @RateLimit}，就完全使用它的参数，不与类上的注解做属性合并
 * （与 Spring 的 {@code @Transactional} 一致）。
 *
 * @author Cade Li
 * @date 2022/9/4
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 每秒限制数量
     */
    double limitPerSecond() default 1;

    /**
     * 等待超时时间
     * < 0 表示不限时阻塞
     * = 0 表示不等待，立即返回
     * > 0 表示超时时间
     */
    long waitTimeout() default -1;

    /**
     * 等待时间单位
     */
    TimeUnit waitTimeUnit() default TimeUnit.SECONDS;

    /**
     * 预热时间，单位毫秒
     * < 0 表示不需要预热
     */
    long warmupMillis() default -1;
}
