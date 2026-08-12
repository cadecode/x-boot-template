package com.github.cadecode.xboot.starter.cache.util;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

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
public class RedisKit implements InitializingBean {

    private static RedisTemplate<String, Object> TEMPLATE;
    private static RedisLockKit LOCK_KIT;

    private RedisTemplate<String, Object> redisTemplate;
    private RedisLockKit redisLockKit;

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

    public static Object get(String key) {
        return TEMPLATE.opsForValue().get(key);
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

    public static Boolean setIfAbsent(String key, Object o, long timeout, TimeUnit timeUnit) {
        return TEMPLATE.opsForValue().setIfAbsent(key, o, timeout, timeUnit);
    }

    public static Boolean setIfPresent(String key, Object o, long timeout, TimeUnit timeUnit) {
        return TEMPLATE.opsForValue().setIfPresent(key, o, timeout, timeUnit);
    }

    public static Boolean del(String key) {
        return TEMPLATE.delete(key);
    }

    public static Boolean has(String key) {
        return TEMPLATE.hasKey(key);
    }

    public static Boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return TEMPLATE.expire(key, timeout, timeUnit);
    }

    public static Set<String> keys(String key) {
        return TEMPLATE.keys(key);
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
