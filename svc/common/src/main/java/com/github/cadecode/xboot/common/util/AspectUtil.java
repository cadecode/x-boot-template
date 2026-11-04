package com.github.cadecode.xboot.common.util;

import com.github.cadecode.xboot.common.exception.HelperException;
import org.springframework.aop.support.AopUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 切面取注解工具
 * <p>
 * {@code JoinPoint} 签名里的方法不一定是真正执行的方法：JDK 动态代理时它是接口方法，CGLIB 代理时才是实现方法。
 * 直接 {@code getAnnotation} 在 JDK 代理下读不到标在实现类或实现方法上的注解，表现为切面执行了却没有效果。
 * 这里统一先还原成目标类与实现方法，再读注解。
 * <p>
 * 只依赖 Spring 的静态方法 {@link AopUtils}，不触及容器。
 *
 * @author Cade Li
 * @since 2024/6/3
 */
public class AspectUtil {

    private AspectUtil() {
    }

    /**
     * 从目标类上取注解
     *
     * @param target          切点目标对象；不经过代理的调用方（如 {@code ResponseBodyAdvice}）可直接传目标类
     * @param signatureMethod {@code JoinPoint} 签名里的方法
     * @param annotationType  注解类型
     * @return 没有标注解时返回 null
     */
    public static <A extends Annotation> A classAnnotation(Object target, Method signatureMethod,
                                                           Class<A> annotationType) {
        return resolveType(target, signatureMethod).getAnnotation(annotationType);
    }

    /**
     * 从目标方法上取注解
     *
     * @param target          切点目标对象；不经过代理的调用方（如 {@code ResponseBodyAdvice}）可直接传目标类
     * @param signatureMethod {@code JoinPoint} 签名里的方法
     * @param annotationType  注解类型
     * @return 没有标注解时返回 null
     */
    public static <A extends Annotation> A methodAnnotation(Object target, Method signatureMethod,
                                                            Class<A> annotationType) {
        return resolveMethod(resolveType(target, signatureMethod), signatureMethod).getAnnotation(annotationType);
    }

    /**
     * 取注解，方法上的优先、类上的兜底
     *
     * @param target          切点目标对象；不经过代理的调用方（如 {@code ResponseBodyAdvice}）可直接传目标类
     * @param signatureMethod {@code JoinPoint} 签名里的方法
     * @param annotationType  注解类型
     * @return 方法上和类上都没有时返回 null
     */
    public static <A extends Annotation> A findAnnotation(Object target, Method signatureMethod,
                                                         Class<A> annotationType) {
        Class<?> type = resolveType(target, signatureMethod);
        A annotation = resolveMethod(type, signatureMethod).getAnnotation(annotationType);
        return Objects.nonNull(annotation) ? annotation : type.getAnnotation(annotationType);
    }

    /**
     * 取注解，取不到直接报错，供「切点命中即必然存在注解」的场景使用
     *
     * @param target          切点目标对象；不经过代理的调用方（如 {@code ResponseBodyAdvice}）可直接传目标类
     * @param signatureMethod {@code JoinPoint} 签名里的方法
     * @param annotationType  注解类型
     */
    public static <A extends Annotation> A requireAnnotation(Object target, Method signatureMethod,
                                                             Class<A> annotationType) {
        A annotation = findAnnotation(target, signatureMethod, annotationType);
        if (Objects.isNull(annotation)) {
            throw new HelperException("{} annotation not found on {}",
                    annotationType.getSimpleName(), signatureMethod);
        }
        return annotation;
    }

    private static Class<?> resolveType(Object target, Method signatureMethod) {
        // 直接传类：不经过代理的调用方（如 MethodParameter#getContainingClass）
        if (target instanceof Class<?> type) {
            return type;
        }
        return Objects.isNull(target)
                ? signatureMethod.getDeclaringClass()
                : AopUtils.getTargetClass(target);
    }

    private static Method resolveMethod(Class<?> type, Method signatureMethod) {
        return AopUtils.getMostSpecificMethod(signatureMethod, type);
    }
}
