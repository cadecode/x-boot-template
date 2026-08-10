package com.github.cadecode.xboot.starter.concurrent.util;

import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * TransmittableThreadLocal 工具类
 * 用于线程池场景下的上下文传递
 *
 * @author Cade Li
 * @date 2023/6/9
 */
public class TransmitUtil {

    /**
     * 包装线程池 Executor
     */
    public static Executor wrap(Executor executor) {
        return TtlExecutors.getTtlExecutor(executor);
    }

    /**
     * 包装 Runnable
     */
    public static TtlRunnable wrap(Runnable runnable) {
        return TtlRunnable.get(runnable);
    }

    /**
     * 包装 Callable
     */
    public static <T> TtlCallable<T> wrap(Callable<T> callable) {
        return TtlCallable.get(callable);
    }
}
