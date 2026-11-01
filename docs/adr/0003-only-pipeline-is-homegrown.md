# Extension 只在 Pipeline 上自研，其余交给 Spring

`common/extension/` 提供四套扩展工具，但只有 Pipeline 是自研的：`PipelineExecutor` 同时充当链构建器（`appendFilter`）与执行入口（`execute`），选择哪些 Filter 参与执行的职责放在 `FilterSelector` 里，而不是放在执行器或 Filter 上。Plugin 用 Spring Plugin Framework 的 `supports()`，StateMachine 直接用 Spring State Machine，Event 直接用 Spring 的事件机制——这三样已经有成熟实现，自研只会多一层要维护的抽象。所有扩展类型共用 `ExtensionType` 一个标识接口，避免 Pipeline 与 Plugin 各自定义一套业务类型。

## Considered Options

- 引入工作流引擎（Camunda / Flowable）统一承载流程：能力过剩，且把模板绑到一整套运行时与数据库表上。
- 四套全部自研：Pipeline 的链式语义简单、需要「既是构建器又是入口」这一特定形态，Spring 没有对应实现，其余三套没有这个理由。
