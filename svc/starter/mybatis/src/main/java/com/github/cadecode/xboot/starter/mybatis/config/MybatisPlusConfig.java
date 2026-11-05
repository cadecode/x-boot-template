package com.github.cadecode.xboot.starter.mybatis.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.github.cadecode.xboot.starter.mybatis.handler.BaseEntityFillHandler;
import com.github.cadecode.xboot.starter.mybatis.handler.DataScopePermissionHandler;
import com.github.cadecode.xboot.starter.mybatis.interceptor.DataScopePermissionInterceptor;
import com.github.cadecode.xboot.starter.mybatis.spi.DataScopeResolver;
import com.github.cadecode.xboot.starter.mybatis.spi.EntityFillResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * MyBatis-Plus 配置
 * <p>
 * 数据权限用 {@link DataScopePermissionInterceptor}（MP {@code DataPermissionInterceptor} 的子类，
 * 没标注解时跳过 SQL 解析）：SQL 里的每张表都会回调
 * {@link DataScopePermissionHandler}，只有标注了 {@code @DataScope} 且命中表名时才追加条件。
 * <p>
 * 拦截器顺序：MyBatis {@code InterceptorChain.pluginAll} 从前往后逐个包装，<b>列表靠后者在最外层、先执行</b>；
 * Spring 注入 {@code Interceptor[]} 时按 {@code @Order} 升序排列，所以数值大的在外层。
 * 这里给 MP 的 {@code MybatisPlusInterceptor} 标 {@code @Order(100)}、分页的 {@code PageInterceptor} 标
 * {@code @Order(0)}，保证数据权限先改写 SQL，分页据此生成的 count 也带权限条件。
 *
 * @author Cade Li
 * @since 2024/5/20
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * MP 插件链（当前只有数据权限）
     * <p>
     * 后续接入 MP 分页时，在这里 addInnerInterceptor 把 {@code PaginationInnerInterceptor} 加上即可。
     */
    @Order(100)
    @ConditionalOnMissingBean
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new DataScopePermissionInterceptor(new DataScopePermissionHandler()));
        log.info("Create MybatisPlusInterceptor with DataScopePermissionInterceptor");
        return interceptor;
    }

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

    /**
     * 未接入用户模块时的空实现：不限制数据范围
     */
    @ConditionalOnMissingBean(DataScopeResolver.class)
    @Bean
    public DataScopeResolver dataScopeResolver() {
        return () -> null;
    }
}
