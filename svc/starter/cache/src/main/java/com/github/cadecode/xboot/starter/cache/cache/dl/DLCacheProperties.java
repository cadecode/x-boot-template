package com.github.cadecode.xboot.starter.cache.cache.dl;

import lombok.Data;

import java.util.Map;

/**
 * 双级缓存配置项
 *
 * @author Cade Li
 * @date 2023/6/15
 */
@Data
public class DLCacheProperties {

    /**
     * 默认过期时间，0 表示不过期，默认 30 分钟
     */
    public static final long DEFAULT_EXPIRATION = 30 * 60 * 1000L;

    /**
     * 最大缓存条数，默认 5 万
     */
    public static final long DEFAULT_MAXIMUM_SIZE = 50000L;

    /**
     * Redis pub/sub 缓存刷新通知主题
     */
    public static final String DEFAULT_SYNC_TOPIC = "cache:dl:refresh:topic";

    /**
     * 是否允许缓存 null 值
     */
    private boolean allowNullValues = true;

    /**
     * 默认过期时间，0 表示不过期，默认 30 分钟
     */
    private long defaultExpiration = DEFAULT_EXPIRATION;

    /**
     * 针对 cacheName 单独设置过期时间
     */
    private Map<String, Long> cacheExpirationMap;

    private LocalConfig local = new LocalConfig();
    private RemoteConfig remote = new RemoteConfig();

    @Data
    public static class LocalConfig {

        /**
         * 初始化容量
         */
        private int initialCapacity;

        /**
         * 最大缓存条数，默认 5 万
         */
        private long maximumSize = DEFAULT_MAXIMUM_SIZE;
    }

    @Data
    public static class RemoteConfig {

        /**
         * Redis pub/sub 缓存刷新通知主题
         */
        private String syncTopic = DEFAULT_SYNC_TOPIC;
    }
}
