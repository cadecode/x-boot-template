package com.github.cadecode.xboot.starter.job.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * xxl-job 执行器自动配置
 *
 * @author Cade Li
 * @since 2026/11/7
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(XxlJobProperties.class)
@ConditionalOnProperty(name = "xxl.job.enabled", havingValue = "true")
public class XxlJobAutoConfig {

    private final XxlJobProperties properties;

    @ConditionalOnMissingBean
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobProperties.Admin admin = properties.getAdmin();
        XxlJobProperties.Executor executor = properties.getExecutor();
        log.info("Create XxlJobSpringExecutor, appname: {}, adminAddresses: {}, port: {}",
                executor.getAppname(), admin.getAddresses(), executor.getPort());
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(admin.getAddresses());
        xxlJobSpringExecutor.setAccessToken(properties.getAccessToken());
        xxlJobSpringExecutor.setAppname(executor.getAppname());
        xxlJobSpringExecutor.setAddress(executor.getAddress());
        xxlJobSpringExecutor.setIp(executor.getIp());
        xxlJobSpringExecutor.setPort(executor.getPort());
        xxlJobSpringExecutor.setLogPath(executor.getLogpath());
        // 不配置时不调用，保留框架默认值；传 0 会关掉日志清理
        if (executor.getLogretentiondays() != null) {
            xxlJobSpringExecutor.setLogRetentionDays(executor.getLogretentiondays());
        }
        return xxlJobSpringExecutor;
    }

}
