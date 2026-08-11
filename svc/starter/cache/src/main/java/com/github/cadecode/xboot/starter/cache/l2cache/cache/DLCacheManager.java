package com.github.cadecode.xboot.starter.cache.l2cache.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.cadecode.xboot.starter.cache.l2cache.DLCacheProperties;
import com.github.cadecode.xboot.starter.cache.l2cache.DLCacheProperties.LocalConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 双级缓存 Manager
 *
 * @author Cade Li
 * @date 2023/6/15
 */
@Slf4j
@RequiredArgsConstructor
public class DLCacheManager implements CacheManager {

    private final ConcurrentHashMap<String, DLCache> cacheMap = new ConcurrentHashMap<>();
    private final DLCacheProperties cacheProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public DLCache getCache(String name) {
        return cacheMap.computeIfAbsent(name, n -> {
            DLCache dlCache = buildCache(n);
            log.debug("Create DLCache instance, name:{}", n);
            return dlCache;
        });
    }

    private DLCache buildCache(String name) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder();
        long expiration = 0;

        // 按 cacheName 查找过期配置
        Map<String, Long> cacheExpirationMap = cacheProperties.getCacheExpirationMap();
        if (Objects.nonNull(cacheExpirationMap) && cacheExpirationMap.containsKey(name)
                && cacheExpirationMap.get(name) > 0) {
            expiration = cacheExpirationMap.get(name);
        } else if (cacheProperties.getDefaultExpiration() > 0) {
            expiration = cacheProperties.getDefaultExpiration();
        }
        if (expiration > 0) {
            caffeine.expireAfterWrite(expiration, TimeUnit.MILLISECONDS);
        }

        // 本地缓存参数
        LocalConfig localConfig = cacheProperties.getLocal();
        if (localConfig.getInitialCapacity() > 0) {
            caffeine.initialCapacity(localConfig.getInitialCapacity());
        }
        if (localConfig.getMaximumSize() > 0) {
            caffeine.maximumSize(localConfig.getMaximumSize());
        }

        return new DLCache(name, expiration, cacheProperties, caffeine.build(), redisTemplate);
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(cacheMap.keySet());
    }
}
