# 一个技术一个 starter，由 framework 统一聚合

后端按「dependency（BOM）→ common → starter/* → framework → server/*」分层。每个集成技术单独一个 starter 模块（web、cache、concurrent、datasource、mybatis、swagger、actuator），只放自动配置与默认值；`framework` 依赖它们全部并提供 `x-boot.framework` 统一配置；业务模块只依赖 `framework`。这样换掉某个技术只需删掉一个 starter 与 framework 里的一行依赖，业务代码无需改动，代价是模块数量多、IDE 里有 10 个小模块。

## Considered Options

- 单个 `x-boot-starter-all`：模块少，但任一技术的配置类都会强制参与所有业务应用的类路径，无法按需裁剪。
- 用 Spring Boot 自己的 autoconfigure 分层（`xxx-spring-boot-autoconfigure` + `xxx-spring-boot-starter` 双模块）：更标准，但本模板每个 starter 只服务自己一个应用，拆双模块的收益为零。
