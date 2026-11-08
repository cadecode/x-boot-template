# xxl-job 执行器落成 svc/starter/job，锁 3.5.0

移植 uni-boot `common/plugin/job` 那套（`XxlJobAutoConfig` + `XxlJobProperties`），但落成 `svc/starter/job`，模块名 `x-boot-starter-job`，目标版本从 2.4.0 升到 3.5.0。x-boot-template 已经是 JDK 17（`svc/pom.xml`），3.x 的硬门槛不构成障碍；3.x 执行器与 admin 2.4 及以后协议兼容，更早版本不兼容。

## 为什么不用第三方 starter

官方只发 `xxl-job-core`，没有 starter 产物 —— Maven Central 上 `com/xuxueli/` 下只有 `xxl-job` 父 POM 与 `xxl-job-core`。社区同名 starter 有十几个不同 groupId（`com.bestzyx`、`com.github.rebue.sbs`、`com.mobaijun`、`in.hocg.boot`、`com.github.hiwepy`、`io.github.wangrui027` 等），没有事实标准，且：

- Boot 2 时代只写 `spring.factories` 的 starter 在 Boot 3 上静默失效（Boot 3 只认 `AutoConfiguration.imports`），引入后不报错、bean 不注册
- 版本跟不上：跟踪最新的是 `com.bestzyx:xxl-job-spring-boot-starter:3.3.3`，依赖 core 3.3.2，而 core 已到 3.5.0
- 传递依赖污染：`com.github.liaomengge:base-xxl-job-spring-boot-starter` 会把 `spring-boot-starter-actuator` 与 `spring-cloud-context` 带成 compile 依赖

自己写的 starter 只有两个类、约 70 行，版本由 `svc/dependency` 的 BOM 管住。

## 为什么 framework 不聚合它

`XxlJobSpringExecutor` 在 `afterSingletonsInstantiated` 里就 `startEmbedServer()`，会占一个端口（默认从 9999 找可用）并起 netty 线程。framework 一旦依赖它，每个引 framework 的服务都具备这个能力，只靠 `xxl.job.enabled` 挡着。定时任务不是所有业务服务的默认需求，因此按需依赖。

顺带说明：ADR 0001 原文说的「framework 依赖它们全部」与实际不符 —— framework 现在只有 web、swagger、datasource、mybatis 四个，cache / concurrent / actuator 都没进。该 ADR 已一并修正为按需。

## 配置前缀用 xxl.job.*，是故意的偏离

仓库自己的 starter 全部用 `x-boot.*`（`x-boot.cache`、`x-boot.dynamic-ds`、`x-boot.framework`），但 xxl-job 官方 sample 是 `xxl.job.admin.addresses` / `xxl.job.executor.appname` / `xxl.job.accessToken`，嵌套结构（`admin` / `executor`）也照官方。改成 `x-boot.job.*` 会让以后每看一行官方文档都要在脑子里翻译一次，所以保持官方键名。

`XxlJobProperties` 的字段名同样用官方写法（`appname`、`logpath`、`logretentiondays` 全小写），为的是让 mica-auto 生成的 `spring-configuration-metadata.json` 里键名与官方文档逐字相同，IDE 补全直接给官方键。绑定本身不要求小写 —— 实测 camelCase 字段也能从官方全小写 key 绑定，全小写只是为了元数据一致。

## 默认关闭，且不做启动期校验

`@ConditionalOnProperty(name = "xxl.job.enabled", havingValue = "true")`，沿用 uni-boot，不写就关。这个 starter 一启用就有运行时副作用（占端口、起线程、对外发注册请求），应该由一行显式开关确认，不能从「某个必填项存在」推导出来。

必填项 `adminAddresses` / `appname` / `accessToken` 不自己校验，交给 `XxlJobExecutor.start()`。实测（`xxl-job-core:3.5.0` + Spring Boot 3.2.5，独立探针工程，`@Bean` 方法里只 set 属性不校验）：

| 场景 | 结果 | 耗时 |
|---|---|---|
| 三项齐全，admin 不可达 | 启动成功 | 3.8s |
| `accessToken` 空 | `RuntimeException: xxl-job executor accessToken empty.` | 4.5s |
| `appname` 空 | `RuntimeException: xxl-job executor appname empty.` | 4.1s |
| `adminAddresses` 空 | `RuntimeException: xxl-job executor adminAddresses empty.` | 3.9s |

