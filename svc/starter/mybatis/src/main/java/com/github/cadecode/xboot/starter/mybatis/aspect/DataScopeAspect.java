package com.github.cadecode.xboot.starter.mybatis.aspect;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.github.cadecode.xboot.common.exception.HelperException;
import com.github.cadecode.xboot.common.util.AspectUtil;
import com.github.cadecode.xboot.common.util.SpringUtil;
import com.github.cadecode.xboot.starter.mybatis.annotation.DataScope;
import com.github.cadecode.xboot.starter.mybatis.spi.DataScopeResolver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Objects;

/**
 * 数据权限注解切面
 * <p>
 * 把注解解析成 {@link DataScopeParam} 放进 ThreadLocal，SQL 改写时由
 * {@link com.github.cadecode.xboot.starter.mybatis.handler.DataScopePermissionHandler} 取用。
 *
 * @author Cade Li
 * @since 2024/5/23
 */
@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    /**
     * 栈式保存，支持"外层方法和内层方法都标了注解"的嵌套调用
     */
    private static final ThreadLocal<Deque<DataScopeParam>> DATA_SCOPE_LOCAL =
            ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * 当前请求的数据权限参数，未标注解时为 null
     */
    public static DataScopeParam currDataScope() {
        return DATA_SCOPE_LOCAL.get().peek();
    }

    @Pointcut("@within(com.github.cadecode.xboot.starter.mybatis.annotation.DataScope) "
            + "|| @annotation(com.github.cadecode.xboot.starter.mybatis.annotation.DataScope)")
    public void pointCut() {
    }

    @Before("pointCut()")
    public void before(JoinPoint point) {
        DATA_SCOPE_LOCAL.get().push(buildParam(point));
    }

    @After("pointCut()")
    public void after() {
        Deque<DataScopeParam> deque = DATA_SCOPE_LOCAL.get();
        if (!deque.isEmpty()) {
            deque.pop();
        }
        if (deque.isEmpty()) {
            DATA_SCOPE_LOCAL.remove();
        }
    }

    private DataScopeParam buildParam(JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        // 方法上的注解优先，类上的兜底；回退粒度是整条注解，不做属性合并
        DataScope dataScope = AspectUtil.requireAnnotation(point.getTarget(), signature.getMethod(), DataScope.class);
        String field = ObjUtil.defaultIfEmpty(dataScope.field(), DataScope.DEFAULT_FIELD);
        DataScopeResolver resolver = resolveResolver(dataScope.resolverName());
        log.debug("DataScope: enableFilter={}, field={}, tables={}, resolver={}",
                dataScope.enableFilter(), field, Arrays.toString(dataScope.tables()), resolver);
        return new DataScopeParam(dataScope.enableFilter(), field, dataScope.tables(), resolver);
    }

    /**
     * 取解析器：写了 bean 名按名字取；没写时用容器里唯一的实现，多个实现用标了 @Primary 的那个
     */
    private DataScopeResolver resolveResolver(String resolverName) {
        if (StrUtil.isNotBlank(resolverName)) {
            return SpringUtil.getBean(resolverName, DataScopeResolver.class);
        }
        // getIfUnique：只有一个实现时返回它；多个实现时返回 @Primary 那个；都不满足则 null
        ObjectProvider<DataScopeResolver> provider =
                SpringUtil.getBeanFactory().getBeanProvider(DataScopeResolver.class, true);
        DataScopeResolver resolver = provider.getIfUnique();
        if (Objects.nonNull(resolver)) {
            return resolver;
        }
        if (SpringUtil.getBeanNamesForType(DataScopeResolver.class).length == 0) {
            return null;
        }
        throw new HelperException("Multiple DataScopeResolver beans found, mark one with @Primary "
                + "or set resolverName on @DataScope: {}",
                Arrays.toString(SpringUtil.getBeanNamesForType(DataScopeResolver.class)));
    }

    /**
     * 一次请求的数据权限参数
     */
    @Getter
    @ToString
    @RequiredArgsConstructor
    public static class DataScopeParam {

        private final boolean enableFilter;

        /**
         * 数据范围列，可带表别名
         */
        private final String field;

        /**
         * 生效的表名，空数组表示不限制
         */
        private final String[] tables;

        private final DataScopeResolver resolver;

        public boolean matchTable(String tableName) {
            if (tables.length == 0) {
                return true;
            }
            return Arrays.stream(tables).anyMatch(t -> t.equalsIgnoreCase(tableName));
        }
    }
}
