package com.github.cadecode.xboot.main.demo.plugin;

import com.github.cadecode.xboot.common.exception.ExtensionException;
import com.github.cadecode.xboot.common.extension.plugin.PluginContext;
import com.github.cadecode.xboot.common.extension.plugin.PluginExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plugin Demo 测试
 *
 * @author Cade Li
 * @since 2026/7/4
 */
@SpringBootTest
public class PluginTests {

    @Autowired
    private PluginExecutor pluginExecutor;

    @Test
    @DisplayName("匹配 TYPE_A → implA")
    void testMatchTypeA() {
        PluginContext context = new PluginTestContext(PluginTestType.TYPE_A);

        String result = pluginExecutor.submit(PluginTestService.class, context, PluginTestService::doSomething);

        assertEquals("implA-result", result);
    }

    @Test
    @DisplayName("匹配 TYPE_B → implB")
    void testMatchTypeB() {
        PluginContext context = new PluginTestContext(PluginTestType.TYPE_B);

        String result = pluginExecutor.submit(PluginTestService.class, context, PluginTestService::doSomething);

        assertEquals("implB-result", result);
    }

    @Test
    @DisplayName("无匹配类型 → 抛 ExtensionException")
    void testNoMatch() {
        PluginContext context = new PluginTestContext(new PluginTestUnknownType("UNKNOWN"));

        assertThrows(ExtensionException.class, () ->
                pluginExecutor.submit(PluginTestService.class, context, PluginTestService::doSomething));
    }
}
