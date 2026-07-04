# Pipeline（责任链）使用指南

基于自研 Filter 链表实现的责任链模式，用于流程化逻辑拆分。

## 1. 角色定义

| 角色 | 接口/类 | 职责 |
|------|---------|------|
| 上下文 | `PipelineContext` | 封装请求参数、输出模型、业务类型、FilterSelector |
| 上下文抽象 | `AbstractPipelineContext` | 提供 `pipelineType` + `filterSelector` 构造注入 |
| 过滤器 | `PipelineFilter<T>` | 处理单元：`doFilter(context, chain)` |
| 过滤器抽象 | `AbstractPipelineFilter<T>` | 模板方法：Selector 匹配 → `handle()` → chain 传递 |
| 过滤器链 | `PipelineFilterChain` | 链表接口：`filter()` + `next()` |
| 过滤器链实现 | `DefaultPipelineFilterChain` | 链表节点，持有当前 filter + 下一节点指针 |
| 管道构建执行器 | `PipelineExecutor` | `appendFilter()` 构建链表，`execute()` 执行链 |
| 过滤器选择器 | `FilterSelector` | 运行时决定哪些 filter 生效：`matchFilter(name)` |
| 业务类型 | `ExtensionType` | Marker 接口：`getType()` 返回业务标识 |

## 2. 使用方式

### 方式一：编程式（手工编排）

适用于 filter 少、不需要动态配置的简单场景。

**Step 1：定义 Context**

```java
// 业务类型枚举
@Getter
public enum OrderCodeEnum implements ExtensionType {
    PLACE_ORDER("ORDER"),
    CANCEL_ORDER("CANCEL");

    private final String type;
    OrderCodeEnum(String type) { this.type = type; }
}

// 上下文
@Getter
@Setter
public class OrderContext extends AbstractPipelineContext {
    private boolean continueChain = true;
    private OrderParam param;
    private OrderModel model;

    public OrderContext(ExtensionType pipelineType, FilterSelector filterSelector) {
        super(pipelineType, filterSelector);
    }

    @Override
    public boolean continueChain() { return continueChain; }
}
```

**Step 2：实现 Filter**

```java
@Component
public class CheckOrderFilter extends AbstractPipelineFilter<OrderContext> {
    @Override
    public void handle(OrderContext context) {
        OrderModel model = context.getModel();
        if (model == null) {
            context.setContinueChain(false);
            return;
        }
        System.out.println("校验通过");
    }
}
```

**Step 3：手工编排执行**

```java
// 构建 Pipeline
PipelineExecutor<OrderContext> executor = new PipelineExecutor<>();
executor.appendFilter(new SaveOrderFilter(), "保存订单");
executor.appendFilter(new QueryOrderFilter(), "查询信息");
executor.appendFilter(new CheckOrderFilter(), "校验订单");

// 构造选择器（选哪些 filter 生效）
LocalListFilterSelector selector = new LocalListFilterSelector(
    List.of("SaveOrderFilter", "QueryOrderFilter", "CheckOrderFilter"));

// 创建上下文并执行
OrderContext context = new OrderContext(OrderCodeEnum.PLACE_ORDER, selector);
context.setParam(orderParam);
executor.execute(context);

// 获取处理结果
OrderModel model = context.getModel();
```

### 方式二：YAML 启用控制 ✨ 推荐

开发时固定 filter 顺序，每个业务模块在自己的 YAML 中配置启用哪些 filter。通过 `FilterSelectorFactory` 静态工具解析。

**Step 1：开发时构建 Pipeline（固定顺序）**

```java
// Filter 实现
@Component
public class SaveOrderFilter extends AbstractPipelineFilter<OrderContext> {
    @Override
    public void handle(OrderContext context) {
        System.out.println("保存下单请求");
    }
}
```

```java
// Pipeline 配置：开发时确定 filter 顺序
@Configuration
public class OrderPipelineConfig {
    @Bean
    public PipelineExecutor<OrderContext> orderPipeline(
            SaveOrderFilter saveFilter,
            QueryOrderFilter queryFilter,
            CheckOrderFilter checkFilter) {
        PipelineExecutor<OrderContext> gen = new PipelineExecutor<>();
        gen.appendFilter(saveFilter, "保存订单");    // desc + 固定顺序
        gen.appendFilter(queryFilter, "查询信息");
        gen.appendFilter(checkFilter, "校验订单");
        return gen;
    }
}
```

