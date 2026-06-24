# 核心链路打通设计（NL → SQL → 真实执行）

> 创建：2026-06-24
> 状态：设计待评审
> 关联：[2026-06-16-nl2sql-init-design.md](2026-06-16-nl2sql-init-design.md)、[2026-06-21-database-schema-scanner-design.md](2026-06-21-database-schema-scanner-design.md)

## 背景与问题

平台定位是「自然语言查数据库」，但核心业务链路两端均为 mock，平台目前是演示壳：

- **NL → SQL**：`ai-service/MockLLMService` 用关键词 `if-else` 返回固定 SQL，平台最核心的能力是假的。
- **SQL 执行**：`query-service/QueryService.query()` 发完 MQ 即返回写死的假数据；`SQLReadyListener` 注释「骨架阶段仅打印日志，真实执行后续实现」；`queryBySql()` 返回 `Map.of("result","mock")`。

已就绪的基础（不复用就浪费）：schema 扫描（真连库读表/列结构）、增强版 Spring Cache、可靠 MQ、AES 配置加密、测试基础设施。唯独「自然语言 → 真 SQL → 真实结果」这条主链路未通。本设计补齐它。

## 目标 / 非目标

**目标**

- ai-service 接入真实 LLM（MiniMax），替换 `MockLLMService`。
- query-service 真实连接目标库执行 SQL，返回真实结果。
- NL2SQL 携带 schema 上下文（表 + 列），保证生成 SQL 可用。
- 端到端可验证：输入自然语言 → 返回真实查询结果。

**非目标（本轮不做，见「已知风险与后续项」）**

- 安全护栏（只读限制 / 行数上限 / 执行超时）—— 用户明确选择后置。
- MQ 异步链路改造 —— 保留现状，不动。
- 非 MySQL 库执行 —— 仅 MySQL，与 scanner 现状一致。
- 认证授权、限流熔断。

## 关键设计决策

### D1. LLM 接入：MiniMax 的 Anthropic Messages 兼容端点

参考 `/home/san/PycharmProjects/finbot`（`config.yaml`）：`baseUrl=https://api.minimaxi.com/anthropic`，`api=anthropic-messages`，`model=MiniMax-M2.7`，key 走 `MINIMAX_API_KEY`。

- 用 Spring `RestClient` 直打 `{baseUrl}/v1/messages`，**不引入重型 SDK**。
- 协议：header `x-api-key: <key>` + `anthropic-version: 2023-06-01`；请求体 `{model, max_tokens, system, messages:[{role:"user", content}]}`；响应取 `content[0].text`。
- 抽出 `LlmClient` 接口 + `AnthropicMessagesLlmClient` 实现；`MockLLMService` 降级为可选 fallback。
- 配置 `nl2sql.llm.{baseUrl, apiKey, model, maxTokens, timeout}`；`apiKey` 用 `ENC()` 加密，复用现有 `SecureConfigEncryptor` 启动自动解密。

### D2. 链路形态：同步打通

- 复用现有同步 `AiController.convert()`，query-service 用 Feign 同步调用（不依赖 MQ 异步链路验证）。
- 链路：`query →(Feign)→ ai.convert →(Feign)→ schema 取上下文 + LLM → SQL`；`query →(Feign)→ schema 取连接 → DriverManager 执行 → 真实结果`。
- MQ 异步链路（`NL2SQLEvent` / `SQLReadyEvent`）**保留不动**，作为后续扩展。
- 代价：`/api/query/nl` 响应时间从「瞬时 mock」变到「LLM 秒级 + SQL 执行」，这是打通的必然代价。

### D3. 数据源路由：OpenFeign 调 schema-service 内部接口

query-service 只持有 `dataSourceId`，连接信息（host/port/user/password）全在 schema-service 的 `data_sources` 表，且密码以 `ENC()` 加密存储。

- 新增 schema-service 内部接口 `GET /api/schema/internal/datasource/{id}/connection`，**服务内解密**后返回连接信息 DTO，经 Feign 内网传输。
- 执行用 `DriverManager.getConnection` 即连即用（复用 `MySqlDatabaseScanner` 的 URL 模板），用完即关；MVP 不上连接池。

### D4. schema 上下文：喂给 LLM

- 新增 schema-service 内部接口 `GET /api/schema/internal/datasource/{id}/schema?database={db}`，返回精简表 + 列结构。
- ai-service 拼入 system prompt（DDL 摘要：表名 + 列名/类型/注释），让 LLM 知道可查的 schema。
- `ConvertRequest` 增加可选 `databaseName` 字段：缺省时若该数据源仅一个库则自动选用，多库时必须指定（否则返回参数错误 `BAD_REQUEST`）。

### D5. OpenFeign 引入（net-new 依赖）

全项目当前未用 Feign。引入 `spring-cloud-starter-openfeign`，各业务服务加 `@EnableFeignClients`。Feign 客户端接口与跨服务共享 DTO 放 `common`（依赖 `optional`，下游显式声明），符合现有基础设施约定。

## 架构与数据流

```
POST /api/query/nl  { naturalLanguage, dataSourceId, databaseName? }
 └─ query-service.QueryService.query()
     ├─ Feign → ai-service  POST /api/ai/convert { nl, dataSourceId, databaseName }
     │     └─ ai-service:
     │         ├─ Feign → schema-service  GET /internal/datasource/{id}/schema?database=
     │         ├─ LlmClient.chat(系统prompt[DDL摘要] + 用户问句)  →  SQL
     │         └─ 返回 SQL
     ├─ Feign → schema-service  GET /internal/datasource/{id}/connection
     ├─ SqlExecutor：DriverManager 建连 → 执行 SQL → ResultSet → List<Map>
     ├─ 存 QueryHistory（含真实 data）
     └─ 返回真实 QueryResult
```

