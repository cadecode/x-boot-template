# 数据权限用 MP 的 DataPermissionInterceptor

移植 `uni-boot-cloud` 的 `@DataScope` 数据权限，但实现落在 MyBatis-Plus 官方 SPI 上，并按 MP 3.5.15 的新 API 重写。

## 依赖

`DataPermissionInterceptor` / `MultiDataPermissionHandler` 自 MP 3.5.9 起从 `mybatis-plus-extension` 拆到 `mybatis-plus-jsqlparser`。该模块有两个产物，对应两代 jsqlparser：

| 产物 | 传递的 jsqlparser | 与 PageHelper 6.1.0 |
|---|---|---|
| `mybatis-plus-jsqlparser` | 5.2 | ❌ CTE 的 count 改写 `NoSuchMethodError` |
| `mybatis-plus-jsqlparser-4.9` | 4.9 | ✅ 兼容 |

所以 `starter/mybatis` 引 `com.baomidou:mybatis-plus-jsqlparser-4.9`，`svc/dependency` 再把 `com.github.jsqlparser:jsqlparser` 钉到 **4.9**，压过 pagehelper 传递来的 4.7 —— classpath 上只能有一个 jsqlparser。

> 5.2 不可用的原因（写小程序直接喂 SQL 给 pagehelper 的 `DefaultCountSqlParser` 实测）：jsqlparser 5.x 把 `WithItem#getSelect()` 的返回类型改成了 `ParenthesedSelect`，pagehelper 6.1.0 的 `DefaultCountSqlParser` 仍按 `Select` 调用，带 `WITH` 的 SQL 一进 count 改写就 `NoSuchMethodError: 'net.sf.jsqlparser.statement.select.Select net.sf.jsqlparser.statement.select.WithItem.getSelect()'`；普通查询与 join 不受影响。4.9 上三类 SQL 全部正常。

## 结构

| 类 | 职责 |
|---|---|
| `annotation/DataScope` | 类/方法注解：`enableFilter`、`field`（数据范围列，可带别名）、`tables`（生效表名白名单）、`resolverName` |
| `aspect/DataScopeAspect` | AOP 解析注解 → `DataScopeParam` 放 ThreadLocal，`@After` 清理 |
| `spi/DataScopeResolver` | SPI：`getScopes()` + `isAdmin()`；默认实现返回 `null`（不限制） |
| `handler/DataScopePermissionHandler` | 实现 `MultiDataPermissionHandler`，按表回调，命中白名单才返回 `field IN (...)` |

## 与 uni-boot-cloud 的差别（关键决定）

1. **handler 用新 API**：uni-boot 实现旧的 `DataPermissionHandler#getSqlSegment(where, msId)`，一条 SQL 一个条件；
   这里实现 `MultiDataPermissionHandler#getSqlSegment(Table, where, msId)`，MP 逐表回调。
   → CTE（`withItemsList`）会被处理；join 时可按表决定，且 `tables` 白名单避免给字典表加不存在的列。
2. **只返回条件本身**：新 API 由 `BaseMultiTableInnerInterceptor#andExpression` 负责和原 where 合并；返回 `null` 表示不改动。
   uni-boot 因为用旧 API，必须自己 `new AndExpression(where, in)` 并返回原 where。
3. **不再手动塞拦截器**：uni-boot 用 `InitializingBean` + `configuration.addInterceptor` 逐个 `SqlSessionFactory` 处理；
   这里只声明 `MybatisPlusInterceptor` bean，交给 `MybatisPlusAutoConfiguration#setPlugins`。
4. **顺序用 `@Order`**：`MybatisPlusInterceptor` 标 `@Order(100)`、PageHelper 的 `PageInterceptor` 标 `@Order(0)`。
   MyBatis `InterceptorChain.pluginAll` 从前往后逐个包装，**列表靠后者在最外层、先执行**；而 Spring 注入
   `Interceptor[]` 时按 `@Order` 升序排列（`DefaultListableBeanFactory#resolveMultipleBeans` → `Arrays.sort`），
   所以 `@Order` 数值大的在外层 → 数据权限先改写 SQL，PageHelper 据此生成的 count 也带权限条件。
   uni-boot 是 PageHelper 在外层，count SQL 用未过滤的 SQL 生成 → **分页总数泄露无权限数据**。
5. **默认解析器**：容器里始终有一个 `DataScopeResolver`（`@ConditionalOnMissingBean`，返回 `null` = 不限制），
   注解不写 `resolverName` 时取容器里唯一的实现，有多个实现则直接抛异常提示补 `resolverName`；
   uni-boot 要求业务提供名为 `defaultDataScopeResolver` 的 bean，取不到直接抛。

## 注解读取口径

四个取注解的地方（本切面、`DynamicDsAspect`、`RateLimitAspect`、`ApiResultAdvisor`）统一走
`common/util/AspectUtil`：先用 `AopUtils.getTargetClass` + `AopUtils.getMostSpecificMethod` 把
「签名方法 + 目标对象」还原成「目标类 + 实现方法」，再用 JDK 原生 `getAnnotation` 读取。
方法上的注解优先、类上的兜底，**回退粒度统一为整条注解**：方法上只要标了注解，就完全使用它的属性，
不与类上的注解做属性合并（与 Spring 的 `@Transactional` 一致）。四个注解（`@DataScope`、`@DynamicDs`、
`@RateLimit`、`@ApiFormat`）口径一致。

代价（明确接受）：不使用 `AnnotatedElementUtils.findMergedAnnotation`，所以**元注解与 `@AliasFor`
属性合成不生效**。原因：切点是 `@within || @annotation`，只按直接标注匹配，元注解场景根本进不来，
合并能力用不上；而 `getMostSpecificMethod` 已经覆盖了泛型桥接方法这一实际收益。

## 用法

```java
@DataScope(field = "u.dept_id", tables = {"sys_user"})
public PageInfo<SysUser> page(PageParams params) { ... }
```

- 不写 `tables` 表示所有表都加条件，此时被查到的每张表都要有该列，否则 `Unknown column`
- `enableFilter = false` 可临时关掉
- UPDATE / DELETE 也会被加条件（MP 在 `beforePrepare` 里处理），不想影响写操作就别把注解标在写方法上
