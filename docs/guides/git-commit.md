# Git Commit Convention

> **Scope**: 项目级共享规范，适用于前后端所有提交。

Follow **Angular Conventional Commits**:

```
<type>(<scope>): <subject>

<body lines — why, not just what>

<footer>
```

---

## Types

| Type | Purpose |
|------|---------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `style` | Code formatting (no logic change) |
| `refactor` | Code restructure (no feature/fix) |
| `perf` | Performance improvement |
| `test` | Adding/updating tests |
| `build` | Build system, external dependencies |
| `ci` | CI configuration changes |
| `chore` | Other changes (non-src, non-test) |
| `revert` | Revert previous commit |

## Scope

Module or layer name in parentheses, omit for project-wide changes:

| 后端 Scope | 前端 Scope | 通用 Scope |
|-----------|-----------|-----------|
| `(server)` / `(main)` | `(app)` | `(spec)` |
| `(common)` | `(component)` | |
| `(framework)` | `(store)` | `(deps)` |
| `(codegen)` | `(view)` | |
| `(mybatis)` | | |
| `(web)` | | |

## Subject

- ≤ 50 chars, starts with verb, imperative mood
- Chinese or English subject is acceptable (be consistent within a commit)

## Body (可选)

对本次 commit 的详细描述，与第一行之间空一行。用于说明变更动机、对比方案等复杂信息。

```
feat(server): add login ip whitelist

用户登录时校验 IP 是否在白名单内，不在则拒绝。
白名单从 Nacos 配置中心动态读取，无需重启。
```

## Footer (可选)

**破坏性更新** `BREAKING CHANGE:`：当前提交导致与之前版本不兼容时标注，并附迁移方法：

```
refactor(common): 重构 ExtensionType 接口

BREAKING CHANGE: getType() 返回值类型从 String 改为 int

迁移方法：枚举实现类将 getType() 返回值改为数字 ID。
```

**关闭 Issue**：`Closes #xxx`

```
fix(server): 修复订单超时未取消

Closes #234
```

## Examples

```
refactor: 代码目录调整至 svc 目录下
docs: add README + AGENTS + 命名规则 markdown doc
test(server): add application tests
refactor(common): 移除不必要的泛型
chore(server): remove redundant config
feat(codegen): 添加代码生成工具类
```
