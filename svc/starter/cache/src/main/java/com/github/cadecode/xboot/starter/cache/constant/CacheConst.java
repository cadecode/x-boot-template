package com.github.cadecode.xboot.starter.cache.constant;

/**
 * Cache 常量
 *
 * @author Cade Li
 * @date 2023/6/17
 */
public final class CacheConst {

    private CacheConst() {
        throw new UnsupportedOperationException();
    }

    /**
     * cache type 配置匹配值，与 {@link com.github.cadecode.xboot.starter.cache.enums.CacheTypeEnum#getType()} 一致。
     * 注解属性需编译期常量，无法引用枚举方法，故保留字面量
     */
    public static final String TYPE_REDIS = "REDIS";
    public static final String TYPE_DL = "DL";

    /**
     * CacheManager bean 名，与 {@link com.github.cadecode.xboot.starter.cache.enums.CacheTypeEnum#getCacheManager()} 一致
     */
    public static final String MANAGER_REDIS = "redisCacheManager";
    public static final String MANAGER_DL = "dlCacheManager";

}
