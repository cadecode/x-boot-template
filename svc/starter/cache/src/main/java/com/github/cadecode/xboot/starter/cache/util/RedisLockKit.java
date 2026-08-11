package com.github.cadecode.xboot.starter.cache.util;

import cn.hutool.core.thread.ThreadUtil;
import com.github.cadecode.xboot.starter.cache.exception.RedisLockException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式锁工具
 * 支持可重入、自动续期（每 15s 续 30s）
 *
 * @author Cade Li
 * @date 2022/2/15
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RedisLockKit {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, LockContent> contentMap = new ConcurrentHashMap<>();

    private static final ScheduledThreadPoolExecutor RENEW_EXECUTOR = new ScheduledThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            ThreadUtil.newNamedThreadFactory("lockRenewExecutor-", true));

    /**
     * 阻塞获取锁
     */
    public void lock(String name) {
        lock(name, "");
    }

    public void lock(String name, Object value) {
        if (checkReentrant(name)) {
            storeLock(name, null, true);
            return;
        }
        while (true) {
            if (tryLock0(name, value)) {
                return;
            }
            sleep();
        }
    }

    /**
     * 尝试获取锁（非阻塞）
     */
    public boolean tryLock(String name) {
        return tryLock(name, "");
    }

    public boolean tryLock(String name, Object value) {
        if (checkReentrant(name)) {
            storeLock(name, null, true);
            return true;
        }
        return tryLock0(name, value);
    }

    /**
     * 在指定时间内阻塞获取锁
     */
    public boolean tryLock(String name, long timeout, TimeUnit timeUnit) {
        return tryLock(name, "", timeout, timeUnit);
    }

    public boolean tryLock(String name, Object value, long timeout, TimeUnit timeUnit) {
        if (checkReentrant(name)) {
            storeLock(name, null, true);
            return true;
        }
        long totalTime = timeUnit.toMillis(timeout);
        long current = System.currentTimeMillis();
        while (System.currentTimeMillis() - current <= totalTime) {
            if (tryLock0(name, value)) {
                return true;
            }
            sleep();
        }
        return false;
    }

    /**
     * 释放锁
     */
    public void unlock(String name) {
        if (!checkReentrant(name)) {
            return;
        }
        LockContent lockContent = contentMap.get(name);
        Integer count = lockContent.getCount();
        if (count > 0) {
            lockContent.setCount(--count);
        }
        if (count == 0) {
            contentMap.remove(name);
            lockContent.getFuture().cancel(true);
            redisTemplate.delete(name);
        }
    }

    /**
     * 强制清除锁（不检查持有者）
     */
    public void clear(String name) {
        LockContent lockContent = contentMap.get(name);
        if (Objects.nonNull(lockContent)) {
            contentMap.remove(name);
            lockContent.getFuture().cancel(true);
        }
        redisTemplate.delete(name);
    }

    /**
     * 检查可重入
     */
    private boolean checkReentrant(String name) {
        if (Objects.isNull(name)) {
            throw new RedisLockException("Lock name must not be null");
        }
        return Objects.nonNull(contentMap.get(name))
                && contentMap.get(name).getCurrThread() == Thread.currentThread();
    }

    /**
     * 记录锁状态到 contentMap
     */
    private void storeLock(String name, ScheduledFuture<?> future, boolean reentrant) {
        LockContent lockContent = contentMap.get(name);
        if (reentrant) {
            lockContent.setCount(lockContent.getCount() + 1);
            return;
        }
        // 清理旧锁残留
        if (Objects.nonNull(lockContent)) {
            lockContent.getFuture().cancel(true);
        }
        lockContent = new LockContent(future, 1, Thread.currentThread());
        contentMap.put(name, lockContent);
    }

    /**
     * 尝试 Redis setIfAbsent
     */
    private boolean tryLock0(String name, Object value) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(name, value, 30, TimeUnit.SECONDS);
        if (Objects.equals(success, false)) {
            return false;
        }
        ScheduledFuture<?> future = renewLock(name, value);
        storeLock(name, future, false);
        return true;
    }

    /**
     * 开启锁续期任务（每 15s 续 30s）
     */
    private ScheduledFuture<?> renewLock(String name, Object value) {
        return RENEW_EXECUTOR.scheduleAtFixedRate(() -> {
            Boolean success = redisTemplate.opsForValue().setIfPresent(name, value, 30, TimeUnit.SECONDS);
            if (Objects.equals(success, true)) {
                return;
            }
            contentMap.remove(name);
            log.warn("Renewing lock failed, key is {}", name);
            throw new RedisLockException("Renewing lock failed, key is {}", name);
        }, 15, 15, TimeUnit.SECONDS);
    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(300);
        } catch (InterruptedException e) {
            // 不响应中断
        }
    }

    @Data
    @AllArgsConstructor
    private static class LockContent {
        /**
         * 续期任务
         */
        private ScheduledFuture<?> future;
        /**
         * 重入次数
         */
        private Integer count;
        /**
         * 持有锁的线程
         */
        private Thread currThread;
    }
}
