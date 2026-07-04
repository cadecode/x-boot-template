# Spring State Machine 使用指南

> 依赖：`spring-statemachine-core`（已在 `svc/common/pom.xml` 中声明，版本见 `svc/dependency/pom.xml`）  
> 可运行测试：`svc/server/admin/src/test/.../admin/state/StateTests.java`

## 1. 快速开始

以订单状态机为例，30 行配置跑通 CREATED → PAID → SHIPPED → COMPLETED：

```java
// ① 定义状态和事件
enum OrderState { CREATED, PAID, SHIPPED, COMPLETED, CANCELLED }
enum OrderEvent { PAY, SHIP, COMPLETE, CANCEL }

// ② 配置状态机（放在 @Configuration 类中）
@Configuration
@EnableStateMachineFactory  // 多实例模式，每次 getStateMachine() 返回新实例
public class OrderStateMachineConfig
        extends StateMachineConfigurerAdapter<OrderState, OrderEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states)
            throws Exception {
        states.withStates().initial(OrderState.CREATED)
                .states(EnumSet.allOf(OrderState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderState, OrderEvent> transitions)
            throws Exception {
        transitions
                .withExternal().source(OrderState.CREATED).target(OrderState.PAID)
                    .event(OrderEvent.PAY)
                .and().withExternal().source(OrderState.PAID).target(OrderState.SHIPPED)
                    .event(OrderEvent.SHIP)
                .and().withExternal().source(OrderState.SHIPPED).target(OrderState.COMPLETED)
                    .event(OrderEvent.COMPLETE)
                .and().withExternal().source(OrderState.CREATED).target(OrderState.CANCELLED)
                    .event(OrderEvent.CANCEL);
    }
}

// ③ 使用
@Autowired StateMachineFactory<OrderState, OrderEvent> factory;

StateMachine<OrderState, OrderEvent> sm = factory.getStateMachine();
sm.startReactively().block();

sm.sendEvent(Mono.just(MessageBuilder.withPayload(OrderEvent.PAY).build())).blockLast();
// sm.getState().getId() == PAID

sm.sendEvent(Mono.just(MessageBuilder.withPayload(OrderEvent.SHIP).build())).blockLast();
// sm.getState().getId() == SHIPPED

// 非法事件自动拒绝：CREATED 状态下发送 SHIP → 状态不变
sm.stopReactively().block();
```

---

## 2. 核心概念

| 概念 | 类型 | 说明 |
|------|------|------|
| **State** | 自定义 enum | 实体可能的状态（如 `CREATED`, `PAID`） |
| **Event** | 自定义 enum | 触发状态转换的事件（如 `PAY`, `SHIP`） |
| **Transition** | 配置规则 | `source → target` 的转换，由事件触发，框架保证合法性 |
| **Guard** | `ctx → boolean` | 转换**前**校验，返回 `false` 拒绝事件 |
| **Action** | `ctx → void` | 转换**中**执行的副作用（审计、通知） |
| **Interceptor** | 钩子类 | 转换**前/后**的全阶段回调（日志、事件发布） |
| **Extended State** | `Map<Object, Object>` | 携带业务参数（订单 ID、金额），Guard/Action/Interceptor 中读取 |

---

## 3. 单实例 vs 多实例

| | `@EnableStateMachine` | `@EnableStateMachineFactory` |
|------|------|------|
| Bean 类型 | `StateMachine<S,E>` | `StateMachineFactory<S,E>` |
| 实例数 | 全局 1 个 | 按需创建 N 个 |
| 适合 | 系统级状态（应用生命周期） | 业务实体（一个订单一个实例） |
| 注解监听 | ✅ `@WithStateMachine` + `@OnTransition` | ❌ 不支持 |
| 编程式监听 | ✅ `StateMachineInterceptor` | ✅ `StateMachineInterceptor` |

状态、转换、Guard、Action 的配置代码**两种模式完全相同**，只有注解不同。

```java
// 单实例
@EnableStateMachine(name = "orderMachine")
// 多实例
@EnableStateMachineFactory
```

其他配置选项：

```java
@Override
public void configure(StateMachineConfigurationConfigurer<S, E> config) throws Exception {
    config.withConfiguration()
            .autoStartup(false)   // 手动控制启动
            .machineId("myId");   // 指定机器 ID
}
```

---

## 4. 扩展状态传参

Guard / Action / Interceptor 中通过两种方式获取业务数据：

