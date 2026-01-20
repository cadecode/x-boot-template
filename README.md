# uni-boot-template

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-brightgreen)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**通用的 Spring Boot 后台管理系统模板**

uni-boot-template 是一个轻量级、可扩展的企业级 Spring Boot 后台管理系统模板，采用多模块架构设计，提供开箱即用的基础设施和最佳实践。

## ✨ 核心特性

### 🏗️ 模块化架构
- **dependency**: 统一管理所有依赖版本（BOM）
- **common**: 公共基础模块（异常、工具类、扩展点机制）
- **starter**: 可插拔的自动配置模块（遵循Spring Boot Starter规范）
- **framework**: 框架层统一配置（集成所有starter）
- **server**: 业务服务模块（admin管理后台）

 ### 🔌 扩展点机制

采用 **Pipeline 责任链 + Plugin 策略** 双扩展点模式，支持业务逻辑的可插拔扩展：

**Pipeline 机制**：基于责任链模式，按配置顺序执行多个 Filter，支持业务流程编排

**Plugin 机制**：基于策略模式，根据上下文动态选择实现，支持运行时扩展

**使用场景**：
- 订单处理流程（校验→库存→价格→支付）
- 多种支付方式切换（支付宝、微信、银联）
- 消息发送渠道选择（短信、邮件、站内信）

### 🎯 统一异常处理

```java
// 自定义异常
throw GeneralException.of(ErrorEnum.USER_NOT_FOUND, "userId: {}", userId);

// 全局异常捕获，自动转换为统一响应格式
{
  "status": 404,
  "data": null,
  "error": {
    "code": "USER_1001",
    "message": "用户不存在",
    "path": "/api/user/123"
  }
}
```

 ### 🔄 多数据源

**特性**:
- 原生支持，无需额外依赖
- 支持 Druid、HikariCP、DBCP2、BeeCP 等数据源
- 注解驱动，使用简洁
- 支持 Mapper、Service、Controller 多层级的注解声明
- 支持读写分离和负载均衡策略

### 📦 统一响应格式

```java
@ApiFormat  // 标记需要统一返回格式
@RestController
public class UserController {
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getById(id);
    }
}

// 自动包装为:
{
  "status": 200,
  "data": {"id": 1, "name": "张三"},
  "error": null
}
```

### 📚 API 文档

集成 **Knife4j + SpringDoc OpenAPI 3**，提供美观的API文档界面：

- 访问地址: `http://localhost:8080/doc.html`
- 自动扫描所有Controller生成文档
- 支持在线调试
- 支持导出文档

### 🔧 代码生成

- Entity（实体类）
- Mapper（数据访问层）
- Service（业务层）
- Controller（控制层）
- EntityConvert（MapStruct转换器）

```bash
# 执行代码生成
```

### 🔒 配置加密

支持使用 **Jasypt** 加密敏感配置信息：

```bash
# 加密配置
mvn jasypt:encrypt \
  -D'jasypt.plugin.path'="file:server/admin/src/main/resources/application.yml" \
  -D'jasypt.encryptor.password'="your-password"
```

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 基础框架 |
| Java | 17 | 开发语言 |
| Druid | 1.2.21 | 数据库连接池 |
| Knife4j | 4.4.0 | API文档 |
| Hutool | 5.8.12 | 工具库 |
| MapStruct | 1.5.2 | 对象映射 |
| Lombok | 1.18.32 | 简化代码 |
| Jasypt | 3.0.5 | 配置加密 |

## 📦 项目结构

```
uni-boot-template
├── dependency/              # 依赖版本管理模块（BOM）
│   └── pom.xml
├── common/                  # 公共基础模块
│   ├── consts/             # 常量定义
│   ├── enums/              # 枚举定义（ErrorCode、ExtensionType）
│   ├── exception/          # 异常类（BaseException、GeneralException）
│   ├── extension/          # 扩展点机制
│   │   ├── pipeline/       # Pipeline扩展点
│   │   └── plugin/         # Plugin扩展点
│   └── util/               # 工具类（AssertUtil、JacksonUtil、SpringUtil）
├── starter/                 # 自定义Starter模块
│   ├── web/                # Web配置（统一返回、全局异常）
│   ├── swagger/            # API文档配置
│   ├── datasource/         # 动态数据源配置
│   └── actuator/           # 监控配置
├── framework/               # 框架层配置
│   └── config/             # 配置类（Jackson、线程池、WebMvc）
│   ├── generator/          # 自定义生成器
│   └── resources/template/  # 代码模板
├── server/                  # 服务模块
│   └── admin/               # 管理后台服务
│       └── src/main/
│           ├── java/        # 业务代码
│           └── resources/   # 配置文件
└── pom.xml                  # 父POM
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+ (或其他支持的数据库)

### 1. 克隆项目

```bash
git clone https://github.com/cadecode/uni-boot-template.git
cd uni-boot-template
```

### 2. 编译项目

```bash
# 编译所有模块
mvn clean install

