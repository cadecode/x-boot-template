# Extension 工具使用指南

`svc/common/.../extension/` 封装了四套设计模式工具，用于构建可扩展的业务逻辑。

| 工具 | 模式 | 基础 | 适用场景 | 详细文档 |
|------|------|------|---------|---------|
| **Pipeline** | 责任链 | 自研 Filter 链表 | 流程化逻辑拆分，多个处理单元按序执行 | `pipeline/README.md` |
| **Plugin** | 策略模式 | Spring Plugin Framework | 多种算法/策略动态切换 | `plugin/README.md` |
| **StateMachine** | 有限状态机 | Spring State Machine | 状态流转规则化，保证状态转换正确性 | `state/README.md` |
| **Event** | 观察者 | Spring Event | 模块解耦通信，同步/异步/事务事件 | `event/README.md` |

---

## 对比

| 维度 | Pipeline | Plugin | StateMachine | Event |
|------|----------|--------|-------------|-------|
| 模式 | 责任链 | 策略 | 有限状态机 | 观察者 |
| 核心操作 | 多个 filter **依次处理** | 选择一个 plugin **执行替代** | 定义转换规则，**保证流程正确** | **发布-订阅**，解耦通信 |
| 输出 | 链式处理，模型逐渐完善 | 策略返回结果 | 状态流转，Guard/Action | 通知、日志、缓存更新 |
| 匹配方式 | YAML 控制启用/禁用 | `supports(context)` 动态匹配 | 事件驱动，非法自动拒绝 | 事件类型匹配 |
| 典型场景 | 订单下单流程、审批流 | 支付方式切换、通知渠道 | 订单状态流转、审批工作流 | 跨模块通知、异步日志 |

---

## 项目结构

```
common/src/main/java/.../extension/
├── pipeline/
│   ├── README.md                          # Pipeline 使用指南
│   ├── PipelineContext.java
│   ├── PipelineFilter.java
│   ├── PipelineExecutor.java
│   └── selector/
│       ├── FilterSelector.java
│       ├── LocalListFilterSelector.java
│       ├── MatchAllFilterSelector.java
│       ├── DummyFilterSelector.java
│       └── FilterSelectorFactory.java
├── plugin/
│   ├── README.md                          # Plugin 使用指南
│   ├── PluginService.java
│   ├── PluginContext.java
│   ├── PluginExecutor.java
│   └── PluginSelectorExecutor.java
├── state/
│   └── README.md                          # StateMachine 使用指南
└── event/
    └── README.md                          # Spring Event 使用指南

server/main/src/test/java/.../main/demo/
├── pipeline/                              # Pipeline Demo（15 tests）
│   ├── PipelineTests.java
│   ├── PipelineTestConfig.java
│   └── ...
├── state/                                 # StateMachine Demo（4 tests）
│   ├── StateTests.java
│   ├── StateTestConfig.java
│   └── ...
├── event/                                 # Event Demo（4 tests）
│   ├── EventTests.java
│   ├── EventTestListener.java
│   └── ...
└── plugin/                                 # Plugin Demo（3 tests）
    ├── PluginTests.java
    └── ...
```
