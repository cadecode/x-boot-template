package com.github.cadecode.xboot.starter.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.cadecode.xboot.starter.cache.constant.CacheConst;
import com.github.cadecode.xboot.starter.cache.l2cache.cache.DLCacheManager;
import com.github.cadecode.xboot.starter.cache.l2cache.sync.DLCacheRefreshListener;
import com.github.cadecode.xboot.starter.cache.manager.DynaTtlRedisCacheManager;
import com.github.cadecode.xboot.starter.cache.util.KeyGeneUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * CacheManager 自动配置
 *
 * @author Cade Li
 * @date 2022/1/5
 */
@RequiredArgsConstructor
@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheManagerAutoConfig {

    @ConditionalOnProperty(name = "x-boot.cache.type", havingValue = CacheConst.CAFFEINE)
    @Bean(name = CacheConst.CAFFEINE)
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES));
        caffeineCacheManager.setAllowNullValues(true);
        return caffeineCacheManager;
    }

    @ConditionalOnProperty(name = "x-boot.cache.type", havingValue = CacheConst.REDIS)
    @Bean(name = CacheConst.REDIS)
    public RedisCacheManager redisCacheManager(RedisTemplate<String, Object> redisTemplate) {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(o -> o + KeyGeneUtil.SEPARATOR)
                .serializeKeysWith(SerializationPair.fromSerializer(redisTemplate.getStringSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(redisTemplate.getValueSerializer()))
                .entryTtl(Duration.ofMinutes(30));
        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(
                Objects.requireNonNull(redisTemplate.getConnectionFactory()));
        DynaTtlRedisCacheManager cacheManager = new DynaTtlRedisCacheManager(cacheWriter, cacheConfiguration);
        cacheManager.setTransactionAware(true);
        return cacheManager;
    }

    /**
     * 双级缓存（Caffeine L1 + Redis L2）
     */
    @ConditionalOnProperty(name = "x-boot.cache.type", havingValue = CacheConst.DL)
    @ConditionalOnMissingBean
    @Bean(name = CacheConst.DL)
    public DLCacheManager dlCacheManager(CacheProperties cacheProperties, RedisTemplate<String, Object> redisTemplate) {
        return new DLCacheManager(cacheProperties.getDlCache(), redisTemplate);
    }

    @ConditionalOnProperty(name = "x-boot.cache.type", havingValue = CacheConst.DL)
    @Bean
    public DLCacheRefreshListener dlCacheRefreshListener(DLCacheManager dlCacheManager, CacheProperties cacheProperties) {
        return new DLCacheRefreshListener(dlCacheManager, cacheProperties.getDlCache());
    }
}
