package com.github.cadecode.xboot.main.demo.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event Demo 业务 Service
 * <p>
 * 模拟正常事务和回滚事务，用于验证事务事件
 *
 * @author Cade Li
 * @since 2026/7/4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventTestService {

    private final ApplicationEventPublisher publisher;

    /** 正常事务：发布事件，提交后触发 AFTER_COMMIT 监听器 */
    @Transactional
    public void doNormalTransaction(Long orderId) {
        log.info("[service] doNormalTransaction start: orderId={}", orderId);
        publisher.publishEvent(new EventTestEvent(orderId, "normal"));
        log.info("[service] doNormalTransaction end: orderId={}", orderId);
    }

    /** 回滚事务：发布事件后抛异常，回滚后触发 AFTER_ROLLBACK 监听器 */
    @Transactional
    public void doRollbackTransaction(Long orderId) {
        log.info("[service] doRollbackTransaction start: orderId={}", orderId);
        publisher.publishEvent(new EventTestEvent(orderId, "rollback"));
        log.info("[service] doRollbackTransaction throwing: orderId={}", orderId);
        throw new RuntimeException("模拟回滚异常");
    }
}
