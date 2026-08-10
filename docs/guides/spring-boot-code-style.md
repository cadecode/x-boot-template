# Spring Boot 代码风格指南

> 包结构、分层命名、实体类规范、工具类命名的完整参考模板。

---

## 整体包结构

```
com.example.project
│
├── bean                      # 实体类统一目录
│   ├── entity                # 数据库实体
│   ├── vo                    # 接口返回对象
│   ├── dto                   # 接口接收参数
│   ├── bo                    # 业务层聚合对象
│   ├── doc                   # MongoDB 文档实体
│   ├── cache                 # 缓存实体对象
│   ├── index                 # ES 索引实体
│   ├── message               # MQ 消息体
│   └── event                 # Spring 事件对象
├── controller                # Web 接口层
├── service                   # 业务逻辑接口
│   └── impl                  # 业务逻辑实现
├── mapper                    # 数据访问层
├── biz                       # 业务聚合处理
├── client                    # Feign 客户端
├── convert                   # 对象转换器
├── aspect                    # AOP 切面
├── config                    # 配置类
├── constant                  # 常量类
├── enums                     # 枚举类
├── exception                 # 自定义异常
├── util                      # 工具类目录（含 Util 和 Kit）
└── job                       # 定时任务
    ├── XxxJob.java
    └── XxxHelper.java        # 辅助类，放在对应功能包内
```

---

## 分层命名规范

| 层级职责            | 类名后缀           | 包名                | 说明                                |
| ------------------- | ------------------ | ------------------- | ----------------------------------- |
| Web 接口层          | `XxxController`    | `controller`        | 接收请求、参数校验、返回 Vo         |
| Service 接口        | `XxxService`       | `service`           | 业务逻辑接口定义                    |
| Service 实现        | `XxxServiceImpl`   | `service.impl`      | 业务逻辑实现，加事务注解            |
| 数据访问层          | `XxxMapper`        | `mapper`            | MyBatis/MyBatis-Plus 映射接口       |
| 业务聚合处理        | `XxxBiz`           | `biz`               | 跨 Service 或外部调用的复杂业务编排 |
| 远程服务客户端      | `XxxClient`        | `client`            | Feign 声明式 HTTP 客户端            |
| 对象转换器          | `XxxConvert`       | `convert`           | 统一处理对象转换                    |
| AOP 切面            | `XxxAspect`        | `aspect`            | 横切关注点（日志、权限、监控等）    |
| 常量类              | `XxxConst`         | `constant`          | 常量按业务拆分，不堆在一个类中      |
| 枚举类              | `XxxEnum`          | `enums`             | 统一管理枚举定义                    |
| 自定义异常          | `XxxException`     | `exception`         | 业务异常或系统异常                  |
| 配置类              | `XxxConfig`        | `config`            | 如 `RedisConfig`、`SwaggerConfig`   |
| 定时任务            | `XxxJob`           | `job`               | 定时调度任务类                      |
| 辅助类（业务/功能） | `XxxHelper`        | 对应功能包内        | 如 `job.JobHelper`、`mail.MailHelper` |

---

## 实体类命名规范

### 业务实体

| 对象类型         | 类名后缀           | 包路径               | 说明                       |
| ---------------- | ------------------ | -------------------- | -------------------------- |
| 数据库实体       | `Xxx` / `XxxPo`    | `bean.entity` / `bean.po` | 与数据库表结构一一对应     |
| 接口返回对象     | `XxxVo`            | `bean.vo`            | 面向前端展示               |
| 接口接收参数     | `XxxDto`           | `bean.dto`           | 接收请求参数，可含校验注解 |
| 业务层聚合对象   | `XxxBo`            | `bean.bo`            | 业务逻辑层内部使用         |
| MongoDB 文档实体 | `XxxDoc`           | `bean.doc`           | MongoDB 实体定义           |
| 缓存实体对象     | `XxxCache`         | `bean.cache`         | Redis/Caffeine 缓存结构    |
| ES 索引实体      | `XxxIndex`         | `bean.index`         | Elasticsearch 文档映射     |
| MQ 消息体        | `XxxMessage`       | `bean.message`       | 队列/主题传输的消息体      |
| Spring 事件对象  | `XxxEvent`         | `bean.event`         | 领域事件或应用事件对象     |

#### 使用示例

```java
// 保存用户：接收 Dto，返回 Vo
@PostMapping("/save")
public SysUserVo saveUser(@RequestBody @Valid SysUserSaveDto dto) {
    SysUserPo po = userConvert.toPo(dto);
    userMapper.insert(po);
    return userConvert.toVo(po);
}
```

> ❌ 不要将 `Entity` / `Po` 直接返回给前端
> ✅ 各层对象严格隔离，降低变动影响面

### 业务之外的实体

对于**独立于业务之外**的实体，如工具类、框架配置等需要的对象，有以下两种处理方式：

| 方式               | 适用场景                                       | 示例                                                     |
| ------------------ | ---------------------------------------------- | -------------------------------------------------------- |
| **使用内部类**     | 仅被当前类使用、逻辑简单、内聚性强             | Controller 内部的请求/响应类、Service 内部的中间对象     |
| **放在业务类一起** | 与某个业务紧密相关，但不属于标准实体类型       | 业务特有的参数封装、中间计算结果对象                     |

#### 示例

**方式一：使用内部类（适用于仅当前类使用）**

```java
@RestController
public class UserController {

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest request) {
        // ...
    }

    // 内部类，仅用于当前 Controller
    static class LoginRequest {
        private String username;
        private String password;
    }
}
```

**方式二：放在业务类一起（适用于业务特有对象）**

```java
// 放在使用它的 Service 同包下，但不属于标准 bean 类型
// service/OrderStatContext.java
public class OrderStatContext {
    private Long userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    // 订单统计的中间计算对象
}
```

#### 选择建议

- **仅当前类使用** → 内部类
- **业务特有但不属于标准 Entity/Vo/Dto/Bo** → 放在对应业务包下

---

## 工具类命名规范

| 类型                | 命名后缀   | 包位置         | 是否静态 | 是否依赖容器 | 说明                              |
| ------------------- | ---------- | -------------- | -------- | ------------ | --------------------------------- |
| 静态工具类          | `XxxUtil`  | `util`         | ✅ 是    | ❌ 否        | 纯静态方法，无状态，不依赖 Spring |
| 容器化工具类        | `XxxKit`   | `util`         | ❌ 否    | ✅ 是        | 需要注入 Bean，加 `@Component`    |
| 辅助类（业务/功能） | `XxxHelper`| **对应功能包内** | ❌ 否    | ✅ 是        | 业务或功能专用，如 `job.JobHelper` |

### 示例代码

**Util（静态调用）**

```java
public class DateUtil {
    public static String format(LocalDateTime date, String pattern) {
        // 静态方法，不依赖 Spring
    }
}
// 使用
String dateStr = DateUtil.format(now, "yyyy-MM-dd");
```

**Kit（注入使用，放在 util 包）**

```java
@Component
public class RedisKit {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
// 使用
@Service
public class UserService {
    @Autowired
    private RedisKit redisKit;
}
```

**Helper（功能专用，放在对应功能包内）**

```java
// job/JobHelper.java
@Component
public class JobHelper {
    @Autowired
    private MailService mailService;

    public void sendAlert(String jobName, Exception e) {
        mailService.send("Job Alert", jobName + " 执行失败：" + e.getMessage());
    }
}
```