```java
// 方式一：Extended State（批量传参，适合初始化时设置）
sm.getExtendedState().getVariables().put("orderId", 123L);
sm.getExtendedState().getVariables().put("amount", new BigDecimal("9900"));

// 方式二：Message Headers（每次 sendEvent 时传参，适合事件级参数）
Message<OrderEvent> msg = MessageBuilder
        .withPayload(OrderEvent.PAY)
        .setHeader("orderId", 123L)
        .build();
sm.sendEvent(Mono.just(msg)).blockLast();
```

在回调中读取：

```java
// Guard / Action 中
Long orderId = ctx.getExtendedState().get("orderId", Long.class);

// Interceptor 中 — Extended State
Long orderId = sm.getExtendedState().getVariables().get("orderId");
// Interceptor 中 — Message Headers
Long orderId = message.getHeaders().get("orderId", Long.class);
```

---

## 5. Guard + Action + Interceptor

三者按执行顺序形成完整的转换生命周期：

```
sendEvent(E) → Guard(通过?) → Action(副作用) → State 变更 → Interceptor.postStateChange(通知)
```

### 5.1 Guard

```java
transitions
    .withExternal().source(State.S1).target(State.S2).event(Event.GO)
        .guard(ctx -> {
            BigDecimal amount = ctx.getExtendedState().get("amount", BigDecimal.class);
            return amount != null && amount.compareTo(threshold) < 0;
        });
```

### 5.2 Action

```java
transitions
    .withExternal().source(State.S1).target(State.S2).event(Event.GO)
        .action(ctx -> log.info("{} → {}",
                ctx.getSource().getId(), ctx.getTarget().getId()))
        .action(ctx -> { /* 第二个 Action：更新 DB */ });
```

多个 `.action()` 按声明顺序执行。如需可复用逻辑，提取为 `@Bean`：

```java
.guard(amountGuard())     // Guard 是 @Bean 方法，可注入依赖
.action(auditAction())    // Action 是 @Bean 方法
```

### 5.3 Interceptor

每个实例运行时注册，覆盖转换全阶段：

```java
sm.getStateMachineAccessor().doWithAllRegions(access -> {
    access.addStateMachineInterceptor(new StateMachineInterceptorAdapter<>() {

        public void preStateChange(...)  { }  // 状态变更前
        public void postStateChange(...) {    // ★ 最常用：变更后通知
            if (transition == null || transition.getSource() == null) return;
            MyState src = transition.getSource().getId();
            MyState tgt = state.getId();
            MyEvent evt = msg != null ? msg.getPayload() : null;
            // 在此：发 Spring Event、写历史记录、更新 ES ...
        }
        public void stateMachineError(...) { }  // 异常处理
    });
});
```

> Interceptor 适合**外部通知**（发消息、记录历史）；Guard + Action 适合**转换内逻辑**（校验、审计）。

---

## 6. 注解监听（仅单实例）

仅 `@EnableStateMachine` 支持。

### 6.1 日常使用

```java
@Component
@WithStateMachine                // 绑定默认 stateMachine bean
public class OrderListener {

    @OnTransition(source = "CREATED", target = "PAID")
    public void onPaid() {
        // 订单支付后
    }

    @OnTransition                 // 不指定 source/target = 监听所有转换
    public void onAny(Message<OrderEvent> msg, ExtendedState extState) {
        // 通用处理
    }
}
```

指定 name 绑定不同状态机：

```java
@EnableStateMachine(name = "orderMachine")
@WithStateMachine(name = "orderMachine")
```

### 6.2 全部钩子

| 注解 | 参数类型 | 触发时机 |
|------|------|------|
| `@OnTransition` | `Message<E>`, `StateContext<S,E>`, `ExtendedState` | 状态转换后 |
| `@OnStateChanged` | `State<S,E>`, `Transition<S,E>` | 状态变更后（含 Internal Transition） |
| `@OnStateEntry` | `State<S,E>` | 进入某状态时 |
| `@OnStateExit` | `State<S,E>` | 离开某状态时 |
| `@OnEventNotAccepted` | `Message<E>` | 事件被拒绝时 |

### 6.3 注解 vs 编程式 决策

```
你是多实例（@EnableStateMachineFactory）？
 ├── 是 → 编程式 StateMachineInterceptor（§5.3）
 └── 否 → 你是单实例？
            ├── 简单回调 → 注解（§6）
            └── 需要完整钩子 → Interceptor（§5.3，两种模式通用）
```

---

## 7. 可运行测试

```bash
cd svc && mvn test -pl server/admin \
  -Dtest="com.github.cadecode.xboot.admin.status.StateMachineTests"
```

测试覆盖：正常流转 S1→S2→S3→S4、非法事件拒绝、取消流程、已前进后禁取消。
