package com.github.cadecode.xboot.starter.cache.util;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 静态工具类
 *
 * @author Cade Li
 * @date 2022/5/29
 */
@Component
public class RedisUtil implements InitializingBean {

    private static RedisTemplate<String, Object> TEMPLATE;
    private static RedisLockKit LOCK_KIT;

    private RedisTemplate<String, Object> redisTemplate;
    private RedisLockKit redisLockKit;

    /**
     * 直通模板，用于本类未封装的操作，避免各处重复注入
     */
    public static RedisTemplate<String, Object> getTemplate() {
        return TEMPLATE;
    }

    public static RedisLockKit getLock() {
        return LOCK_KIT;
    }

    @Autowired(required = false)
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired(required = false)
    public void setRedisLockKit(RedisLockKit redisLockKit) {
        this.redisLockKit = redisLockKit;
    }

    // ---- value operations ----

    /**
     * 序列化器把具体类型写进了 @class，读出来已是原类型，因此转型安全
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) TEMPLATE.opsForValue().get(key);
    }

    public static void set(String key, Object o) {
        TEMPLATE.opsForValue().set(key, o);
    }

    public static Boolean setIfAbsent(String key, Object o) {
        return TEMPLATE.opsForValue().setIfAbsent(key, o);
    }

    public static Boolean setIfPresent(String key, Object o) {
        return TEMPLATE.opsForValue().setIfPresent(key, o);
    }

    public static void set(String key, Object o, long timeout, TimeUnit timeUnit) {
        TEMPLATE.opsForValue().set(key, o, timeout, timeUnit);
    }

    public static void set(String key, Object o, Duration timeout) {
        TEMPLATE.opsForValue().set(key, o, timeout);
    }

    public static Boolean setIfAbsent(String key, Object o, long timeout, TimeUnit timeUnit) {
        return TEMPLATE.opsForValue().setIfAbsent(key, o, timeout, timeUnit);
    }

    public static Boolean setIfAbsent(String key, Object o, Duration timeout) {
        return TEMPLATE.opsForValue().setIfAbsent(key, o, timeout);
    }

    public static Boolean setIfPresent(String key, Object o, long timeout, TimeUnit timeUnit) {
        return TEMPLATE.opsForValue().setIfPresent(key, o, timeout, timeUnit);
    }

    public static Boolean setIfPresent(String key, Object o, Duration timeout) {
        return TEMPLATE.opsForValue().setIfPresent(key, o, timeout);
    }

    public static Boolean del(String key) {
        return TEMPLATE.delete(key);
    }

    /**
     * 批量删除，返回删除个数（单个版本返回是否删除成功）
     */
    public static Long del(Collection<String> keys) {
        return TEMPLATE.delete(keys);
    }

    public static Boolean has(String key) {
        return TEMPLATE.hasKey(key);
    }

    public static Boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return TEMPLATE.expire(key, timeout, timeUnit);
    }

    public static Boolean expire(String key, Duration timeout) {
        return TEMPLATE.expire(key, timeout);
    }

    /**
     * 匹配 key，底层是 Redis 的 KEYS 命令，全库扫描，线上慎用
     */
    public static Set<String> keys(String pattern) {
        return TEMPLATE.keys(pattern);
    }

    @Override
    public void afterPropertiesSet() {
        TEMPLATE = redisTemplate;
        LOCK_KIT = redisLockKit;
        if (Objects.isNull(TEMPLATE)) {
            throw new IllegalArgumentException("Bean redisTemplate not found");
        }
    }
}
