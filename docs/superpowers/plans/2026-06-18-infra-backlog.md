# 待开发项清单（基础设施盘点结论）

> 创建：2026-06-18（下班存档）
> 背景：Web 层基础设施（异常/校验/追踪/分页/文档/actuator/网关 CORS）已全部完成并验证，
> 已提交 `60ebae7` 并推送。本文件记录盘点后识别的**后续待开发项**，明天起按优先级推进。

## 现状：已齐备的基础设施

缓存(Redis 单层+L2扩展点)、MQ(JSON+死信+重试)、全局异常、Bean 校验、i18n、
TraceId 链路追踪、BaseEntity 审计、PageResult 分页、Swagger 文档、actuator 监控、
网关路由+CORS+全局过滤、统一响应 R<T>。
→ 作为「支撑业务开发的脚手架」已够用，可放心写业务功能。

## 待开发项（按优先级）

### 🔴 第一档 — 尽早补，缺了拖累后续所有开发

1. **测试基础设施**（性价比最高，建议最先做）
   - 现状：整个项目 **零 `src/test` 代码**，`spring-boot-starter-test` 已引但未用，
     基础设施全靠手动 curl 验证，回归成本将随代码量爆炸。
   - 待做：common 缓存/异常/i18n 单元测试；各服务 controller 集成测试骨架；
     考虑 Testcontainers 起 Redis/RabbitMQ/MySQL 做集成测试。

2. **配置外部化 / 敏感信息处理**
   - 现状：DB/RabbitMQ 密码以 `${ENV:明文默认值}` 形式散落各 `application.yml`，默认值即明文。
   - 待确认：`DataSourceConfig.passwordEncrypted` 字段名标称 encrypted，但**未见加解密逻辑**，
     需确认数据源密码是否真加密存储。
   - 待做：敏感配置移除明文默认值；数据源密码落地加密（如 AES/Jasypt）。

### 🟡 第二档 — 取决于业务走向，业务形态清晰后再决定

3. **认证授权**
   - 现状：网关 `TraceGlobalFilter` 仅预留鉴权入口注释，未实现；所有接口当前裸奔。
   - 触发条件：平台要多用户 / 对外开放时必做；纯内部 demo 可缓。

4. **限流 / 熔断降级**
   - 现状：未实现。已引 Spring Cloud Alibaba 全家桶（Sentinel 可直接用）。
   - 触发条件：接入**真实 LLM**（慢且贵）后几乎必然要加；网关可用 `RequestRateLimiter`。

### 🟢 第三档 — 锦上添花，按需

5. 分布式 ID（雪花）、操作审计日志、Nacos 配置中心（统一管理散落的 yml）、
   容器化部署（Dockerfile / k8s manifest）。

## 另有既存 TODO（独立文件）

- **L2 二级缓存**：Caffeine(L1) + Redis(L2) + 失效广播。扩展点已预留
  （`CacheProperties.MultiLevel` 开关、`CacheConfig` 的 `@ConditionalOnMissingBean(CacheManager)`）。
  实施前置条件：**先做失效广播**（建议复用现有 RabbitMQ 发 fanout），否则引入数据不一致。
  仅对读多写少、容忍秒级旧值的缓存区按 cacheName 选择性启用
  （候选：`schema:table`/`ds:list`/`schema:tables`；排除：`query:history`）。
  详见 [2026-06-18-cache-mq-infra-progress.md](2026-06-18-cache-mq-infra-progress.md)。

## 建议的下一步

明天从 **#1 测试基础设施** 开始（越早做越省事），同时确认 **#2 的密码加密现状**。
认证/限流等待业务形态（多用户？对外？接真 LLM？）明确后再启动。
