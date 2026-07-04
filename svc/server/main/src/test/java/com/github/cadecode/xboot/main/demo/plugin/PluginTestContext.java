package com.github.cadecode.xboot.main.demo.plugin;

import com.github.cadecode.xboot.common.enums.ExtensionType;
import com.github.cadecode.xboot.common.extension.plugin.PluginContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Plugin Demo 上下文
 *
 * @author Cade Li
 * @since 2026/7/4
 */
@Getter
@RequiredArgsConstructor
public class PluginTestContext implements PluginContext {
    private final ExtensionType pluginType;
}
