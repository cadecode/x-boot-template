package com.github.cadecode.xboot.starter.job.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.github.cadecode.xboot.common.util.JacksonUtil;
import com.xxl.job.core.context.XxlJobHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * xxl-job 任务辅助方法
 *
 * @author Cade Li
 * @since 2026/11/7
 */
public class XxlJobUtil {

    private XxlJobUtil() {
    }

    /**
     * 按当前分片下标过滤列表；不在任务上下文或未开启分片时原样返回
     */
    public static <T> List<T> shard(List<T> list) {
        if (CollUtil.isEmpty(list)) {
            return list;
        }
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        if (shardIndex < 0 || shardTotal <= 1) {
            return list;
        }
        if (shardIndex >= shardTotal) {
            XxlJobHelper.log("shardIndex out of range, skip. shardIndex: {}, shardTotal: {}", shardIndex, shardTotal);
            return new ArrayList<>();
        }
        List<T> result = new ArrayList<>(list.size() / shardTotal + 1);
        for (int i = 0; i < list.size(); i++) {
            if (i % shardTotal == shardIndex) {
                result.add(list.get(i));
            }
        }
        return result;
    }

    /**
     * 取任务参数并按 JSON 反序列化，参数为空返回 null
     */
    public static <T> T getJsonParam(Class<T> type) {
        return getJsonParam(type, null);
    }

    /**
     * 取任务参数并按 JSON 反序列化，参数为空返回 defaultValue
     */
    public static <T> T getJsonParam(Class<T> type, T defaultValue) {
        String param = XxlJobHelper.getJobParam();
        if (StrUtil.isBlank(param)) {
            return defaultValue;
        }
        try {
            T bean = JacksonUtil.toBean(param, type);
            return bean != null ? bean : defaultValue;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "job param is not a valid json of " + type.getSimpleName() + ": " + param, e);
        }
    }

    /**
     * 取逗号分隔的任务参数，参数为空返回空列表
     */
    public static List<String> getCommaParamList() {
        String param = XxlJobHelper.getJobParam();
        if (StrUtil.isBlank(param)) {
            return new ArrayList<>();
        }
        return StrUtil.splitTrim(param, ',');
    }

}