## 组件清单（按模块）

### common（共享 DTO + Feign 客户端）

- `dto/DataSourceConnectionDTO`：`host / port / databaseNames / username / password`（password 明文，仅内部传输）。
- `dto/SchemaContextDTO`：`database` + `List<TableBrief>`，`TableBrief = { tableName, List<ColumnBrief{ name, type, comment } > }`。
- `feign/SchemaServiceClient`：`getConnection(Long id)`、`getSchema(Long id, String database)`。
- `feign/AiServiceClient`：`convert(ConvertRequest)`。
- pom：`spring-cloud-starter-openfeign`（optional）+ openfeign 的 BOM 依赖管理。

### ai-service

- `LlmClient`（接口）：`String chat(String systemPrompt, String userPrompt)`。
- `AnthropicMessagesLlmClient`：`RestClient` 实现，构造 Anthropic Messages 请求、解析响应、映射异常。
- `LlmProperties`：`@ConfigurationProperties("nl2sql.llm")`。
- `SchemaContextBuilder`：调 `SchemaServiceClient` 取 schema，拼 DDL 摘要。
- 改 `AiController.convert` 与 `ConvertRequest`（`+databaseName`）：注入 `LlmClient` + 上下文。
- `MockLLMService` 降级为 fallback：开关 `nl2sql.llm.fallback-to-mock`（默认 `false`，即 LLM 失败时报错而非静默 mock）。
- `AiResultCode`（12xxx band）：`AI_LLM_UNAVAILABLE(12001)`。
- pom：显式声明 openfeign。

### query-service

- `SqlExecutor`：按连接信息建连 → 执行 → `ResultSet` 转 `List<Map<String,Object>>` + 推断 `chartType`（单数值列 → bar/line，否则 table）+ `executeTimeMs` / `totalCount`，用完即关。
- 改 `QueryService.query()`：Feign `convert` 取 SQL → Feign `getConnection` → `SqlExecutor` 执行 → 真实 `QueryResult` + 存历史。
- `QueryResultCode`（13xxx band）：`QUERY_DATASOURCE_CONNECT_FAILED(13001)`、`QUERY_SQL_EXECUTE_FAILED(13002)`。
- pom：显式声明 openfeign。

### schema-service

- `GET /api/schema/internal/datasource/{id}/connection` → `DataSourceConnectionDTO`（服务内解密密码）。
- `GET /api/schema/internal/datasource/{id}/schema?database=` → `SchemaContextDTO`。
- `DataSourceService` 增 `getConnection(Long id)`（解密）、`getSchemaContext(Long id, String database)`。

## 错误处理

遵循项目约定：`throw new BaseException(XxxResultCode.YYY)` + i18n，不在代码里拼中文串。

| 场景 | 错误码 | i18n key |
|---|---|---|
| LLM 调用失败 / 超时 | `AiResultCode.AI_LLM_UNAVAILABLE` (12001) | `ai.llm_unavailable` |
| 目标库连不上 | `QueryResultCode.QUERY_DATASOURCE_CONNECT_FAILED` (13001) | `query.datasource_connect_failed` |
| SQL 执行失败 | `QueryResultCode.QUERY_SQL_EXECUTE_FAILED` (13002) | `query.sql_execute_failed` |

三类均补 `messages_zh_CN.properties` / `messages_en.properties`。错误响应不泄露底层堆栈。

## 测试策略

复用已就绪的 Mockito / 切片测试 / H2：

- `AnthropicMessagesLlmClient`：Mock `RestClient`，验证 header/body 构造、响应解析、异常 → `AI_LLM_UNAVAILABLE`。
- `SchemaContextBuilder`：验证 DDL 摘要拼接。
- `SqlExecutor`：**H2 内存库执行真实 SQL**，验证 `ResultSet → Map`、`chartType` 推断、`totalCount`。
- `QueryService` / `AiController`：切片测试，Mock Feign 客户端 + `LlmClient`。
- schema-service 两个内部接口的切片测试。

## 配置

Nacos `nl2sql-ai-service.yml`（或 local `application-local.yml`）：

```yaml
nl2sql:
  llm:
    base-url: https://api.minimaxi.com/anthropic
    api-key: ENC(...)            # 或 ${MINIMAX_API_KEY}
    model: MiniMax-M2.7
    max-tokens: 1024
    timeout: 30s
    fallback-to-mock: false
```

## 已知风险与后续项

- ⚠️ **无只读护栏**：LLM 可生成 `DROP` / `DELETE` 并直接在用户真实库执行。零代码缓解：被查库使用**只读数据库账号**。后续轮次补：只读校验（可基于现有 `AiController.validate` 的 select 检查扩展）、行数上限、`Statement.setQueryTimeout` 执行超时。
- ⚠️ `DataSourceController.list()` 现直接对外返回含 `passwordEncrypted` 的实体（既存问题），本轮不动。
- ⚠️ 内部接口（`/internal/**`）无鉴权，仅靠内网隔离；鉴权后置。
- 性能：同步 LLM 调用占用请求线程；高频场景后续可改回异步或加超时 / 熔断。

## 验收标准

- 配置真实 `MINIMAX_API_KEY` 后，`POST /api/query/nl` 传入自然语言 + 有效 `dataSourceId` / `databaseName`，返回基于真实库数据的 `QueryResult`。
- LLM 不可用时返回 `AI_LLM_UNAVAILABLE`（非静默 mock）。
- 目标库连不上 / SQL 出错时返回对应错误码，不泄露堆栈。
- 新增单测 / 切片测试通过；现有测试不回归。
