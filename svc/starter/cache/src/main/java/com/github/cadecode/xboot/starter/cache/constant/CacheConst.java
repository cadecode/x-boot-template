package com.github.cadecode.xboot.starter.cache.constant;

/**
 * CacheManager Bean 名称常量
 *
 * @author Cade Li
 * @date 2023/6/17
 */
public final class CacheConst {

    private CacheConst() {
        throw new UnsupportedOperationException();
    }

    public static final String CAFFEINE_5S = "caffeineCacheManager5s";
    public static final String REDIS_5M = "redisCacheManager5m";
    public static final String REDIS_30M = "redisCacheManager30m";
    public static final String DL = "dlCacheManager";

}
