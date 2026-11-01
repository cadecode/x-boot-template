# 双级缓存的 TTL 编码在缓存名后缀上

DLCache（L1 Caffeine + L2 Redis）不支持按缓存名单独配置 TTL，而是要求调用方把 TTL 写进缓存名：`user#6`、`product#5m`，duration 用 Spring `DurationStyle` 语法，由 `DynaTtlNameUtil` 按最后一个 `#` 切分。这样每个缓存点的过期时间就写在业务代码里、无需改配置，也让 Redis 后端的动态 TTL 与本地 Caffeine 用同一份过期时间。代价是**缓存名含 `#数字` 会产生歧义**（`order#2024` 会被当成 2024ms 的 TTL），且 `@Cacheable` 与 `@CacheEvict` 必须写完全一致的完整缓存名，否则会生成两个实例、永不命中。这是加载在数据键格式上的约定，改它等于让线上已有缓存全部失效，所以记录在此，避免后人「顺手清掉名字里的 #」。

## Considered Options

- 每个缓存名在 yaml 里配 TTL：调用方无需记协议，但新增缓存点要改配置，且 Redis 与本地两端的时间源容易漂移。
