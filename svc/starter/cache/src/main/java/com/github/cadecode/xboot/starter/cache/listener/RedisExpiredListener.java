package com.github.cadecode.xboot.starter.cache.listener;

import cn.hutool.core.util.ObjUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.Topic;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis key 过期监听器
 *
 * @author Cade Li
 * @date 2023/6/12
 */
@Slf4j
@Component
@ConditionalOnBean(RedisExpiredHandler.class)
public class RedisExpiredListener extends RedisMessageListener {

    private final List<RedisExpiredHandler> expiredHandlers;
    private final ConcurrentHashMap<String, RedisExpiredHandler> handlerMap = new ConcurrentHashMap<>();

    public RedisExpiredListener(List<RedisExpiredHandler> expiredHandlers) {
        this.expiredHandlers = expiredHandlers;
    }

    @Override
    public List<Topic> topics() {
        return Collections.singletonList(new PatternTopic("__keyevent@*__:expired"));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = new String(message.getBody());
        RedisExpiredHandler handler;

        if (handlerMap.containsKey(key)) {
            handler = handlerMap.get(key);
        } else {
            // 按 @Order 排序，取第一个 checkKey 通过的处理器
            Optional<RedisExpiredHandler> handlerOpt = expiredHandlers.stream()
                    .filter(o -> o.checkKey(key))
                    .sorted(Comparator.comparing(o -> {
                        Order order = o.getClass().getAnnotation(Order.class);
                        return ObjUtil.defaultIfNull(order, Order::value, 0);
                    }).reversed())
                    .findAny();
            if (handlerOpt.isEmpty()) {
                return;
            }
            handler = handlerOpt.get();
            handlerMap.put(key, handler);
        }
        log.info("Redis expired listener find key {}, handler {}", key, handler.getClass().getName());
        try {
            handler.handle(key);
        } catch (Exception e) {
            log.error("Redis expired listener handler failed, key {}, handler {}", key, handler.getClass().getName(), e);
        }
    }
}
