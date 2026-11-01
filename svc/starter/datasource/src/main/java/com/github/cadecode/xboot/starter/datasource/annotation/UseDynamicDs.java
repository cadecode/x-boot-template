package com.github.cadecode.xboot.starter.datasource.annotation;

import java.lang.annotation.*;

/**
 * 指定动态数据源注解
 *
 * @author Cade Li
 * @date 2022/5/10
 *
 * @deprecated 由 baomidou dynamic-datasource 取代，保留仅为参考；请勿在业务中使用。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated
public @interface UseDynamicDs {
    String value();
}
