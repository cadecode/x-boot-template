package com.github.cadecode.xboot.starter.cache.config;

import com.github.cadecode.xboot.starter.cache.cache.dl.DLCacheProperties;
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
     * cache manager 类型，对应 CacheConst 中的常量值
     */
    private String type;

    @NestedConfigurationProperty
    private DLCacheProperties dlCache = new DLCacheProperties();
}
