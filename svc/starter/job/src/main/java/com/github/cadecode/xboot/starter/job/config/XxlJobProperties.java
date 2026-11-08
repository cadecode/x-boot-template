package com.github.cadecode.xboot.starter.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * xxl-job 执行器配置，字段名与官方 sample 的 key 一致
 *
 * @author Cade Li
 * @since 2026/11/7
 */
@Data
@ConfigurationProperties("xxl.job")
public class XxlJobProperties {

    /**
     * 是否启用执行器
     */
    private boolean enabled;

    /**
     * 调度中心通讯 token，3.x 起必填
     */
    private String accessToken;

    private Admin admin = new Admin();

    private Executor executor = new Executor();

    @Data
    public static class Admin {

        /**
         * 调度中心地址，多个用逗号分隔，不要带 /api 后缀
         */
        private String addresses;

    }

    @Data
    public static class Executor {

        private String appname;

        /**
         * 执行器注册地址，留空则用 http://ip:port/
         */
        private String address;

        private String ip;

        /**
         * 执行器端口，0 表示从 9999 起找可用端口
         */
        private int port;

        private String logpath;

        /**
         * 日志保留天数，不配置则不覆盖框架默认值 30
         */
        private Integer logretentiondays;

    }

}
