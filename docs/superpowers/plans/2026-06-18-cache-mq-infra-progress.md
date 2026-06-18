# 缓存 + MQ 基础设施 — 进度与待续计划

> 创建：2026-06-18（上班前临时存档）
> 状态：**计划已确认，代码尚未开始**。工作区干净，无任何代码改动。

## 背景

用户要求完善基础架构：消息队列消费 + Redis 缓存，均用注解管理（Spring Cache 增强版），
强调高扩展性与可读性。已确认：**本次只做单层 Redis 缓存，但全程预留 L2（二级缓存）扩展点**，
L2 作为后续 TODO。

## 已确认的设计决策

- 缓存方案：**增强版 Spring Cache**（保留原生 `@Cacheable/@CacheEvict`，定制 CacheManager），
  不引入自定义 AOP 注解，不做二级缓存（仅预留扩展点）。
- L2 二级缓存 = Caffeine(L1 本地) + Redis(L2 分布式)，靠 Redis Pub/Sub 广播本地失效。本次**不实现**。

## 待执行任务清单（会话内 TaskList，重启会丢，故抄录于此）

1. **common 缓存基础设施（单层 Redis + L2 扩展点）** ← 下次从这里开始
   - `common/cache/CacheNames.java` — 缓存名常量集中定义
   - `common/cache/CacheTtl.java` — TTL 常量，与 CacheNames 一一对应
   - `common/cache/CacheProperties.java` — `@ConfigurationProperties("nl2sql.cache")`，
     含 `keyPrefix / defaultTtl / nullTtl / Map<String,Duration> ttls`，
     **预留** `multiLevel.enabled`(默认 false) 与每 cache 的 `localTtl` 字段
   - `common/cache/CacheConfig.java` — `@EnableCaching` + `@AutoConfiguration`，
     `RedisCacheManager`：`GenericJackson2JsonRedisSerializer` + key 前缀 `nl2sql:` +
     按 cacheName 差异化 TTL + 空值防穿透(`cacheNullValues`, nullTtl=60s)。
     CacheManager 用单一工厂方法产出，加 `@ConditionalOnProperty("nl2sql.cache.multi-level.enabled")`
     占位，未来加 `CompositeCacheManager`(Caffeine L1+Redis L2) 即可切换，调用方代码不变。
   - `common/pom.xml` 增加 `spring-boot-starter-cache` + `spring-boot-starter-data-redis`
     + `spring-boot-starter-amqp` + `spring-boot-configuration-processor`，**均 optional**。
     （注意：编辑前必须先 Read 该文件）

2. **common MQ 基础设施（去重 + JSON + 死信重试）**
   - `common/mq/MqConst.java` — 统一 exchange/queue/routingKey 常量，
     消除 ai-service 与 query-service 两边重复（当前两边各自定义同名常量）
   - `common/mq/RabbitJsonConfig.java` — `@AutoConfiguration`，注册
     `Jackson2JsonMessageConverter`（替换当前 JDK 序列化）+ publisher-confirms/returns 回调
   - 每个业务队列声明 DLX + DLQ（`x-dead-letter-exchange`），消费端有限重试超限进 DLQ
   - 预留 `nl2sql.mq.*` 配置前缀控制重试次数

3. **各服务接入缓存注解与统一 MQ 配置**
   - schema-service / query-service 的 Service 方法加 `@Cacheable/@CacheEvict` 示例
   - 各服务 `RabbitConfig` 改为引用 `MqConst`，删除重复常量
   - 各服务注册 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   - application.yml 增加 `nl2sql.cache` / `nl2sql.mq` 配置项

4. **编译验证**
   - `mvn -q -pl common,schema-service,query-service,ai-service -am compile`

5. **[TODO 后续迭代] L2 二级缓存** — Caffeine L1 + Redis L2 + 失效广播，
   开关 `nl2sql.cache.multi-level.enabled`，复用本次预留字段，调用方 `@Cacheable` 不变。

## 现状关键事实（来自代码勘察）

- 技术栈：Spring Boot 3.2.5 / Spring Cloud Alibaba 2023 / Java 17 / Nacos / MySQL / Redis / RabbitMQ
- MQ 现状：`@RabbitListener` 消费可用，但 (a) 常量在 ai/query 两边**重复**；
  (b) 走 **JDK 默认序列化**（非 JSON）；(c) **无 DLX/重试/publisher-confirm**。
  关键类：`ai-service .../config/RabbitConfig.java`、`.../listener/NL2SQLListener.java`；
  `query-service .../config/RabbitConfig.java`、`.../listener/SQLReadyListener.java`。
- Redis 现状：query-service 已有 `spring-boot-starter-data-redis` 依赖，但
  **无 @EnableCaching、无 CacheManager、无任何缓存注解**，完全空白。
- 事件 DTO 在 `common/event/`：NL2SQLEvent / SQLReadyEvent / ResultReadyEvent。
