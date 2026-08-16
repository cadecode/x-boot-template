package com.github.cadecode.xboot.starter.cache.cache.dl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 双级缓存刷新消息体
 *
 * @author Cade Li
 * @date 2023/6/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DLCacheRefreshMsg {

    private String cacheName;

    private Object key;
}