三个失败场景都是在 bean 创建完成之后、`afterSingletonsInstantiated` 阶段抛，应用起不来。自己再加 `@Validated` / `Assert` 的唯一收益是把消息里的 `accessToken` 换成 `xxl.job.accessToken`，代价是重复逻辑，并且框架哪天放宽校验时我们的断言会误杀。因此不做。

## 已知代价：注册失败是静默的

这条比上面三条都重要，因为它是唯一不会被任何校验拦住的失败路径 —— `xxl.job.admin.addresses` 写错时，应用照常启动、端口照常占用、任务永不执行。

实现上 `ExecutorRegistryThreadHelper` 起一个 `CyclicThread("ExecutorRegistryThread#registryThread", daemon, runnable, Const.BEAT_TIMEOUT * 1000L, alignTime = true)`，循环体里 `catch (Throwable e)` 之后只 `logger.info`，从不外抛。`Const.BEAT_TIMEOUT = 30`；`alignTime = true` 让等待变成 `sleep(30000 - now % 30000)`，即对齐到 30 秒墙钟边界（目的是让集群内所有节点同时注册），首次立即执行。

实测（admin 指向不可达地址，应用存活 40 秒）：

```
22:49:44.109  INFO  CyclicThread : start
22:49:44.183  INFO  ExecutorRegistryThreadHelper : xxl-job registry error, registryParam:{... registryValue='http://192.168.0.130:19999/'}
                java.lang.RuntimeException: Http Request Error (Connection refused: getsockopt). for url : http://127.0.0.1:18080/xxl-job-admin/api/registry
22:50:00.010  INFO  ExecutorRegistryThreadHelper : xxl-job registry error（第二次）
22:50:23.242  WARN  ExecutorRegistryThreadHelper : registry-remove error（关闭时）
```

期间没有一条 ERROR。两次重试间隔 15.8 秒，是对齐墙钟的结果，不是 30 秒固定间隔。排查入口是搜日志 `xxl-job registry error`，不是看启动状态。

同一份日志里还能看到执行器自报的注册地址是 `IPTool.getIp()` 挑的物理网卡地址（`http://192.168.0.130:19999/`）。多网卡或容器部署时可能出现「注册成功但回调地址不可达」—— 任务能调度、执行结果收不到，比地址写错更难查。

## 3.5.0 相对 2.4.0 在客户端侧的破坏性变化

对照 `xxl-job-core` 2.4.0 与 3.5.0 的 sources jar 逐文件比较，照搬 uni-boot 代码时不能抄的部分：

- **`@XxlJob` 方法参数不再注入**：`MethodJobHandler.execute()` 变成 `method.invoke(target, new Object[paramTypes.length])`，填进去全是 null。能编译、能跑、参数永远拿不到。取参数只能 `XxlJobHelper.getJobParam()`
- **`ReturnT` 被整体注释**：`util/deprecated/ReturnT.java` 非注释行数为 0，同目录 12 个 util 文件全部如此。成败改用 `XxlJobHelper.handleSuccess()` / `handleFail()`
- **`IJobHandler.execute(String)` 改成 `execute()`**
- **`accessToken` 从可选变必填**
- **`logRetentionDays` 默认值 0 → 30**：uni-boot 用 `int` 字段，会把框架新默认值覆盖回 0；而 `JobLogFileCleanThread` 在 `logRetentionDays < 3` 时直接 return，等于日志永不清理。移植时字段类型必须是 `Integer`，默认 30
- **包结构重排**：`biz/*` → `openapi/{admin,executor}/*`，`enums` → `constant`，`XxxThread` → `XxxThreadHelper`
- **`jakarta.annotation-api` 取代 `javax.annotation-api`**

`XxlJobSpringExecutor` 的 8 个 setter（`adminAddresses`、`accessToken`、`appname`、`address`、`ip`、`port`、`logPath`、`logRetentionDays`）全部保留，所以 uni-boot 的自动配置类本身不用改。

## Considered Options

- **照搬 2.4.0**：能与更老的 admin（≤ 2.2）通信，但等于把已废弃的 `ReturnT`、方法参数注入一起搬进来，且会引入上文列的日志清理退化
- **用第三方 starter**：省 70 行，代价是供应链不确定性（同名 artifact 十几个发布者）、版本滞后、传递依赖污染
- **用 `@ConditionalOnProperty("xxl.job.admin.addresses")` 替代 `enabled` 开关**：少一个配置项，但配错时静默不启用，与本 ADR 要暴露的失败路径冲突
- **在 properties 上加 `@Validated` + `@NotBlank`**：见「不做启动期校验」，收益仅为措辞，且是本仓库 starter 层没有先例的模式
