package com.github.cadecode.xboot.main.demo.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Event Demo 测试
 * <p>
 * 验证同步事件、异步事件、事务提交后事件、事务回滚事件
 * <p>
 * 注意：Spring 中不存在"同步事件"与"异步事件"的区分——所有 @EventListener 都会响应同类型事件，
 * @Async 仅改变执行线程。本测试用独立 counters 分别验证各监听路径。
 *
 * @author Cade Li
 * @since 2026/7/4
 */
@SpringBootTest
public class EventTests {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private EventTestService eventTestService;

    @BeforeEach
    void setUp() {
        EventTestListener.reset();
    }

    @Test
    @DisplayName("同步监听：@EventListener 在发布者线程同步执行")
    void testSyncListener() {
        publisher.publishEvent(new EventTestEvent(1L, "sync"));

        // @EventListener handleSync 同步执行
        assertEquals(1, EventTestListener.syncCount.get());
    }

    @Test
    @DisplayName("异步监听：@Async + @EventListener 在独立线程执行")
    void testAsyncListener() throws InterruptedException {
        publisher.publishEvent(new EventTestEvent(2L, "async"));

        // 同步监听器也触发
        assertEquals(1, EventTestListener.syncCount.get());

        // 异步监听器在独立线程执行，需等待
        Thread.sleep(500);
        assertEquals(1, EventTestListener.asyncCount.get());
    }

    @Test
    @DisplayName("事务提交后监听：@TransactionalEventListener(AFTER_COMMIT)")
    void testTransactionalCommitEvent() {
        eventTestService.doNormalTransaction(3L);

        // AFTER_COMMIT 触发，AFTER_ROLLBACK 不触发
        assertEquals(1, EventTestListener.afterCommitCount.get());
        assertEquals(0, EventTestListener.afterRollbackCount.get());
    }

    @Test
    @DisplayName("事务回滚后监听：@TransactionalEventListener(AFTER_ROLLBACK)")
    void testTransactionalRollbackEvent() {
        assertThrows(RuntimeException.class, () -> eventTestService.doRollbackTransaction(4L));

        // AFTER_ROLLBACK 触发，AFTER_COMMIT 不触发
        assertEquals(1, EventTestListener.afterRollbackCount.get());
        assertEquals(0, EventTestListener.afterCommitCount.get());
    }
}
