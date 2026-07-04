package com.github.cadecode.xboot.main.demo.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Event Demo 监听器
 * <p>
 * 覆盖同步、异步、事务提交、事务回滚四种场景
 *
 * @author Cade Li
 * @since 2026/7/4
 */
@Slf4j
@Component
public class EventTestListener {

    /** 同步监听计数 */
    static final AtomicInteger syncCount = new AtomicInteger(0);
    /** 异步监听计数 */
    static final AtomicInteger asyncCount = new AtomicInteger(0);
    /** 事务提交后监听计数 */
    static final AtomicInteger afterCommitCount = new AtomicInteger(0);
    /** 事务回滚后监听计数（用于断言为 0） */
    static final AtomicInteger afterRollbackCount = new AtomicInteger(0);

    /** 同步监听器 */
    @EventListener
    public void handleSync(EventTestEvent event) {
        syncCount.incrementAndGet();
        log.info("[sync] orderId={}, content={}", event.orderId(), event.content());
    }

    /** 异步监听器 */
    @Async
    @EventListener
    public void handleAsync(EventTestEvent event) {
        asyncCount.incrementAndGet();
        log.info("[async] orderId={}, thread={}", event.orderId(), Thread.currentThread().getName());
    }

    /** 事务提交后监听器 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCommit(EventTestEvent event) {
        afterCommitCount.incrementAndGet();
        log.info("[after-commit] orderId={}", event.orderId());
    }

    /** 事务回滚后监听器 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleAfterRollback(EventTestEvent event) {
        afterRollbackCount.incrementAndGet();
        log.info("[after-rollback] orderId={}", event.orderId());
    }

    /** 重置所有计数器（每个 test 之前调用） */
    static void reset() {
        syncCount.set(0);
        asyncCount.set(0);
        afterCommitCount.set(0);
        afterRollbackCount.set(0);
    }
}
