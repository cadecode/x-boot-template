package com.github.cadecode.xboot.starter.cache.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.convert.DurationStyle;

import java.time.Duration;

/**
 * 动态 TTL 缓存名解析器
 * <p>
 * 协议：cacheName#<duration>，duration 使用 Spring DurationStyle 语法（与配置绑定一致）
 * 示例：user#6 → 6ms, product#5m → 5min, dict#2h → 2h, order#500ms → 500ms, dict#1d → 1d
 * <p>
 * 注意：以最后一个 # 分割，缓存名末尾的 #数字 会被解析为 TTL，如 order#2024 → order + 2024ms，
 * 业务缓存名含 # 时需注意避免歧义；解析失败视为无后缀（返回 null）
 *
 * @author Cade Li
 * @date 2024/7/24
 */
public final class DynaTtlNameUtil {

    private DynaTtlNameUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * 解析缓存名，不带后缀或解析失败返回 null
     */
    public static ParsedName parse(String name) {
        int idx = name.lastIndexOf('#');
        if (idx <= 0 || idx == name.length() - 1) {
            return null;
        }
        String realName = name.substring(0, idx);
        String ttlPart = name.substring(idx + 1);
        Duration ttl;
        try {
            ttl = DurationStyle.detectAndParse(ttlPart);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return new ParsedName(realName, ttl);
    }

    /**
     * 剥离后缀后的缓存名与 TTL
     */
    @Getter
    @AllArgsConstructor
    public static class ParsedName {
        private final String name;
        private final Duration ttl;
    }
}
