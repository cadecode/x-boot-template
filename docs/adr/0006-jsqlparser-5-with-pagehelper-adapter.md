# jsqlparser 统一到 5.2，PageHelper 用 sqlparser5.1 适配模块

ADR 0004 决定不引 `mybatis-plus-jsqlparser`，ADR 0005 因此把 jsqlparser 钉在 4.9。这两条现在都改了：`starter/mybatis` 引 `com.baomidou:mybatis-plus-jsqlparser`（钉 5.2），`svc/dependency` 把 `com.github.jsqlparser:jsqlparser` 钉到 5.2，PageHelper 传递来的 jsqlparser 在 `starter/mybatis/pom.xml` 里排除，改由 `com.github.pagehelper:sqlparser5.1` 提供 count 改写实现。classpath 上仍然只有一个 jsqlparser。

## 为什么 5.2 现在可用了

4.9 的唯一理由是 PageHelper 6.1.0 的 `DefaultCountSqlParser` 在 jsqlparser 5.x 上抛 `NoSuchMethodError`（`WithItem#getSelect()` 返回类型从 `Select` 变成 `ParenthesedSelect`）。

PageHelper 6.1.1 把 count 解析器做成了 SPI。`AbstractDialect` 构造时调 `ClassUtil.newInstance(properties.getProperty("countSqlParser"), CountSqlParser.class, properties, DefaultCountSqlParser::new)`：没有显式指定 `countSqlParser` 属性时，它走 `ServiceLoader.load(CountSqlParser.class)`，取到第一个实现就用。`sqlparser5.1` 模块里 `META-INF/services/com.github.pagehelper.parser.CountSqlParser` 注册的是 `CountJSqlParser51`，于是 `DefaultCountSqlParser` 根本没被实例化。

所以升级不是绕过问题，是换了一份按 5.x AST 写的实现。`PageHelperConfig` 没有设置 `countSqlParser` 属性，SPI 生效。

回归覆盖：`MybatisFeatureTests#a7_cteWithPageHelper` 打的就是当年抛错的那条 `WITH` 语句，断言 `total=3`。

## 没变的部分

分页仍然只由 PageHelper 完成。MP 的 `PaginationInnerInterceptor` 依旧不注册，`PaginationInnerInterceptor` 与 `PageInterceptor` 不能同时注册的结论不变（ADR 0004）。`MybatisFeatureTests#a6_mpSelectPageWithoutPlugin` 继续断言 `selectPage` 不分页。

## Considered Options

- 留在 jsqlparser 4.9：可用，但 MP 侧只能用 `mybatis-plus-jsqlparser-4.9` 这个代际产物，两个库在 jsqlparser 代际上永久分叉，每次升 MP 都要确认 4.9 产物仍在维护。
- 升 PageHelper 但继续排除适配模块：就是当年 `NoSuchMethodError` 的复现，不可行。