**Step 2：YAML 配置启用/禁用（业务模块自己的 prefix）**

```yaml
order:
  pipeline:
    filter-selectors:
      ORDER:                        # 只配启用的 filter
        - SaveOrderFilter
        - CheckOrderFilter
        # QueryOrderFilter 未列出 = 禁用
      CHARGE:
        - QueryOrderFilter
```

```java
// 业务模块维护自己的 Properties
@Data
@ConfigurationProperties(prefix = "order.pipeline")
public class OrderPipelineProperties {
    private Map<String, List<String>> filterSelectors;
}
```

> YAML 列表只控制启用/禁用，不控制顺序。顺序由 `PipelineExecutor.appendFilter()` 调用顺序决定。

**Step 3：通过 FilterSelectorFactory 静态方法创建 selector，PipelineExecutor 执行**

```java
@Autowired
private PipelineExecutor<OrderContext> orderPipeline;
@Autowired
private OrderPipelineProperties props;

public void placeOrder(OrderParam param) {
    // 调用 common 的静态工厂方法，传入业务的 filter-selectors
    FilterSelector selector = FilterSelectorFactory.createFilterSelector(
            OrderCodeEnum.PLACE_ORDER.getType(), props.getFilterSelectors());

    OrderContext context = new OrderContext(OrderCodeEnum.PLACE_ORDER, selector);
    context.setParam(param);

    orderPipeline.execute(context);
    OrderModel model = context.getModel();
}
```

| Selector 类型 | YAML 配置情况 | 行为 |
|-------------|------------|------|
| `LocalListFilterSelector` | type 在 YAML 中有配置 | 只匹配 YAML 中列出的 filter 名 |
| `MatchAllFilterSelector` | type 配置为 `on` | 全部 filter 生效 |
| `DummyFilterSelector` | type 配置为 `off` | 全部 filter 禁用 |
| — | type 未在 YAML 中配置 | 抛出 ExtensionException |

## 3. FilterSelector 说明

| Selector | 行为 | 使用场景 |
|----------|------|---------|
| `LocalListFilterSelector` | 基于列表的选择器 | 编程式或声明式，精确控制哪些 filter 生效 |
| `MatchAllFilterSelector` | `matchFilter(name)` 始终返回 true | 无过滤需求时使用 |
| `DummyFilterSelector` | `matchFilter(name)` 始终返回 false | 禁用所有 filter 时使用 |

> **扩展数据源**：`FilterSelectorFactory.createFilterSelector(type, map)` 接收 `Map<String, List<String>>`，与来源无关。从 YAML、DB、Nacos 加载的数据只要转成 Map 即可传入，无需自定义 Selector。如确需自定义，实现 `FilterSelector` 接口的两个方法即可。

> `AbstractPipelineFilter.doFilter()` 模板会先调用 `context.getFilterSelector().matchFilter(name)`，只有匹配时才执行 `handle()`。

## 4. 中断链

在 `handle()` 中调用 `context.setContinueChain(false)` 即可中断后续 filter 执行：

```java
@Override
public void handle(OrderContext context) {
    if (invalid) {
        context.setContinueChain(false);  // 校验不通过，后续 filter 不再执行
        return;
    }
    // 正常处理...
}
```

## 5. API 速查

```java
// PipelineFilter 接口
public interface PipelineFilter<T extends PipelineContext> {
    void doFilter(T context, PipelineFilterChain<T> chain);
}

// PipelineExecutor 构建 + 执行
PipelineExecutor<T> gen = new PipelineExecutor<>();
gen.appendFilter(filter, "desc");
gen.execute(context);

// FilterSelectorFactory（静态工具）
FilterSelector selector = FilterSelectorFactory.createFilterSelector(
    type.getType(), props.getFilterSelectors());
```

## 6. 可运行测试

`svc/server/main/src/test/java/com/github/cadecode/xboot/main/demo/pipeline/PipelineTests.java`（15 cases）
