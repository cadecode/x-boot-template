package com.github.cadecode.xboot.starter.job.config;

import cn.hutool.core.util.StrUtil;
import com.github.cadecode.xboot.starter.job.util.XxlJobAdminKit;
import com.xxl.job.core.constant.Const;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.openapi.admin.AdminJobBiz;
import com.xxl.tool.http.HttpTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * xxl-job 自动配置
 *
 * @author Cade Li
 * @since 2026/11/7
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(XxlJobProperties.class)
public class XxlJobAutoConfig {

    /**
     * 调度中心通讯超时，与执行器默认值保持一致
     */
    private static final int TIMEOUT_SECONDS = 3;

    private final XxlJobProperties properties;

    @ConditionalOnProperty(name = "xxl.job.enabled", havingValue = "true")
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
        // 只在配置时覆盖，否则 set(null) 会压掉框架默认日志目录，启动时报 logPath cannot be empty
        if (StrUtil.isNotBlank(executor.getLogpath())) {
            xxlJobSpringExecutor.setLogPath(executor.getLogpath());
        }
        // 不配置时不调用，保留框架默认值；传 0 会关掉日志清理
        if (executor.getLogretentiondays() != null) {
            xxlJobSpringExecutor.setLogRetentionDays(executor.getLogretentiondays());
        }
        return xxlJobSpringExecutor;
    }

    /**
     * 调度中心任务管理，只依赖调度中心地址，不要求启用执行器
     */
    @ConditionalOnProperty(name = "xxl.job.admin.addresses")
    @ConditionalOnMissingBean
    @Bean
    public XxlJobAdminKit xxlJobAdminKit() {
        // 与 XxlJobExecutor 一致：地址末尾拼 /api；多个地址只取第一个
        String address = properties.getAdmin().getAddresses().split(",")[0].trim();
        String baseUrl = address.endsWith("/") ? address + "api" : address + "/api";
        log.info("Create XxlJobAdminKit, baseUrl: {}", baseUrl);
        AdminJobBiz adminJobBiz = HttpTool.createClient()
                .url(baseUrl)
                .connectTimeout(TIMEOUT_SECONDS * 1000)
                .readTimeout(TIMEOUT_SECONDS * 1000)
                .header(Const.XXL_JOB_ACCESS_TOKEN, properties.getAccessToken())
                .header(Const.XXL_JOB_APPNAME, properties.getExecutor().getAppname())
                .proxy(AdminJobBiz.class);
        return new XxlJobAdminKit(adminJobBiz);
    }

}
