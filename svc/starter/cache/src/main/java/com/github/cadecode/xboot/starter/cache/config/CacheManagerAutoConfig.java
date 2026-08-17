package com.github.cadecode.xboot.starter.cache.config;

import com.github.cadecode.xboot.starter.cache.cache.dl.DLCacheManager;
import com.github.cadecode.xboot.starter.cache.cache.dl.DLCacheRefreshListener;
import com.github.cadecode.xboot.starter.cache.cache.redis.DynaTtlRedisCacheManager;
import com.github.cadecode.xboot.starter.cache.constant.CacheConst;
import com.github.cadecode.xboot.starter.cache.util.KeyGeneUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.Objects;

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

    @ConditionalOnProperty(name = "x-boot.cache.type", havingValue = CacheConst.TYPE_REDIS)
    @Bean(name = CacheConst.MANAGER_REDIS)
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
    @ConditionalOnProperty(name = "x-boot.cache.type", havingValue = CacheConst.TYPE_DL)
    @ConditionalOnMissingBean
    @Bean(name = CacheConst.MANAGER_DL)
    public DLCacheManager dlCacheManager(CacheProperties cacheProperties, RedisTemplate<String, Object> redisTemplate) {
        return new DLCacheManager(cacheProperties.getDlCache(), redisTemplate);
    }

    @ConditionalOnProperty(name = "x-boot.cache.type", havingValue = CacheConst.TYPE_DL)
    @Bean
    public DLCacheRefreshListener dlCacheRefreshListener(DLCacheManager dlCacheManager, CacheProperties cacheProperties) {
        return new DLCacheRefreshListener(dlCacheManager, cacheProperties.getDlCache());
    }
}
