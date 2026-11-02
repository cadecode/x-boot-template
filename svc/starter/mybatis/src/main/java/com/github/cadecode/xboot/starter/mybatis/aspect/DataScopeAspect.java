package com.github.cadecode.xboot.starter.mybatis.aspect;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
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
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
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
        Class<?> targetClass = Objects.isNull(point.getTarget())
                ? signature.getMethod().getDeclaringClass()
                : AopUtils.getTargetClass(point.getTarget());
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(), targetClass);
        DataScope methodAnn = AnnotatedElementUtils.findMergedAnnotation(method, DataScope.class);
        DataScope classAnn = AnnotatedElementUtils.findMergedAnnotation(targetClass, DataScope.class);
        if (Objects.isNull(methodAnn) && Objects.isNull(classAnn)) {
            return null;
        }
        boolean enableFilter = Objects.nonNull(methodAnn) ? methodAnn.enableFilter() : classAnn.enableFilter();
        // 方法上的配置优先，类上的兜底
        String field = ObjUtil.defaultIfEmpty(fieldOf(methodAnn),
                ObjUtil.defaultIfEmpty(fieldOf(classAnn), DataScope.DEFAULT_FIELD));
        String[] tables = Objects.nonNull(methodAnn) && methodAnn.tables().length > 0
                ? methodAnn.tables()
                : (Objects.isNull(classAnn) ? new String[0] : classAnn.tables());
        String resolverName = ObjUtil.defaultIfEmpty(resolverNameOf(methodAnn), resolverNameOf(classAnn));
        DataScopeResolver resolver = resolveResolver(resolverName);
        log.debug("DataScope: enableFilter={}, field={}, tables={}, resolver={}",
                enableFilter, field, Arrays.toString(tables), resolver);
        return new DataScopeParam(enableFilter, field, tables, resolver);
    }

    /**
     * 按 bean 名取解析器；未指定名字时取容器里唯一的实现
     */
    private DataScopeResolver resolveResolver(String resolverName) {
        if (StrUtil.isNotBlank(resolverName)) {
            return SpringUtil.getBean(resolverName, DataScopeResolver.class);
        }
        Collection<DataScopeResolver> resolvers = SpringUtil.getBeansOfType(DataScopeResolver.class).values();
        if (CollUtil.isEmpty(resolvers)) {
            return null;
        }
        if (resolvers.size() > 1) {
            throw new IllegalStateException("存在多个 DataScopeResolver，请在 @DataScope 上指定 resolverName："
                    + Arrays.toString(SpringUtil.getBeanNamesForType(DataScopeResolver.class)));
        }
        return resolvers.iterator().next();
    }

    private static String fieldOf(DataScope ann) {
        return Objects.isNull(ann) ? null : ann.field();
    }

    private static String resolverNameOf(DataScope ann) {
        return Objects.isNull(ann) ? null : ann.resolverName();
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