# 跳过测试编译
mvn clean install -DskipTests
```

### 3. 配置数据库

编辑 `server/admin/src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/your_database
      username: your_username
      password: your_password
```

### 4. 启动服务

```bash
cd server/admin
mvn spring-boot:run
```

或直接运行 `AdminApplication` 主类。

### 5. 访问服务

- 应用地址: http://localhost:8080
- API 文档: http://localhost:8080/doc.html
- Druid 监控: http://localhost:8080/druid/ (账号: dev/dev123)

## 📖 使用指南

### 添加新模块

1. 在根 `pom.xml` 的 `<modules>` 中添加模块声明
2. 创建模块目录和 `pom.xml`
3. 根据需要依赖其他模块（common、starter等）

### 扩展点使用

详见 [AGENTS.md](AGENTS.md) 中的扩展机制章节。

### 统一返回格式控制

```java
// 类级别：所有方法都统一返回
@ApiFormat
@RestController
public class DemoController {
    // ...
}

// 方法级别：仅该方法统一返回
@RestController
public class DemoController {
    @ApiFormat
    @GetMapping("/api/data")
    public Object getData() {
        // ...
    }
}

// 原始返回：不包装
@RestController
public class DemoController {
    @GetMapping("/raw")
    public String raw() {
        return "raw string";
    }
}
```

## 🔨 Maven 命令

### 编译打包

```bash
# 打包（默认 dev 环境）
mvn clean package

# 打包（指定环境）
mvn clean package -P prod

# 跳过测试打包
mvn clean package -DskipTests

# 打包单个模块
cd server/admin
mvn clean package
```

### 版本管理

```bash
# 查看依赖更新
mvn versions:display-dependency-updates

# 统一升级子模块版本
mvn versions:set -DnewVersion=1.0.0

# 回退版本变更
mvn versions:revert
```

### 配置加密/解密

```bash
# 加密配置文件中的密码等敏感信息
mvn jasypt:encrypt \
  -D'jasypt.plugin.path'="file:server/admin/src/main/resources/application.yml" \
  -D'jasypt.encryptor.password'="your-password"

# 解密配置文件
mvn jasypt:decrypt \
  -D'jasypt.plugin.path'="file:server/admin/src/main/resources/application.yml" \
  -D'jasypt.encryptor.password'="your-password"
```

### 代码生成

```bash

# 生成数据库文档
```

## 📝 配置说明

### 环境配置

支持三种环境配置（通过Maven Profile控制）：

- **dev**: 开发环境（默认）- `mvn spring-boot:run`
- **test**: 测试环境 - `mvn spring-boot:run -P test`
- **prod**: 生产环境 - `mvn spring-boot:run -P prod`

配置文件位置：`server/admin/src/main/resources/application-{profile}.yml`

### Swagger 配置

```yaml
uni-boot:
  swagger:
    title: API Docs
    description: ${spring.application.name} 接口文档
    version: @project.version@
    contact-name: Cade Li
    contact-url: https://github.com/cadecode/uni-boot-template
    contact-email: cade.li@qq.com

knife4j:
  enable: true
  setting:
    language: zh_cn
```

### Druid 监控配置

```yaml
spring:
  datasource:
    druid:
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: dev
        login-password: dev123
      filter:
        stat:
          enabled: true
          log-slow-sql: true
          slow-sql-millis: 1000
```

## 🧪 测试

```bash
# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=AdminApplicationTests

# 运行指定测试方法
```

## 📚 更多文档

- [AGENTS.md](AGENTS.md) - 详细的技术架构和模块说明

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。

## 👨‍💻 作者

Cade Li

## 📮 联系方式

- GitHub: https://github.com/cadecode/uni-boot-template
- Email: cade.li@qq.com

## ⭐ Star History

如果这个项目对你有帮助，请给个 Star 支持一下！
