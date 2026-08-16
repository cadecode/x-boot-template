package com.github.cadecode.xboot.starter.cache.cache.redis;

import com.github.cadecode.xboot.starter.cache.cache.DynaTtlNameParser;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.lang.Nullable;

import java.util.Objects;

/**
 * 支持 cacheName 后缀动态指定 TTL 的 RedisCacheManager
 * <p>
 * 后缀保留在 name 中，作为 Redis key 前缀的一部分：user:1 vs user#6:2
 *
 * @author Cade Li
 * @date 2024/7/24
 */
public class DynaTtlRedisCacheManager extends RedisCacheManager {

    private final RedisCacheConfiguration defaultConfig;

    public DynaTtlRedisCacheManager(RedisCacheWriter cacheWriter,
                                    RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration);
        this.defaultConfig = defaultCacheConfiguration;
    }

    @Override
    protected RedisCache createRedisCache(String name, @Nullable RedisCacheConfiguration cacheConfig) {
        RedisCacheConfiguration base = Objects.nonNull(cacheConfig) ? cacheConfig : defaultConfig;
        DynaTtlNameParser.ParsedName parsed = DynaTtlNameParser.parse(name);
        if (Objects.nonNull(parsed)) {
            base = base.entryTtl(parsed.getTtl());
        }
        return super.createRedisCache(name, base);
    }
}
