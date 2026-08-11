package com.github.cadecode.xboot.starter.cache.listener;

/**
 * Redis key 过期处理器 SPI
 *
 * @author Cade Li
 * @date 2023/6/12
 */
public interface RedisExpiredHandler {

    /**
     * 检查是否由当前处理器处理该 key
     */
    boolean checkKey(String key);

    /**
     * 处理过期事件
     */
    void handle(String key);
}
