package com.github.cadecode.xboot.starter.cache.cache.dl;

import cn.hutool.core.util.ObjUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.cadecode.xboot.starter.cache.exception.DLCacheException;
import com.github.cadecode.xboot.starter.cache.util.KeyGeneUtil;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 双级缓存实现（Caffeine L1 + Redis L2）
 * 读：本地命中直接返回，未命中查 Redis 并回填本地
 * 写：先写 Redis 再通知集群刷新，最后写本地
 * 集群同步：写操作后通过 Redis pub/sub 通知其他节点清理本地缓存
 *
 * @author Cade Li
 * @date 2023/6/15
 */
@Slf4j
@Getter
public class DLCache extends AbstractValueAdaptingCache {

    private final String name;
    private final long expiration;
    private final DLCacheProperties cacheProperties;
    private final Cache<String, Object> caffeineCache;
    private final RedisTemplate<String, Object> redisTemplate;

    public DLCache(String name, long expiration, DLCacheProperties cacheProperties,
                   Cache<String, Object> caffeineCache, RedisTemplate<String, Object> redisTemplate) {
        super(cacheProperties.isAllowNullValues());
        this.name = name;
        this.expiration = expiration;
        this.cacheProperties = cacheProperties;
        this.caffeineCache = caffeineCache;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    protected Object lookup(Object key) {
        String redisKey = getRedisKey(key);
        // 先查本地
        Object val = caffeineCache.getIfPresent(key.toString());
        if (Objects.nonNull(val)) {
            log.trace("DLCache local get cache, key:{}, value:{}", key, val);
            return val;
        }
        // 回源 Redis
        val = redisTemplate.opsForValue().get(redisKey);
        if (Objects.nonNull(val)) {
            log.debug("DLCache remote get cache, key:{}, value:{}", key, val);
            caffeineCache.put(key.toString(), val);
            return val;
        }
        return val;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        T val = (T) lookup(key);
        if (Objects.nonNull(val)) {
            return val;
        }
        // 双检锁，防止缓存击穿
        synchronized (key.toString().intern()) {
            val = (T) lookup(key);
            if (Objects.nonNull(val)) {
                return val;
            }
            try {
                val = valueLoader.call();
                put(key, val);
            } catch (Exception e) {
                throw new DLCacheException("DLCache valueLoader failed", e);
            }
            return val;
        }
    }

    @Override
    public void put(Object key, Object value) {
        putRemote(key, value);
        sendSyncMsg(key);
        putLocal(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        String redisKey = getRedisKey(key);
        Object oldVal = redisTemplate.opsForValue().get(redisKey);
        if (Objects.nonNull(oldVal)) {
            return toValueWrapper(oldVal);
        }
        Boolean setOkFlag;
        if (expiration > 0) {
            setOkFlag = redisTemplate.opsForValue().setIfAbsent(redisKey, value, expiration, TimeUnit.MILLISECONDS);
        } else {
            setOkFlag = redisTemplate.opsForValue().setIfAbsent(redisKey, value);
        }
        if (ObjUtil.equal(setOkFlag, true)) {
            sendSyncMsg(key);
            putLocal(key, value);
        }
        return toValueWrapper(oldVal);
    }

    @Override
    public void evict(Object key) {
        clearRemote(key);
        sendSyncMsg(key);
        clearLocal(key);
    }

    @Override
    public void clear() {
        clearRemote(null);
        sendSyncMsg(null);
        clearLocal(null);
    }

    private void sendSyncMsg(Object key) {
        String syncTopic = cacheProperties.getRemote().getSyncTopic();
        DLCacheRefreshMsg refreshMsg = new DLCacheRefreshMsg(name, key);
        // 加入自身消息 Map，防止重复处理
        DLCacheRefreshListener.SELF_MSG_MAP.add(refreshMsg);
        redisTemplate.convertAndSend(syncTopic, refreshMsg);
    }

    private void putLocal(Object key, Object value) {
        caffeineCache.put(key.toString(), toStoreValue(value));
    }

    private void putRemote(Object key, Object value) {
        if (expiration > 0) {
            redisTemplate.opsForValue().set(getRedisKey(key), toStoreValue(value), expiration, TimeUnit.MILLISECONDS);
            return;
        }
        redisTemplate.opsForValue().set(getRedisKey(key), toStoreValue(value));
    }

    public void clearRemote(Object key) {
        if (Objects.isNull(key)) {
            Set<String> keys = redisTemplate.keys(getRedisKey("*"));
            if (Objects.nonNull(keys)) {
                redisTemplate.delete(keys);
            }
            return;
        }
        redisTemplate.delete(getRedisKey(key));
    }

    public void clearLocal(Object key) {
        if (Objects.isNull(key)) {
            caffeineCache.invalidateAll();
            return;
        }
        caffeineCache.invalidate(key.toString());
    }

    @Override
    protected Object fromStoreValue(Object storeValue) {
        if (isAllowNullValues() && DLCacheNullVal.INSTANCE.equals(storeValue)) {
            return null;
        }
        return storeValue;
    }

    @Override
    protected Object toStoreValue(Object userValue) {
        if (Objects.nonNull(userValue)) {
            return userValue;
        }
        if (isAllowNullValues()) {
            return DLCacheNullVal.INSTANCE;
        }
        throw new DLCacheException("Null value is not allowed in this cache");
    }

    /**
     * 获取 Redis 完整 key（cacheName:key）
     */
    private String getRedisKey(Object key) {
        return KeyGeneUtil.key(this.name, key.toString());
    }

    /**
     * null 值占位符，区分"key 不存在"和"value 为 null"
     */
    @Data
    public static class DLCacheNullVal {
        public static final DLCacheNullVal INSTANCE = new DLCacheNullVal();
        private String desc = "nullVal";
    }
}
