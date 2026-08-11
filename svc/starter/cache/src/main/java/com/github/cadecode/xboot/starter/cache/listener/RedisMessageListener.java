package com.github.cadecode.xboot.starter.cache.listener;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.Topic;

import java.util.List;

/**
 * Redis 消息监听器抽象基类
 * 子类实现 topics() 即可自动注册到 RedisMessageListenerContainer
 *
 * @author Cade Li
 * @date 2023/6/14
 */
public abstract class RedisMessageListener implements MessageListener {

    /**
     * 返回需要监听的 Topic 列表
     */
    public abstract List<Topic> topics();
}
