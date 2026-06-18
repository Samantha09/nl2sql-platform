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

## Web 层（异常 / 校验 / 追踪 / 文档）

`common/web/` `common/trace/` `common/entity/` + `common/PageResult`，由 `WebAutoConfiguration` /
`OpenApiAutoConfiguration` 装配（`@ConditionalOnWebApplication(SERVLET)`），仅 web 服务激活：

- `GlobalExceptionHandler`（`@RestControllerAdvice`）— 统一捕获业务异常 / 参数校验异常 /
  缺参 / 兜底 `Exception`，全部转 `R<T>`；校验失败汇总字段错误返回 400，兜底记日志返回 500 不泄露堆栈
- `TraceIdFilter` — 从 `X-Trace-Id` 头取（网关下发）或生成 traceId，放入 MDC 并回写响应头，
  请求结束清理防止线程复用串号；日志 pattern `%X{traceId:-}` 输出（见各服务 `logback-spring.xml`）
- `BaseEntity`（`@MappedSuperclass`）— 统一 `id`/`createdAt`/`updatedAt`，用 JPA 生命周期回调
  （`@PrePersist`/`@PreUpdate`）维护时间戳，不依赖 Hibernate 专有注解；`QueryHistory`/`DataSourceConfig` 继承
- `PageResult<T>` — 纯 POJO 分页结果（records/total/pageNum/pageSize/pages），`PageResult.of(...)`
  由各服务从 Spring Data `Page` 适配，common 不依赖 spring-data
- `OpenApiAutoConfiguration` — 每个 web 服务生成统一标题的 Swagger 文档；UI 在 `/swagger-ui.html`，
  JSON 在 `/v3/api-docs`

下游服务需显式声明 `spring-boot-starter-validation` / `spring-boot-starter-actuator` /
`springdoc-openapi-starter-webmvc-ui` 才激活对应能力（common 中均为 `optional`）。

## 网关（CORS / 全局过滤 / 路由）

`gateway-service`：
- 全局 CORS：`spring.cloud.gateway.globalcors`，放行所有来源/方法/头
- `TraceGlobalFilter`（`GlobalFilter`，最高优先级）— 注入/透传 traceId 下发下游、记录访问日志
  （方法/路径/耗时/状态），预留鉴权入口
- actuator 暴露 `health,info,metrics,gateway`，`/actuator/gateway/routes` 查看路由
