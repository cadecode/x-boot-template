# MyBatis 集成用 MyBatis-Plus，分页交给 PageHelper

新增 `starter/mybatis`，替代 `old-dev` 分支里的 mybatis-flex 方案。依赖 `mybatis-plus-spring-boot3-starter`（Boot 3 专用产物），分页用 `pagehelper` 原生包 + 手写 `PageInterceptor` bean（参数写死，同 `old-dev`，不走 yml），不引 `mybatis-plus-jsqlparser`。公共审计字段由抽象类 `BaseEntity` + `MetaObjectHandler` 填充，审计人取值留给 `EntityFillResolver` SPI（当前无认证体系，默认返回 `null` 即不填）。数据源默认用 baomidou `dynamic-datasource`，原自研 `DynamicDs` 保留为可选实现（默认关闭）。SQL 日志与慢 SQL 用 p6spy。

## 为什么换掉 flex

`old-dev` 的 `MyBatisFlexConfig` 依赖三样东西：flex 的 `InsertListener`/`UpdateListener`/`SetListener`、`mybatis-flex-processor` 的 APT 生成 `TableDef`、以及 flex 私有的 `mybatis-flex.datasource.*` 多数据源配置。迁到 MyBatis-Plus 后这三样都没有对应物：填充改为 `@TableField(fill = …)` + `MetaObjectHandler`（好处是自定义 XML 的 insert/update 也会填充，因为 MP 把默认 `LanguageDriver` 换成了 `MybatisXMLLanguageDriver`）；APT 直接删掉；数据源配置回到 `spring.datasource.dynamic.*`。

## 为什么分页不用 MyBatis-Plus 自己的

硬冲突在 jsqlparser：PageHelper 6.1.0 的 pom 钉死 `jsqlparser 4.7`，而 MyBatis-Plus 自 3.5.9 起把依赖 jsqlparser 的插件（`PaginationInnerInterceptor`、`BlockAttackInnerInterceptor`、`DataPermissionInterceptor`）拆到了 `mybatis-plus-jsqlparser`（钉 5.2）与 `mybatis-plus-jsqlparser-4.9`（钉 4.9）两个可选模块。同 classpath 只能留一个 jsqlparser 版本：实测 PageHelper 6.1.0 配 jsqlparser 5.2 会 `NoSuchMethodError`。

因此本模板的选择是：**只注册 PageHelper 的 `PageInterceptor`，不引任何 jsqlparser 模块**。`MybatisPlusAutoConfiguration` 会把容器里的 `Interceptor` bean 全部 `setPlugins` 到 `SqlSessionFactory`，所以声明一个 `PageInterceptor` bean 就生效，不需要 old-dev 里那套 `InitializingBean` + `configuration.addInterceptor`。

代价必须写清楚：没有 `PaginationInnerInterceptor` 时，`mapper.selectPage(IPage)` **不报错也不分页**（返回全表、`total` 为 0）；反过来只注册 MP 分页而不注册 PageHelper 时，`PageHelper.startPage()` 静默失效且 ThreadLocal 残留。两者叠加则生成 `... LIMIT ? LIMIT ?` 直接语法错误。所以约定 `selectPage` 在本模板中不可用。

## 为什么用抽象类而不是接口

`old-dev` 的 `BaseEntity` 是接口，靠 flex 的 listener + `set()` 回调工作。MyBatis-Plus 走不通：`@TableField`/`@TableId`/`@TableLogic`/`@Version` 的 `@Target` 都是 `{FIELD, ANNOTATION_TYPE}`，标到接口或 getter 上编译期就报错；且 `ReflectionKit#getFieldList` 只沿 `getSuperclass()` 走，从不遍历接口。抽象基类的字段会被 `TableInfoHelper` 正常收集，是唯一"写一次、全实体生效"的位置。

## 枚举用 MyBatis-Plus 原生

MP 的 `MybatisConfiguration` 构造器把默认枚举 handler 换成 `CompositeEnumTypeHandler`：命中 `IEnum`/`@EnumValue` 的走 `MybatisEnumTypeHandler`（落自定义值），否则委托给 MyBatis 的 `EnumTypeHandler`（落 **`name()`**，不是 `ordinal()`）。所以不需要 `default-enum-type-handler` 配置，业务枚举用 `@EnumValue` 就够。

`old-dev` 的 `DbEnumConvertible` + `DefaultEnumTypeHandler` 搬过来了但**不注册**，纯备用：需要把枚举按数值落库时，在实体字段上写 `@TableField(typeHandler = DefaultEnumTypeHandler.class)`，或自行配置 `mybatis-plus.configuration.default-enum-type-handler`。它和 `old-dev` 一样用 `ordinal()` 兜底（实现 `DbEnumConvertible` 的取 `dbValue()`）——因为枚举名写进 tinyint/int 列会报 `Incorrect integer value`。**代价必须知道**：`ordinal()` 把"声明顺序"当成数据语义，往枚举中间插值或调序会让历史数据整体错位且不报错，所以业务枚举能用 `@EnumValue` / `DbEnumConvertible` 显式声明数值就别靠 `ordinal()`。

## 数据源：默认 dynamic-datasource，自研方案可选

`dynamic-datasource` 只提供"一个 `DataSource` bean"，`MybatisPlusAutoConfiguration` 一行都不用改。但 `DataSource` 只能有一个：自研 `DynamicDs` 与它同时启用会让 `@ConditionalOnSingleCandidate(DataSource.class)` 失配，进而 `SqlSessionFactory`、`JdbcTemplate`、`DataSourceTransactionManager` 全部不创建。所以自研的 `DynamicDs`/`@DynamicDs`/`x-boot.dynamic-ds` 保留为**可选实现**，由 `x-boot.dynamic-ds.enable` 控制，默认关闭；启用时必须同时关掉 dynamic-datasource，两者只能选一个。

注意 `spring.datasource.url` 与 `spring.datasource.druid.*` 的**连接池参数**在引入 `dynamic-datasource` 后不再被读取，唯一入口是 `spring.datasource.dynamic.*`（池参数在 `dynamic.druid.*`，单库可在 `dynamic.datasource.<key>.druid.*` 覆盖）。`spring.datasource.druid.*` 下只剩 Web 监控项有效：`dynamic-datasource` 的 `DruidDynamicDataSourceConfiguration` `@Import` 了 druid starter 的 `DruidStatViewServletConfiguration`、`DruidWebStatFilterConfiguration`，因此 `webStatFilter`、`statViewServlet` 仍写在那里。另：`dynamic.druid.filters` 必须列出 `stat`/`wall`，否则下面对应的 `stat.*`、`wall.*` 配置段不生效。

## Considered Options

- 保留 mybatis-flex（`old-dev` 方案）：flex 无内置分页、生态窄、`SetListener` 这类能力在 MP 没有对应物，且模板的价值在于"主流默认答案"。
- 分页用 MyBatis-Plus 内置插件、砍掉 PageHelper：需要引 `mybatis-plus-jsqlparser(-4.9)`，与 PageHelper 的 jsqlparser 4.7 冲突；且团队已有 `PageHelper.startPage(...).doSelectPageInfo(...)` 的使用习惯。
- 让 `BaseEntity` 保持接口、用组合注解（`@AutoFillCreate` 标 `@TableField`）落地：可行（`ANNOTATION_TYPE` 允许元注解），但每个实体字段仍要标一次，收益为零。
- 自研动态数据源保留并加 `@Primary` 与 dynamic-datasource 共存：两套 ThreadLocal 互不感知，注解切换会静默失效，留隐患。
