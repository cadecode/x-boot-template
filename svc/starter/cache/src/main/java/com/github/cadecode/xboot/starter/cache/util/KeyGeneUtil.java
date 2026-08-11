package com.github.cadecode.xboot.starter.cache.util;

/**
 * Redis key 生成工具
 *
 * @author Cade Li
 * @date 2022/5/29
 */
public final class KeyGeneUtil {

    public static final String SEPARATOR = ":";

    private KeyGeneUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * 生成用冒号分隔的 Redis key
     */
    public static String key(String prefix, Object... extra) {
        StringBuilder sb = new StringBuilder(prefix);
        for (Object obj : extra) {
            sb.append(SEPARATOR).append(obj);
        }
        return sb.toString();
    }

    /**
     * 生成分布式锁 key
     */
    public static String lockKey(Object... extra) {
        return key("lock", extra);
    }
}
