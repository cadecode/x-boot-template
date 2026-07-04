package com.github.cadecode.xboot.main.demo.plugin;

import com.github.cadecode.xboot.common.enums.ExtensionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Plugin Demo 类型枚举
 *
 * @author Cade Li
 * @since 2026/7/4
 */
@Getter
@RequiredArgsConstructor
public enum PluginTestType implements ExtensionType {
    TYPE_A("A"),
    TYPE_B("B"),
    ;

    private final String type;
}
