package com.github.cadecode.xboot.starter.cache.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.github.cadecode.xboot.starter.cache.listener.RedisMessageListener;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.List;

/**
 * Redis 配置：RedisTemplate + MessageListenerContainer
 *
 * @author Cade Li
 * @date 2022/5/24
 */
@RequiredArgsConstructor
@Configuration
public class RedisConfig {

    /**
     * 自动注册所有 RedisMessageListener 实现
     */
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory factory, List<RedisMessageListener> listeners) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        listeners.forEach(o -> container.addMessageListener(o, o.topics()));
        return container;
    }

    /**
     * 全局默认 RedisTemplate，key String / value JSON
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory, ObjectMapper objectMapper) {
        // 复用 Spring 全局 ObjectMapper 配置，追加类型序列化
        ObjectMapper newObjectMapper = objectMapper.copy();
        newObjectMapper.activateDefaultTyping(
                newObjectMapper.getPolymorphicTypeValidator(), DefaultTyping.NON_FINAL, As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(newObjectMapper);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(jsonRedisSerializer);
        return template;
    }
}
