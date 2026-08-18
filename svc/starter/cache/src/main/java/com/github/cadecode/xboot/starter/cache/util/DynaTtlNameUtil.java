package com.github.cadecode.xboot.starter.cache.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动态 TTL 缓存名解析器
 * <p>
 * 协议：cacheName#<数字><单位>，单位默认为秒
 * 示例：user#6 → 6s, product#5m → 5min, dict#2h → 2h
 *
 * @author Cade Li
 * @date 2024/7/24
 */
public final class DynaTtlNameUtil {

    private static final Pattern TTL_SUFFIX = Pattern.compile("^(.+?)#(\\d+)([smh])?$");

    private DynaTtlNameUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * 解析缓存名，不带后缀或解析失败返回 null
     */
    public static ParsedName parse(String name) {
        Matcher m = TTL_SUFFIX.matcher(name);
        if (!m.matches()) {
            return null;
        }
        String realName = m.group(1);
        long value = Long.parseLong(m.group(2));
        String unit = m.group(3);
        // 无单位后缀时 group(3) 为 null，Java 17 switch 对 null 抛 NPE，需先行判断
        Duration ttl = Objects.isNull(unit)
                ? Duration.ofSeconds(value)
                : switch (unit) {
                    case "m" -> Duration.ofMinutes(value);
                    case "h" -> Duration.ofHours(value);
                    default -> Duration.ofSeconds(value);
                };
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
