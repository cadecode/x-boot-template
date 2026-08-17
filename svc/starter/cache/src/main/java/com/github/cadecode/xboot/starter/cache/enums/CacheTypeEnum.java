package com.github.cadecode.xboot.starter.cache.enums;

import com.github.cadecode.xboot.starter.cache.constant.CacheConst;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CacheManager 类型
 * <p>
 * 对应 x-boot.cache.type 配置值。
 * 注意：@ConditionalOnProperty 为精确字符串匹配，yaml 中必须按枚举名大写填写，如：type: REDIS，
 * 小写 redis 会导致 CacheManager bean 不创建。
 *
 * @author Cade Li
 * @date 2024/7/24
 */
@Getter
@AllArgsConstructor
public enum CacheTypeEnum {

    /**
     * Redis 缓存
     */
    REDIS(CacheConst.TYPE_REDIS, CacheConst.MANAGER_REDIS),

    /**
     * 双级缓存（Caffeine L1 + Redis L2）
     */
    DL(CacheConst.TYPE_DL, CacheConst.MANAGER_DL);

    /**
     * cache type 配置匹配值
     */
    private final String type;

    /**
     * CacheManager bean 名
     */
    private final String cacheManager;

}
