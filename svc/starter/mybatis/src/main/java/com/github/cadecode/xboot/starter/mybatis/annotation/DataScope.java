package com.github.cadecode.xboot.starter.mybatis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解
 * <p>
 * 标在类或方法上，本次请求内的 SQL 会按 {@link com.github.cadecode.xboot.starter.mybatis.spi.DataScopeResolver}
 * 返回的范围追加 {@code field IN (...)} 条件。方法上的注解优先，字段级配置回退到类上的注解。
 * <p>
 * 注意：切点是 {@code @within || @annotation}，按<b>目标类</b>匹配，所以注解要标在实现类（或实现类的方法）上；
 * 只标在接口上不会命中切面。被 JDK 代理时方法签名拿到的是接口方法，切面内部会还原到实现类方法再读注解。
 * <p>
 * 只对 SQL 里出现的表名命中 {@link #tables()} 的查询生效；{@code tables} 留空表示所有表都生效，
 * 这时要保证被查询的每张表都有 {@link #field()} 这一列，否则会报 {@code Unknown column}。
 *
 * @author Cade Li
 * @since 2024/5/23
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 默认数据范围列
     */
    String DEFAULT_FIELD = "data_scope";

    /**
     * 是否启用过滤，false 时本次请求不追加条件
     */
    boolean enableFilter() default true;

    /**
     * 数据范围列，可带表别名，如 {@code u.dept_id}；留空用 {@link #DEFAULT_FIELD}
     */
    String field() default "";

    /**
     * 只对这些表生效（表名，不区分大小写）；留空表示不限制
     */
    String[] tables() default {};

    /**
     * 解析器 bean 名称，留空用容器里唯一的 {@code DataScopeResolver}
     */
    String resolverName() default "";
}
