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

    public static final String CAFFEINE = "caffeineCacheManager";
    public static final String REDIS = "redisCacheManager";
    public static final String DL = "dlCacheManager";

}
