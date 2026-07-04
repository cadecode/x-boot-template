package com.github.cadecode.xboot.main.demo.plugin;

import com.github.cadecode.xboot.common.enums.ExtensionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Plugin Demo 无匹配类型（用于验证抛异常场景）
 *
 * @author Cade Li
 * @since 2026/7/4
 */
@Getter
@RequiredArgsConstructor
public class PluginTestUnknownType implements ExtensionType {
    private final String type;
}
