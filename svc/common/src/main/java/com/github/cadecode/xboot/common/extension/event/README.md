# Spring Event 使用指南

Spring Event 是 Spring 内置的观察者模式实现，提供 `ApplicationEventPublisher` + `@EventListener` 进行模块间解耦通信。

| 角色 | 接口/类 | 职责 |
|------|---------|------|
| 事件对象 | 任意 POJO | 携带事件数据，推荐不可变（`record` 或 `final` 字段） |
| 事件发布 | `ApplicationEventPublisher` / `SpringUtil` | 发布事件 |
| 同步监听 | `@EventListener` | 同步处理事件，与发布者同线程同事务 |
| 异步监听 | `@EventListener` + `@Async` | 异步处理，发布者不等待 |
| 事务监听 | `@TransactionalEventListener` | 绑定事务阶段（AFTER_COMMIT / AFTER_ROLLBACK） |

> ✅ **可运行测试**：`svc/server/main/src/test/java/com.github.cadecode.xboot.main.demo.event.EventTests.java`

---

## 1. 同步事件

### 1.1 定义事件对象

推荐使用 Java `record` 或 `final` 字段的 POJO，保证事件不可变：

```java
// Java 17 record
public record OrderCreatedEvent(Long orderId, String orderNo) {}

// 或 final 字段 POJO
@Getter
@RequiredArgsConstructor
public class OrderCreatedEvent {
    private final Long orderId;
    private final String orderNo;
}
```

> **注意**：Spring 4.2+ 事件对象不再需要继承 `ApplicationEvent`，任意 POJO 即可。

### 1.2 定义监听器

同步监听器与发布者在同一线程、同一事务中执行：

```java
@Component
@Slf4j
public class OrderEventListener {

    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("订单创建事件：orderId={}, orderNo={}", 
                 event.getOrderId(), event.getOrderNo());
        // 同步处理：发送通知、记录日志...
    }
}
```

### 1.3 发布事件

两种方式：

```java
// 方式一：注入 ApplicationEventPublisher（Spring 管理 Bean 中）
@Autowired
private ApplicationEventPublisher publisher;

public void createOrder(OrderParam param) {
    // ... 业务逻辑
    publisher.publishEvent(new OrderCreatedEvent(order.getId(), order.getOrderNo()));
}

// 方式二：SpringUtil 静态方法（任意位置，非 Spring Bean 也可用）
public void someUtilMethod() {
    SpringUtil.publishEvent(new OrderCreatedEvent(100L, "ORD-001"));
}
```

> 项目 `SpringUtil` 位于 `common/util/SpringUtil.java`，内部通过 `ApplicationContext.publishEvent()` 实现，无需额外依赖。

---

## 2. 异步事件

