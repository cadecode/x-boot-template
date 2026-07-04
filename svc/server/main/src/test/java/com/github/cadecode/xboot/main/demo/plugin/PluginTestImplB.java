package com.github.cadecode.xboot.main.demo.plugin;

import com.github.cadecode.xboot.common.extension.plugin.PluginContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Plugin Demo 实现 B — 匹配 TYPE_B
 *
 * @author Cade Li
 * @since 2026/7/4
 */
@Component
@RequiredArgsConstructor
public class PluginTestImplB implements PluginTestService {

    @Override
    public boolean supports(PluginContext context) {
        return PluginTestType.TYPE_B.equals(context.getPluginType());
    }

    @Override
    public String doSomething() {
        return "implB-result";
    }
}
