package com.github.cadecode.xboot.starter.datasource.annotation;

import java.lang.annotation.*;

/**
 * 指定动态数据源注解
 * <p>
 * 可标在类或方法上，方法上的注解优先、类上的兜底。
 *
 * @author Cade Li
 * @date 2022/5/10
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DynamicDs {
    String value();
}