### 2.1 启用异步

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    /** 自定义线程池，避免耗尽默认线程 */
    @Bean("eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 2.2 异步监听 + 线程池指定

```java
@Component
@Slf4j
public class OrderEventListener {

    @Async("eventTaskExecutor")
    @EventListener
    public void handleOrderCreatedAsync(OrderCreatedEvent event) {
        log.info("异步处理：orderId={}, thread={}",
                 event.getOrderId(), Thread.currentThread().getName());
        // 非关键路径：发送邮件、推送消息...
    }
}
```

### 2.3 发布者行为

异步事件发布者不等待监听器完成，监听器在独立线程执行：

```java
publisher.publishEvent(new OrderCreatedEvent(100L, "ORD-001"));
// 发布后立即返回，handleOrderCreatedAsync 在 event- 线程执行
log.info("发布完成");  // 这行可能先于异步监听器执行
```

---

## 3. 事务事件

### 3.1 核心概念

`@TransactionalEventListener` 将事件处理绑定到事务阶段：

| 阶段 | 说明 | 使用场景 |
|------|------|---------|
| `AFTER_COMMIT` | 事务提交后执行（默认） | 发送通知、更新缓存、调用外部 API |
| `AFTER_ROLLBACK` | 事务回滚后执行 | 补偿操作、告警日志 |
| `AFTER_COMPLETION` | 事务完成（提交或回滚）后执行 | 清理资源 |
| `BEFORE_COMMIT` | 事务提交前执行 | 最终校验 |

### 3.2 示例

```java
@Component
@Slf4j
public class OrderEventListener {

    /** 事务提交后 → 发送通知 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaidAfterCommit(OrderPaidEvent event) {
        log.info("事务已提交，发送通知：orderId={}", event.getOrderId());
        notificationService.send(event.getOrderId());
    }

    /** 事务回滚后 → 告警 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleOrderPaidAfterRollback(OrderPaidEvent event) {
        log.error("事务回滚，支付失败：orderId={}", event.getOrderId());
    }

    /** 异步 + 事务提交后 → 解耦非阻塞操作 */
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaidAsync(OrderPaidEvent event) {
        log.info("异步事务后：orderId={}", event.getOrderId());
        // 耗时操作：生成报表、同步数据...
    }
}
```

### 3.3 发布者

必须在事务中发布，否则 `@TransactionalEventListener` 不会触发：

```java
@Transactional
public void payOrder(Long orderId) {
    // ... 支付逻辑（可能抛异常，触发回滚）
    publisher.publishEvent(new OrderPaidEvent(orderId));  // 事务内发布
    // 事务提交后 → AFTER_COMMIT 监听器执行
    // 事务回滚 → AFTER_ROLLBACK 监听器执行
}
```

> 若希望无事务时也触发，设置 `fallbackExecution = true`：
> ```java
> @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
> ```

### 3.4 执行时序

```
@Transactional 方法:
  1. 开始事务
  2. 执行业务逻辑
  3. publisher.publishEvent(event)  ← 事件暂存
  4. 事务提交                         ← 成功后触发 AFTER_COMMIT 监听器
  5. 若回滚                           → 触发 AFTER_ROLLBACK 监听器
```

---

## 4. 通过 SpringUtil 发布事件

项目 `SpringUtil.publishEvent()` 已封装，可在任意位置（静态方法、工具类、非 Spring Bean）发布事件：

```java
// Spring Bean 中 → 推荐用 ApplicationEventPublisher
@Autowired
private ApplicationEventPublisher publisher;
publisher.publishEvent(event);

// 非 Spring Bean 中（工具类、静态方法）→ 用 SpringUtil
SpringUtil.publishEvent(event);
```

两者等效，底层都是 `ApplicationContext.publishEvent()`。

---

## 5. 最佳实践

| 实践 | 说明 |
|------|------|
| **事件不可变** | 用 `record` 或 `final` 字段，防止监听器篡改事件数据 |
| **同步事件保持轻量** | 同步监听器与发布者同线程，耗时操作会阻塞调用方 |
| **异步事件使用自定义线程池** | 避免耗尽默认线程，设置合理拒绝策略 |
| **事务事件不用于核心逻辑** | 监听器抛异常不会回滚已提交的事务 |
| **监听器幂等** | 异步和事务事件可能重试，确保重复执行安全 |
| **不依赖监听顺序** | `@EventListener` 不保证多个监听器的执行顺序，如需有序用 `@Order` |
| **非关键路径用异步** | 通知、日志、统计等非核心操作用 `@Async` |
| **事务内发布事务事件** | `@TransactionalEventListener` 必须在事务中发布，否则用 `fallbackExecution = true` |

---

## 6. API 速查

```java
// 发布事件
@Autowired private ApplicationEventPublisher publisher;
publisher.publishEvent(new MyEvent(data));       // Spring Bean 中

SpringUtil.publishEvent(new MyEvent(data));      // 任意位置

// 同步监听
@EventListener
public void handle(MyEvent event) { ... }

// 异步监听
@Async("eventTaskExecutor")
@EventListener
public void handleAsync(MyEvent event) { ... }

// 事务提交后监听
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleAfterCommit(MyEvent event) { ... }

// 事务回滚后监听
@TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
public void handleAfterRollback(MyEvent event) { ... }

// 异步 + 事务提交后
@Async("eventTaskExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleAsyncAfterCommit(MyEvent event) { ... }
```

---

## 7. 项目结构

```
common/src/main/java/.../extension/
├── event/
│   └── README.md                           # Spring Event 使用指南（本文档）
└── ...

server/main/src/test/java/.../main/demo/
└── event/                                  # Event Demo
    ├── EventTests.java                     #   测试（同步/异步/事务/回滚）
    ├── EventTestEvent.java                 #   事件 record
    ├── EventTestListener.java              #   监听器（同步/异步/事务）
    └── EventTestService.java               #   模拟业务 Service（事务事件）
```

## 8. 与其他模式对比

| 维度 | Spring Event | Pipeline | Plugin | StateMachine |
|------|-------------|----------|--------|-------------|
| 模式 | 观察者 | 责任链 | 策略 | 有限状态机 |
| 耦合度 | 发布/订阅解耦 | 链式耦合 | 策略替换 | 规则约束 |
| 适用场景 | 通知、日志、缓存更新、跨模块通信 | 流程化多步处理 | 多实现动态切换 | 状态流转控制 |
| 执行方式 | 同步/异步/事务后 | 同步链式 | 同步策略 | 事件驱动 |

---

## 9. 扩展建议

当前 Spring Event 原生 API 已满足基本使用，以下封装点视项目需要后续引入：

### 9.1 DomainEvent 基类

Spring 4.2+ 事件可为任意 POJO，但缺少元数据。建议抽象基类统一注入：

```java
public abstract class DomainEvent<T> {
    private final String eventId;      // UUID
    private final Instant occurredAt;  // 发生时间
    private final T source;            // 聚合根 ID
    // traceId/user/tenant 等审计字段可预留 setter 供 Enricher 注入
}
```

**价值**：eventId 用于幂等去重，occurredAt 用于时间线重建，source 标识聚合根。

### 9.2 EventBus 类型安全发布

```java
@Component
public class DomainEventBus {
    private final ApplicationEventPublisher publisher;
    private final List<DomainEventEnricher> enrichers;  // SPI

    public <T extends DomainEvent<?>> void publish(T event) {
        enrichers.forEach(e -> e.enrich(event));  // 注入 user/tenant
        publisher.publishEvent(event);
    }
}
```

**价值**：泛型约束只发布 DomainEvent，Enricher SPI 自动注入上下文。

### 9.3 EventLogger 全局审计

```java
@Component
public class DomainEventLogger {
    @EventListener
    public void onDomainEvent(DomainEvent<?> event) {
        log.info("[DOMAIN-EVENT] eventId={} type={} source={}", ...);
    }
}
```

**价值**：一次注册全局审计，eventId 串联发布→消费全链路。

### 9.4 扩展建议

视项目需要后续引入以上封装点。
