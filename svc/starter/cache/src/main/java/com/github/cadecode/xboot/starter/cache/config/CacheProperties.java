package com.github.cadecode.xboot.starter.cache.config;

import com.github.cadecode.xboot.starter.cache.cache.dl.DLCacheProperties;
import com.github.cadecode.xboot.starter.cache.enums.CacheTypeEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 缓存配置项
 *
 * @author Cade Li
 * @date 2024/5/4
 */
@Data
@ConfigurationProperties(prefix = "x-boot.cache")
public class CacheProperties {

    /**
     * cache manager 类型，可选值：REDIS（Redis 缓存）、DL（双级缓存），需按枚举名大写填写
     */
    private CacheTypeEnum type;

    @NestedConfigurationProperty
    private DLCacheProperties dlCache = new DLCacheProperties();
}
