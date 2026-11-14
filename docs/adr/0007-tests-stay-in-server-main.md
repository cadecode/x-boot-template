# 测试留在 server/main，包名保留 main 前缀

全部测试放在 `server/main/src/test` 下，子包镜像被测模块（`main/common/…`、`main/starter/…`），不拆到各模块自己的 `src/test`。

前缀 `main` 不是历史遗留，拆不掉：`SpringBootTestContextBootstrapper` 用 `AnnotatedClassFinder(SpringBootConfiguration.class)` 从测试类的包逐级向上找启动类。去掉前缀后测试位于 `xboot.common`，与 `xboot.main` 不同枝，直接报 `Unable to find a @SpringBootConfiguration`。在 `xboot` 放一个测试专用启动类也不行——那一级是递归扫描，会同时命中它和 `MainApplication`，`AnnotatedClassFinder` 的 `Assert.state(components.size() == 1)` 随即失败（`Found multiple @SpringBootConfiguration annotated classes`）。

保留前缀还顺带解决两件事：`MainApplication` 的默认扫描范围（`com.github.cadecode.xboot.main`）覆盖到测试夹具，不需要给每个测试类写 `@Import`；包名仍归 `server/main` 所有，不产生 split package。

代价必须知道：`common` 与 `starter` 模块自身没有测试，`mvn test -pl svc/common` 是空跑，改 `common` 只能从根目录起整个应用验证。

## 适用范围

这条约束管的是业务应用（`server/main`）。有独立启动类的开发期工具模块（当前是 `server/codegen`）把测试放在自己的 `src/test` 下：`@SpringBootTest` 用 `AnnotatedClassFinder` 从测试类的包向上找 `@SpringBootConfiguration`，工具模块找到的是自己的启动类；搬到 `server/main` 反而要给业务模块加一条对工具模块的依赖，不划算。

## Considered Options

- **下沉到各模块的 `src/test`**：模块级回归生效、无 split package、模块独立发版时测试跟着走。但每个模块要加 test 依赖，且 `common` / `starter` 没有启动类，`@SpringBootTest` 得改写成 `ApplicationContextRunner`；`MybatisTestUser*` 这类夹具被 mybatis 与 datasource 两组共用，还要额外造一个 test-support 模块。
- **并列包名**（`main/common/…` 改成 `common/…`）：包名更直观，但撞上上面说的启动类发现，每个测试类都得显式写 `classes = MainApplication.class`，还要 `@Import` 各自夹具，同时产生 split package。
