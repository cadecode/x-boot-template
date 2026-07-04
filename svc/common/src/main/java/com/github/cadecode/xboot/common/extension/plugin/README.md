# Plugin（策略模式）使用指南

基于 Spring Plugin Framework，通过 `Plugin.supports()` 运行时匹配策略。

## 1. 角色定义

| 角色 | 接口/类 | 职责 |
|------|---------|------|
| 插件服务 | `PluginService extends Plugin<PluginContext>` | 策略接口，继承 Spring Plugin 的 `supports()` |
| 插件上下文 | `PluginContext` | 携带 `getPluginType()` 供匹配 |
| 业务类型 | `ExtensionType` | 与 Pipeline 共用，`getType()` 返回类型标识 |
| 插件执行器 | `PluginExecutor` | 接口：`execute()` / `executeAll()` / `submit()` / `submitAll()` |
| 选择执行器 | `PluginSelectorExecutor` | 核心实现：遍历 pluginRegistry，`filter(o -> o.supports(context))` 选择策略 |

## 2. 使用方式

**Step 1：定义 Plugin 接口**

```java
public interface PayPlugin extends PluginService {
    void pay(OrderModel model);
}
```

**Step 2：实现策略**

```java
@Component
public class AliPayPlugin implements PayPlugin {
    @Override
    public boolean supports(PluginContext context) {
        return PayTypeEnum.ALIPAY.equals(context.getPluginType());
    }

    @Override
    public void pay(OrderModel model) {
        System.out.println("支付宝支付");
    }
}

@Component
public class WeChatPayPlugin implements PayPlugin {
    @Override
    public boolean supports(PluginContext context) {
        return PayTypeEnum.WECHAT.equals(context.getPluginType());
    }

    @Override
    public void pay(OrderModel model) {
        System.out.println("微信支付");
    }
}
```

**Step 3：注册 Plugin Registry（在 Application 类上）**

```java
@SpringBootApplication
@EnablePluginRegistries({PayPlugin.class})
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}
```

**Step 4：通过 PluginSelectorExecutor 调用**

```java
@Autowired
private PluginSelectorExecutor pluginExecutor;

public void doPay(OrderModel model, PayTypeEnum payType) {
    PluginContext context = new PluginContextImpl(payType);  // PluginContext 实现

    // 执行匹配的第一个插件
    pluginExecutor.execute(PayPlugin.class, context, plugin -> {
        plugin.pay(model);
    });

    // 或提交后获取返回值
    String result = pluginExecutor.submit(PayPlugin.class, context,
        PayPlugin::getPayUrl);
}
```

**内部机制**：`PluginSelectorExecutor.selectService()` 遍历 `pluginRegistry` 中所有 bean，调用 `plugin.supports(context)` 找到匹配的插件（引用 `context.getPluginType()` 判断）。

## 3. 两种上下文模式

Plugin 支持两种 `PluginContext` 实现模式：直接实现 `PluginContext` 接口，或通过 `AbstractPluginContext` 继承。业务可根据需要选择。详见 Plugin 模块源码。

## 4. API 速查

```java
// PluginService 接口
public interface PluginService extends Plugin<PluginContext> { }

// PluginSelectorExecutor 调用
pluginExecutor.execute(PluginClass.class, context, plugin -> { ... });
pluginExecutor.submit(PluginClass.class, context, PluginClass::method);
```
