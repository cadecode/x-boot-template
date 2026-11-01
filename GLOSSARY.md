# X-Boot 模板

一套前后端分离的 Spring Boot 后台管理系统模板：后端 `svc/` 用 Maven 多模块，前端 `app/` 目前只有占位 README，尚未开工。这里的词都是本模板自造的概念，通用编程名词不收。

## Language

### 分层与模块

**starter**:
一个技术一个可插拔的自动配置模块（web、cache、concurrent、datasource、swagger、actuator），只做集成与默认配置，不含业务代码。
_Avoid_: plugin module, extension module

**framework**:
聚合层。它依赖若干 starter，把它们的自动配置合起来，并向业务暴露一份统一的 `x-boot.framework` 配置。
_Avoid_: core, base module

**common**:
不含 Spring Boot 自动配置的公共能力：异常体系、返回模型、扩展机制、工具类。业务与 starter 都可依赖它。
_Avoid_: utils module

**dependency**:
纯 BOM 模块，只写 `dependencyManagement`（第三方版本 + 本项目模块版本），不含代码。
_Avoid_: parent pom（`x-boot-parent` 是它的子模块，两者别混）

**server/main**:
唯一可启动的业务服务模块（`MainApplication`），端口 8080，demo 代码与测试都放这里。
_Avoid_: admin（旧名，已重命名）

### 扩展机制

**Extension**:
`common/extension/` 下四套工具的总称：Pipeline、Plugin、StateMachine、Event。各自有 README。
_Avoid_: plugin framework（那只是其中一套，且是 Spring 的）

**ExtensionType**:
所有扩展类型的统一标识接口，`getType()` 返回类型字符串。Pipeline 与 Plugin 共用同一套业务类型枚举，避免两套类型体系。
_Avoid_: 枚举类型、业务类型码

**Pipeline**:
自研的责任链。`PipelineExecutor` 既是构建器（`appendFilter`）也是执行入口（`execute`），一次请求只有一条链。
_Avoid_: workflow, flow engine

**FilterSelector**:
决定链上哪些 `PipelineFilter` 参与本次执行的选择器（`Dummy` / `MatchAll` / `LocalList`）。选择发生在 Filter 内部，不在执行器。
_Avoid_: route, condition

**PluginService**:
策略模式的服务接口，继承 Spring Plugin Framework 的 `Plugin`，靠 `supports()` 在运行时匹配。
_Avoid_: strategy interface

**PluginContext**:
插件上下文，只负责携带 `getPluginType()` 供匹配，不带业务数据本身。
_Avoid_: request context

**StateMachine**:
直接使用 Spring State Machine（`spring-statemachine-core`），本模板不封装，只在 demo 里给订单状态流转示例。
_Avoid_: 状态模式、status flow

### 缓存

**CacheType**:
`x-boot.cache.type` 的取值，只有 `REDIS` 与 `DL` 两种，且必须大写——`@ConditionalOnProperty` 是精确字符串匹配，写 `redis` 会导致 CacheManager 不创建。
_Avoid_: cache mode, backend

**DLCache**:
双级缓存，L1 是本地 Caffeine，L2 是 Redis。读：本地未命中则查 Redis 并回填；写：先写 Redis，再广播刷新通知，最后写本地。对应 CacheManager bean `dlCacheManager`。
_Avoid_: two-level cache, multi-level cache

**动态 TTL 后缀**:
缓存名带 `#<duration>` 时的约定，如 `user#6`。duration 用 Spring `DurationStyle` 语法（`6` 是 6ms、`5m`、`2h`、`1d`）。解析器是 `DynaTtlNameUtil`，以最后一个 `#` 分割。
_Avoid_: ttl suffix、cache name pattern（要区分“带后缀的完整缓存名”）

**完整缓存名**:
含 `#ttl` 后缀在内的整体名字。`@Cacheable` 与 `@CacheEvict` 必须写同一份完整名，否则会创建不同的缓存实例、永远命中不了。
_Avoid_: base name

**刷新通知**:
DLCache 写操作后经 Redis pub/sub 广播的本机缓存清理消息，默认主题 `cache:dl:refresh:topic`，集群内其余节点收到后清掉自己的 L1。
_Avoid_: invalidate, sync event

### 数据源与并发

**动态数据源**:
基于 `AbstractRoutingDataSource` 的实现：`DynamicDs` 决定当前用哪个 key，`DynamicDsHolder` 存 key（ThreadLocal），`@UseDynamicDs("key")` 在类或方法上切换，配置前缀 `x-boot.dynamic-ds`，默认关闭（`enable=true` 才装配）。
_Avoid_: multi datasource, ds routing

**限流注解**:
`@RateLimit(limitPerSecond, waitTimeout)`，由 AOP 基于 Guava RateLimiter 实现，粒度是“每个被注解的方法”。
_Avoid_: throttle, rate control

**上下文透传**:
用 TransmittableThreadLocal（`TransmitUtil` 包装 Executor / Runnable / Callable）把线程上下文带进线程池任务。
_Avoid_: MDC, trace context

### Web 与错误

**ApiResult**:
统一 JSON 返回体，`status` 与 HTTP 状态码保持一致。
_Avoid_: response wrapper, Result

**ApiFormat**:
类或方法上的开关（`@ApiFormat(false)` 关闭），由 `ApiResultAdvisor` 据此决定是否把返回值包成 ApiResult。
_Avoid_: auto wrap

**ErrorCode**:
异常错误码接口，`getCode()` / `getMessage()` / `getStatus()`，由枚举实现；状态值取自 `ApiStatus`。
_Avoid_: error enum

**BaseException**:
异常基类，message 支持 hutool `StrUtil.format` 模板传参。业务异常继承它，通用异常是 `GeneralException`。
_Avoid_: business exception

**ParamEnumConvertible**:
让枚举能在请求参数里按自定义值（而非 `name()`）绑定的接口，配合 `ParamEnumConvertorFactory` 注册到 MVC。
_Avoid_: enum converter
