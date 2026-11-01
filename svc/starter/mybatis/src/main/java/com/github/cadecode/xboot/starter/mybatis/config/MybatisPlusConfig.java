package com.github.cadecode.xboot.starter.mybatis.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.github.cadecode.xboot.starter.mybatis.handler.BaseEntityFillHandler;
import com.github.cadecode.xboot.starter.mybatis.spi.EntityFillResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 * <p>
 * 分页统一交给 PageHelper（见 {@link PageHelperConfig}）：不引 mybatis-plus-jsqlparser、
 * 不注册 PaginationInnerInterceptor，因此 Mapper 的 selectPage(IPage) 不会被分页
 * （返回全表、total 为 0），业务请用 PageHelper。
 *
 * @author Cade Li
 * @since 2024/5/20
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * 公共字段填充器
     * <p>
     * MP 按类型取 MetaObjectHandler，容器中只能有一个（多个需 @Primary）
     */
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    @Bean
    public MetaObjectHandler baseEntityFillHandler(ObjectProvider<EntityFillResolver> entityFillResolver) {
        return new BaseEntityFillHandler(entityFillResolver.getIfAvailable());
    }

    /**
     * 未接入用户模块时的空实现，业务侧提供实现即可覆盖
     */
    @ConditionalOnMissingBean(EntityFillResolver.class)
    @Bean
    public EntityFillResolver entityFillResolver() {
        return () -> null;
    }
}
