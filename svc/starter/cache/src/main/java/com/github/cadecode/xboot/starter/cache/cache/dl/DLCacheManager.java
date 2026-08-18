package com.github.cadecode.xboot.starter.cache.cache.dl;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.cadecode.xboot.starter.cache.util.DynaTtlNameUtil;
import com.github.cadecode.xboot.starter.cache.cache.dl.DLCacheProperties.LocalConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 双级缓存 Manager
 * <p>
 * 支持 cacheName 带 #ttl 后缀（如 user#6、product#5m），协议见 {@link DynaTtlNameUtil}。
 * 注意：缓存名末尾的 #数字 会被解析为 TTL 后缀，如 order#2024 会被当作 order + 2024s，
 * 若业务缓存名本身含 # 数字需注意避免歧义。
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

        // 解析 #ttl 后缀，配置查询用剥离后缀后的 name
        DynaTtlNameUtil.ParsedName parsedName = DynaTtlNameUtil.parse(name);
        String configName = Objects.nonNull(parsedName) ? parsedName.getName() : name;

        // TTL 优先级：配置 map（显式 0=不过期）> #ttl 后缀（0=不过期）> 默认过期（0=不过期）
        long expiration;
        Map<String, Duration> cacheExpirationMap = cacheProperties.getCacheExpirationMap();
        if (Objects.nonNull(cacheExpirationMap) && cacheExpirationMap.containsKey(configName)) {
            expiration = cacheExpirationMap.get(configName).toMillis();
        } else if (Objects.nonNull(parsedName)) {
            expiration = parsedName.getTtl().toMillis();
        } else {
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
