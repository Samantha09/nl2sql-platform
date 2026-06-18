# 基础设施约定（common 模块）

common 的基础设施依赖均为 `optional`，通过 `@AutoConfiguration` + `@ConditionalOnClass` 装配，
下游服务需显式声明对应 starter（`data-redis`/`amqp`）才会激活，对纯 DTO 使用者无副作用。
自动配置注册在 `common/src/main/resources/META-INF/spring/...AutoConfiguration.imports`。

## 缓存（增强版 Spring Cache）

`common/cache/`：
- `CacheConfig` — RedisCacheManager：JSON 序列化、key 前缀 `nl2sql:`、按 cacheName 差异化 TTL、空值防穿透
- `CacheProperties`（`nl2sql.cache`）— 统一扩展入口，预留 `multiLevel`（L2 开关）+ `localTtls`
- `CacheNames` / `CacheTtl` — 缓存名与默认 TTL 常量集中定义

用法：`@Cacheable(cacheNames = CacheNames.XXX, key = "...")`，写操作配 `@CacheEvict`。
TTL 优先级：`nl2sql.cache.ttls.<name>` 配置 > `CacheTtl.DEFAULTS` 内置 > `defaultTtl`。

> 注意：self-invocation（同类内部调用）下 `@Cacheable`/`@CacheEvict` 不生效（AOP 代理限制）。

## MQ（JSON + 死信重试）

`common/mq/`：
- `MqConst` — 三组事件的 exchange/queue/routingKey + DLX/DLQ/DLK 常量
- `RabbitJsonConfig` — JSON 转换器、publisher-confirms/returns 回调、消费端有限重试 + 超限投递死信
- `MqTopology` — 声明式工具，`topologyWithDlq(...)` 一行声明「业务队列 + DLX + DLQ」
- `MqProperties`（`nl2sql.mq`）— 重试次数与退避配置

服务的 `RabbitConfig` 只需返回 `Declarables` 调用 `MqTopology.topologyWithDlq(...)`，常量引用 `MqConst`。

## 枚举 / 异常 / 国际化

`common/enums/` `common/exception/` `common/i18n/`：
- `IEnum<C>` — 枚举统一父接口（getCode/getDesc + 静态 `of()` 反查）
- `IResultCode` — 错误码契约，带 i18n key 与 `getMessage()`
- `ResultCode` — 通用错误码枚举（200/400/.../503），绑定 i18n key + 中文兜底
- `BaseException` — 业务异常父类，承载 `IResultCode`
- `MessageUtils.get(key, args)` — 按当前 locale 解析文案
- i18n 资源在 `common/src/main/resources/i18n/messages[_zh_CN/_en].properties`
- web 服务按 `Accept-Language` 自动切 locale（`I18nWebConfiguration`）

用法：`throw new BaseException(ResultCode.NOT_FOUND);` → 全局处理器 `R.error(code, ex.getMessage())`。
