package com.github.cadecode.xboot.starter.cache.l2cache.sync;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.github.cadecode.xboot.starter.cache.l2cache.DLCacheProperties;
import com.github.cadecode.xboot.starter.cache.l2cache.cache.DLCache;
import com.github.cadecode.xboot.starter.cache.l2cache.cache.DLCacheManager;
import com.github.cadecode.xboot.starter.cache.listener.RedisMessageListener;
import com.github.cadecode.xboot.starter.cache.util.RedisKit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.Topic;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 双级缓存刷新监听器
 * 通过 Redis pub/sub 通知集群中其他节点清理本地缓存
 *
 * @author Cade Li
 * @date 2023/6/15
 */
@Slf4j
@RequiredArgsConstructor
public class DLCacheRefreshListener extends RedisMessageListener {

    public static final ConcurrentHashSet<DLCacheRefreshMsg> SELF_MSG_MAP = new ConcurrentHashSet<>();

    private final DLCacheManager dlCacheManager;
    private final DLCacheProperties cacheProperties;

    @Override
    public List<Topic> topics() {
        String syncTopic = cacheProperties.getRemote().getSyncTopic();
        return Collections.singletonList(new PatternTopic(syncTopic));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        DLCacheRefreshMsg refreshMsg = (DLCacheRefreshMsg) RedisKit.getTemplate().getValueSerializer().deserialize(message.getBody());
        if (Objects.isNull(refreshMsg)) {
            return;
        }
        // 判断是不是自身节点发出
        if (SELF_MSG_MAP.contains(refreshMsg)) {
            SELF_MSG_MAP.remove(refreshMsg);
            return;
        }
        log.debug("DLCache refresh local, cache name:{}, key:{}", refreshMsg.getCacheName(), refreshMsg.getKey());
        DLCache cache = dlCacheManager.getCache(refreshMsg.getCacheName());
        if (Objects.nonNull(cache)) {
            cache.clearLocal(refreshMsg.getKey());
        }
    }
}
