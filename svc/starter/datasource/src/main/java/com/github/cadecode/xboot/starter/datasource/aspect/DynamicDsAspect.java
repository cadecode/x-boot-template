package com.github.cadecode.xboot.starter.datasource.aspect;

import com.github.cadecode.xboot.common.util.AspectUtil;
import com.github.cadecode.xboot.starter.datasource.annotation.DynamicDs;
import com.github.cadecode.xboot.starter.datasource.dynamic.DynamicDsHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 动态数据源切换 AOP 类
 *
 * @author Cade Li
 * @date 2021/12/3
 */
@Slf4j
@Aspect
@Component
@Order(-1)
public class DynamicDsAspect {

    @Pointcut("@within(com.github.cadecode.xboot.starter.datasource.annotation.DynamicDs) " +
            "|| @annotation(com.github.cadecode.xboot.starter.datasource.annotation.DynamicDs)")
    public void pointCut() {

    }

    /**
     * 切换数据源
     * <p>
     * 用 {@code @Around} 而不是 {@code @Before} + {@code @After}：压栈和弹栈必须在同一个方法里成对。
     * 分成两个通知时，key 不存在的那次不压栈，但 {@code @After} 照样弹栈，会把外层的 key 弹掉，
     * 后续 SQL 就落回默认库。
     */
    @Around("pointCut()")
    public Object switchDataSource(ProceedingJoinPoint point) throws Throwable {
        // 获取方法上的注解
        String dataSourceKey = getDataSourceKey(point);
        // key 不在配置里就不切换，原样执行
        if (!DynamicDsHolder.containDataSourceKey(dataSourceKey)) {
            log.info("Switch datasource fail，{} not found", dataSourceKey);
            return point.proceed();
        }
        DynamicDsHolder.setDataSourceKey(dataSourceKey);
        String methodName = point.getSignature().getDeclaringTypeName() + '.' + point.getSignature().getName();
        log.info("Switch datasource to {}，execute method [{}]", dataSourceKey, methodName);
        try {
            return point.proceed();
        } finally {
            // 将数据源恢复为之前的数据源
            DynamicDsHolder.clearDataSourceKey();
            log.info("Reset datasource to {}", DynamicDsHolder.getDataSourceKey());
        }
    }

    /**
     * 获取 DynamicDs 注解内容，以方法为主
     */
    private String getDataSourceKey(JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        // 方法上的注解优先，类上的兜底
        DynamicDs dynamicDs = AspectUtil.requireAnnotation(point.getTarget(), signature.getMethod(), DynamicDs.class);
        return dynamicDs.value();
    }
}
