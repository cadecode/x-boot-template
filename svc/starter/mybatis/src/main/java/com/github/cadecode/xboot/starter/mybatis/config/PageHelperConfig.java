package com.github.cadecode.xboot.starter.mybatis.config;

import com.github.pagehelper.PageInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * PageHelper 配置
 * <p>
 * 参数直接写死，不走 yml（同 old-dev）：分页参数基本不会动，少一层绑定就少一处出错的地方；
 * 需要临时调整时用 {@code PageHelper.startPage(pageNum, pageSize, count)} 单次覆盖。
 * <p>
 * 注意 {@code autoDialect} 不是可配项（源码里只有内部字段，由 {@code helperDialect} /
 * {@code autoRuntimeDialect} 推导），写了也不生效，因此这里不声明它。
 * <p>
 * MybatisPlusAutoConfiguration 会把容器里的 Interceptor bean 全部 setPlugins 到 SqlSessionFactory，
 * 这里声明成 bean 即可生效，不需要手动往 SqlSessionFactory 里塞。
 *
 * @author Cade Li
 * @since 2024/5/20
 */
@Slf4j
@Configuration
public class PageHelperConfig {

    @ConditionalOnMissingBean
    @Bean
    public PageInterceptor pageInterceptor() {
        PageInterceptor pageInterceptor = new PageInterceptor();
        Properties properties = new Properties();
        // pageNum<=0 查第一页，pageNum 超过总页数查最后一页
        properties.setProperty("reasonable", "true");
        // 支持通过 Mapper 方法参数（pageNum/pageSize）传递分页参数
        properties.setProperty("supportMethodsArguments", "true");
        // 分页参数映射（默认已包含 count=countSql，这里写出来作为显式声明）
        properties.setProperty("params", "count=countSql");
        // 多数据源场景下按运行时连接识别方言
        properties.setProperty("autoRuntimeDialect", "true");
        // pageSize=0 不查全部，避免误传 0 时全表扫描；需要时用 startPage(pageNum, pageSize, true)
        properties.setProperty("pageSizeZero", "false");
        pageInterceptor.setProperties(properties);
        log.info("Create PageInterceptor with properties {}", properties);
        return pageInterceptor;
    }
}
