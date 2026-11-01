package com.github.cadecode.xboot.starter.datasource.config;

import com.zaxxer.hikari.HikariConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * HikariDataSource 的动态数据源配置
 *
 * @author Cade Li
 * @since 2023/6/8
 *
 * @deprecated 由 baomidou dynamic-datasource 取代，保留仅为参考；请勿在业务中使用。
 */
@Data
@ConfigurationProperties("x-boot.dynamic-ds")
@Deprecated
public class DynamicDsProperties {

    private boolean enable;

    /**
     * 主数据源 KEY
     */
    private String master;

    /**
     * 数据源 MAP
     */
    private Map<String, HikariConfig> datasource;

}
