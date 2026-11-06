package com.github.cadecode.xboot.main.common.util;

import com.github.cadecode.xboot.common.exception.HelperException;
import com.github.cadecode.xboot.common.util.AspectUtil;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AspectUtil} 的测试，重点是 JDK 代理下能否读到实现类 / 实现方法上的注解
 */
class AspectUtilTests {

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Demo {
        String value() default "";
    }

    interface DemoApi {
        String hello();

        String plain();
    }

    @Demo("classLevel")
    static class DemoImpl implements DemoApi {

        @Demo("methodLevel")
        @Override
        public String hello() {
            return "hello";
        }

        @Override
        public String plain() {
            return "plain";
        }
    }

    static class NoAnnotation {
        public String go() {
            return "go";
        }
    }

    @Test
    void methodAnnotationFindsImplMethodAnnotation() throws Exception {
        // 签名方法是接口方法、target 是实现类实例 —— 这正是 JDK 代理下的形态
        Demo demo = AspectUtil.methodAnnotation(new DemoImpl(), DemoApi.class.getMethod("hello"), Demo.class);
        assertEquals("methodLevel", demo.value());
    }

    @Test
    void classAnnotationFindsImplClassAnnotation() throws Exception {
        Demo demo = AspectUtil.classAnnotation(new DemoImpl(), DemoApi.class.getMethod("hello"), Demo.class);
        assertEquals("classLevel", demo.value());
    }

    @Test
    void findAnnotationPrefersMethod() throws Exception {
        Demo demo = AspectUtil.findAnnotation(new DemoImpl(), DemoApi.class.getMethod("hello"), Demo.class);
        assertEquals("methodLevel", demo.value());
    }

    @Test
    void findAnnotationFallsBackToClass() throws Exception {
        // plain() 上没有注解，应回落到类上的
        Demo demo = AspectUtil.findAnnotation(new DemoImpl(), DemoApi.class.getMethod("plain"), Demo.class);
        assertEquals("classLevel", demo.value());
    }

    @Test
    void classAnnotationReturnsNullWhenAbsent() throws Exception {
        Demo demo = AspectUtil.classAnnotation(new NoAnnotation(), NoAnnotation.class.getMethod("go"), Demo.class);
        assertNull(demo);
    }

    @Test
    void requireAnnotationThrowsWhenAbsent() throws Exception {
        HelperException ex = assertThrows(HelperException.class, () -> AspectUtil.requireAnnotation(
                new NoAnnotation(), NoAnnotation.class.getMethod("go"), Demo.class));
        System.out.println("[AspectUtil] 缺失注解时抛出: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Demo annotation not found"));
    }

    @Test
    void targetCanBeClassForNonProxyCaller() throws Exception {
        // ResponseBodyAdvice 这类不经过代理的调用方直接传目标类
        Demo demo = AspectUtil.findAnnotation(DemoImpl.class, DemoImpl.class.getMethod("hello"), Demo.class);
        assertEquals("methodLevel", demo.value());
    }

    @Test
    void nullTargetUsesDeclaringClass() throws Exception {
        Demo demo = AspectUtil.findAnnotation(null, DemoImpl.class.getMethod("hello"), Demo.class);
        assertEquals("methodLevel", demo.value());
    }

    @Test
    void jdkProxyTargetIsUnwrapped() throws Exception {
        ProxyFactory factory = new ProxyFactory(new DemoImpl());
        factory.setInterfaces(DemoApi.class);
        factory.setProxyTargetClass(false);
        Object proxy = factory.getProxy();

        Demo demo = AspectUtil.findAnnotation(proxy, DemoApi.class.getMethod("hello"), Demo.class);
        assertEquals("methodLevel", demo.value());
        System.out.println("[AspectUtil] 代理类=" + proxy.getClass().getName() + " → 读到注解值=" + demo.value());
    }

    @Test
    void cglibProxyTargetIsUnwrapped() throws Exception {
        ProxyFactory factory = new ProxyFactory(new DemoImpl());
        factory.setProxyTargetClass(true);
        Object proxy = factory.getProxy();

        Demo demo = AspectUtil.findAnnotation(proxy, DemoApi.class.getMethod("hello"), Demo.class);
        assertEquals("methodLevel", demo.value());
        System.out.println("[AspectUtil] 代理类=" + proxy.getClass().getName() + " → 读到注解值=" + demo.value());
    }
}
